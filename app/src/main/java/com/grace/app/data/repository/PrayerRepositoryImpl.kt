package com.grace.app.data.repository

import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.local.dao.OfflineSyncDao
import com.grace.app.data.local.dao.PrayerDao
import com.grace.app.data.local.entity.OfflineSyncEntity
import com.grace.app.data.local.entity.PrayerEntity
import com.grace.app.data.remote.supabase.dto.PrayerDto
import com.grace.app.data.remote.supabase.dto.PrayerInsertDto
import com.grace.app.data.remote.supabase.dto.PrayerIntercessionDto
import com.grace.app.data.remote.supabase.dto.PrayerIntercessionRow
import com.grace.app.data.remote.supabase.dto.UserDto
import com.grace.app.data.remote.supabase.dto.mapper.toDbValue
import com.grace.app.data.remote.supabase.dto.mapper.toDomain
import com.grace.app.data.remote.supabase.dto.mapper.toEntity
import com.grace.app.data.sync.InterceedePayload
import com.grace.app.data.sync.PostPrayerPayload
import com.grace.app.data.sync.SyncActions
import com.grace.app.data.util.CrashReporter
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.model.Prayer
import com.grace.app.domain.model.PrayerCategory
import com.grace.app.domain.repository.PrayerRepository
import com.grace.app.domain.util.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerRepositoryImpl @Inject constructor(
    private val prayerDao: PrayerDao,
    private val offlineSyncDao: OfflineSyncDao,
    private val supabase: SupabaseClient,
    private val networkMonitor: NetworkMonitor,
    private val prefs: UserPreferencesRepo
) : PrayerRepository {

    override fun getPrayers(category: PrayerCategory?): Flow<Result<List<Prayer>>> = flow {
        if (networkMonitor.isOnline) {
            try {
                val remote = supabase.from("prayers")
                    .select {
                        filter { neq("status", "archived") }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<PrayerDto>()

                val authorIds = remote
                    .filter { !it.isAnonymous }
                    .mapNotNull { it.userId }
                    .toSet()
                val nameByUid: Map<String, String> = if (authorIds.isEmpty()) emptyMap()
                else runCatching {
                    supabase.from("users")
                        .select { filter { isIn("id", authorIds.toList()) } }
                        .decodeList<UserDto>()
                        .associate { it.id to it.name }
                }.getOrDefault(emptyMap())

                val enriched = remote.map { dto ->
                    if (dto.isAnonymous) dto
                    else dto.copy(userName = nameByUid[dto.userId] ?: dto.userName)
                }

                val proxyLeaderIds = remote
                    .mapNotNull { it.postedByProxy }
                    .toSet()
                    .filter { it !in nameByUid.keys }
                val proxyNameByUid: Map<String, String> = if (proxyLeaderIds.isEmpty())
                    nameByUid
                else runCatching {
                    val extra = supabase.from("users")
                        .select { filter { isIn("id", proxyLeaderIds) } }
                        .decodeList<UserDto>()
                        .associate { it.id to it.name }
                    nameByUid + extra
                }.getOrDefault(nameByUid)

                prayerDao.insertAll(enriched.map {
                    it.toEntity(proxyLeaderName = it.postedByProxy?.let { id -> proxyNameByUid[id] })
                })
                val protectedIds = pendingPostPrayerLocalIds()
                val keepIds = remote.map { it.id }.toSet() + protectedIds
                if (keepIds.isEmpty()) prayerDao.clearAll()
                else prayerDao.deleteNotIn(keepIds.toList())
            } catch (_: Exception) {
                emit(Result.Error("Couldn't refresh prayers. Showing saved ones."))
            }
        }
        emitAll(
            prayerDao.getAllActive().map { entities ->
                val prayers = entities.map { it.toDomain() }
                    .let { list ->
                        if (category == null) list
                        else list.filter { it.category == category }
                    }
                Result.Success(prayers)
            }
        )
    }.flowOn(Dispatchers.IO)

    private suspend fun pendingPostPrayerLocalIds(): Set<String> = runCatching {
        offlineSyncDao.getPending()
            .asSequence()
            .filter { it.action == SyncActions.POST_PRAYER }
            .mapNotNull {
                runCatching {
                    Json.decodeFromString<PostPrayerPayload>(it.payload).localId
                }.getOrNull()
            }
            .toSet()
    }.getOrDefault(emptySet())

    override suspend fun postPrayer(
        content: String,
        isAnonymous: Boolean,
        category: PrayerCategory
    ): Result<Unit> {
        val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()
        ?: return Result.Error("Your session expired. Please sign in again.")

        if (networkMonitor.isOnline) {
            val online = runCatching {
                val inserted = supabase.from("prayers")
                    .insert(
                        PrayerInsertDto(uid, content, isAnonymous, category.toDbValue())
                    ) { select() }
                    .decodeSingle<PrayerDto>()
                prayerDao.insert(inserted.toEntity())
            }
            if (online.isSuccess) return Result.Success(Unit)
            CrashReporter.log("postPrayer online attempt failed; falling back to offline queue")
            online.exceptionOrNull()?.let { CrashReporter.recordNonFatal(it) }
        }

        return try {
            val localId = UUID.randomUUID().toString()
            prayerDao.insert(
                PrayerEntity(
                    id = localId,
                    userId = if (isAnonymous) null else uid,
                    userName = null,
                    content = content,
                    isAnonymous = isAnonymous,
                    category = category.toDbValue(),
                    status = "active",
                    prayCount = 0,
                    isFlagged = false,
                    expiresAt = null,
                    createdAt = OffsetDateTime.now().toString()
                )
            )
            offlineSyncDao.insert(
                OfflineSyncEntity(
                    id = UUID.randomUUID().toString(),
                    action = SyncActions.POST_PRAYER,
                    payload = Json.encodeToString(
                        PostPrayerPayload(
                            content = content,
                            isAnonymous = isAnonymous,
                            category = category.toDbValue(),
                            userId = uid,
                            localId = localId
                        )
                    )
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Couldn't save your prayer. Try again.", e)
        }
    }

    override suspend fun intercede(prayerId: String): Result<Unit> {
        return try {
            val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()
            ?: return Result.Error("Your session expired. Please sign in again.")

            val current = prayerDao.getById(prayerId)

            if (!networkMonitor.isOnline) {
                current?.let { prayerDao.updatePrayCount(prayerId, it.prayCount + 1) }
                offlineSyncDao.insert(
                    OfflineSyncEntity(
                        id = UUID.randomUUID().toString(),
                        action = SyncActions.INTERCEDE,
                        payload = Json.encodeToString(InterceedePayload(prayerId))
                    )
                )
                return Result.Success(Unit)
            }

            val alreadyPrayed = supabase.from("prayer_intercessions")
                .select {
                    filter {
                        eq("prayer_id", prayerId)
                        eq("user_id", uid)
                    }
                }
                .decodeList<PrayerIntercessionRow>()
                .isNotEmpty()
            if (alreadyPrayed) return Result.Success(Unit)

            current?.let { prayerDao.updatePrayCount(prayerId, it.prayCount + 1) }
            try {
                supabase.from("prayer_intercessions")
                    .insert(PrayerIntercessionDto(prayerId, uid))
                Result.Success(Unit)
            } catch (e: Exception) {
                current?.let { prayerDao.updatePrayCount(prayerId, it.prayCount) }
                Result.Error("Couldn't record your prayer. Try again.", e)
            }
        } catch (e: Exception) {
            Result.Error("Couldn't record your prayer. Try again.", e)
        }
    }

    private suspend fun pendingIntercessions(): Set<String> = runCatching {
        offlineSyncDao.getPending()
            .asSequence()
            .filter { it.action == SyncActions.INTERCEDE }
            .mapNotNull {
                runCatching {
                    Json.decodeFromString<InterceedePayload>(it.payload).prayerId
                }.getOrNull()
            }
            .toSet()
    }.getOrDefault(emptySet())

    override suspend fun markAnswered(prayerId: String): Result<Unit> = try {
        if (networkMonitor.isOnline) {
            supabase.from("prayers").update({ set("status", "answered") }) {
                filter { eq("id", prayerId) }
            }
        }
        prayerDao.updateStatus(prayerId, "answered")
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Couldn't update the prayer. Try again.", e)
    }

    override suspend fun getMyPrayers(): Result<List<Prayer>> {
        val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()
        ?: return Result.Error("Your session expired. Please sign in again.")

        val pending = pendingMyPrayers(uid)

        if (!networkMonitor.isOnline) {
            val cached = prayerDao.getAllActive().first()
                .filter { it.userId == uid }
                .map { it.toDomain() }
            return Result.Success(pending + cached)
        }

        return try {
            val dtos = supabase.from("prayers")
                .select {
                    filter { eq("user_id", uid) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<PrayerDto>()
            Result.Success(pending + dtos.map { it.toDomain() })
        } catch (_: Exception) {
            val cached = prayerDao.getAllActive().first()
                .filter { it.userId == uid }
                .map { it.toDomain() }
            Result.Success(pending + cached)
        }
    }

    private suspend fun pendingMyPrayers(uid: String): List<Prayer> {
        val pendingEntries = runCatching {
            offlineSyncDao.getPending()
                .filter { it.action == SyncActions.POST_PRAYER }
                .mapNotNull {
                    runCatching {
                        Json.decodeFromString<PostPrayerPayload>(it.payload)
                    }.getOrNull()
                }
                .filter { (it.userId ?: uid) == uid }
        }.getOrDefault(emptyList())
        if (pendingEntries.isEmpty()) return emptyList()

        val myName = prefs.userName.first()
        return pendingEntries.mapNotNull { p ->
            val localId = p.localId ?: return@mapNotNull null
            val row = prayerDao.getById(localId) ?: return@mapNotNull null
            val prayer = row.toDomain()
            if (!prayer.isAnonymous && prayer.userName.isNullOrBlank() && !myName.isNullOrBlank()) {
                prayer.copy(userName = myName)
            } else prayer
        }
    }

    override suspend fun updatePrayerContent(
        prayerId: String,
        content: String
    ): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to edit.")
        }
        return try {
            supabase.from("prayers").update({ set("content", content) }) {
                filter { eq("id", prayerId) }
            }
            prayerDao.updateContent(prayerId, content)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Couldn't save your changes. Try again.", e)
        }
    }

    override suspend fun deletePrayer(prayerId: String): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to delete.")
        }
        return try {
            supabase.from("prayers").delete { filter { eq("id", prayerId) } }
            prayerDao.deleteById(prayerId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Couldn't delete the prayer. Try again.", e)
        }
    }

    override suspend fun getMyIntercessions(): Result<Set<String>> {
        val pending = pendingIntercessions()
        return try {
            val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()
            ?: return Result.Success(pending)
            if (!networkMonitor.isOnline) return Result.Success(pending)
            val rows = supabase.from("prayer_intercessions")
                .select { filter { eq("user_id", uid) } }
                .decodeList<PrayerIntercessionRow>()
            Result.Success(rows.map { it.prayerId }.toSet() + pending)
        } catch (e: Exception) {
            Result.Success(pending)
        }
    }

    override fun subscribeToPrayCount(prayerId: String): Flow<Int> = callbackFlow {
        val channel = supabase.channel("prayer_intercessions_$prayerId")
        val changes = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "prayer_intercessions"
        }

        suspend fun pushCount() {
            runCatching {
                val rows = supabase.from("prayer_intercessions")
                    .select { filter { eq("prayer_id", prayerId) } }
                    .decodeList<PrayerIntercessionRow>()
                trySend(rows.size)
            }
        }

        val job = launch {
            changes.collect { pushCount() }
        }
        channel.subscribe()
        pushCount()

        awaitClose {
            job.cancel()
            channelCleanupScope.launch(NonCancellable) {
                runCatching { supabase.realtime.removeChannel(channel) }
            }
        }
    }

    private val channelCleanupScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

}
