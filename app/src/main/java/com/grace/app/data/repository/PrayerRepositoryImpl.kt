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
                // Fetch ALL non-archived prayers (active + answered), so the
                // reconcile below sees the full server picture and can drop a
                // locally-answered ghost too.
                val remote = supabase.from("prayers")
                    .select {
                        filter { neq("status", "archived") }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<PrayerDto>()

                // The prayers table only stores user_id, not user_name. The
                // PrayerDto's userName field exists for display but always
                // arrives null from this select. Batch-fetch real names for
                // every non-anonymous author so PrayerCard can render
                // "Maria S." instead of falling back to "A Youth in Prayer".
                // Anonymous posters are skipped — their identity is never
                // looked up at the data layer.
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

                // Phase P.3 — batched lookup for leader names so the
                // "(via {leader})" tag renders consistently. Reuses the
                // same nameByUid map (we already fetched non-anonymous
                // authors) and adds any proxy-leader ids that aren't
                // already covered.
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

                // Anonymous safeguard applied in mapper for BOTH entity & domain.
                prayerDao.insertAll(enriched.map {
                    it.toEntity(proxyLeaderName = it.postedByProxy?.let { id -> proxyNameByUid[id] })
                })
                // PROTECTED: any local row whose id is still pending in the
                // offline_sync_queue is an optimistic offline draft that the
                // OfflineSyncWorker hasn't drained yet. Without this guard,
                // deleteNotIn() purges the user's queued post the moment the
                // wall is opened online and they think their prayer vanished.
                // Once the worker drains, the queue entry disappears and the
                // next reconcile cleans up the stale local row (the real
                // server row was upserted on the same pass).
                val protectedIds = pendingPostPrayerLocalIds()
                val keepIds = remote.map { it.id }.toSet() + protectedIds
                if (keepIds.isEmpty()) prayerDao.clearAll()
                else prayerDao.deleteNotIn(keepIds.toList())
            } catch (_: Exception) {
                // Stale Room data stays visible; surface a soft error below.
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

    /** Local IDs of POST_PRAYER entries still waiting in the offline queue. */
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

        // Online happy path: write straight to Supabase and cache the
        // server row (with its real id) locally. If ANY exception fires
        // here — RLS rejection, expired JWT, transient network, decode
        // mismatch — we DO NOT drop the user's prayer. We fall through
        // to the offline queue so the worker can replay it later. The
        // original behaviour ("Try again" with the prayer silently lost)
        // was the reported regression.
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
            // Online attempt failed — log for diagnosis, then fall through
            // to the offline queue path below.
            CrashReporter.log("postPrayer online attempt failed; falling back to offline queue")
            online.exceptionOrNull()?.let { CrashReporter.recordNonFatal(it) }
        }

        // Offline (or online-failed fallback): keep an optimistic row so
        // the UI feels instant; the OfflineSyncWorker replays the insert
        // once conditions allow. Same UUID stamped on both the Room row
        // and the payload so getPrayers() can PROTECT this row from the
        // reconcile-deleteNotIn pass until the drain completes.
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
            // Genuinely fatal: Room itself couldn't accept the row.
            Result.Error("Couldn't save your prayer. Try again.", e)
        }
    }

    override suspend fun intercede(prayerId: String): Result<Unit> {
        return try {
            val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()
            ?: return Result.Error("Your session expired. Please sign in again.")

            val current = prayerDao.getById(prayerId)

            if (!networkMonitor.isOnline) {
                // Offline-first: keep the +1 locally and queue the server
                // INSERT for the next time the OfflineSyncWorker runs. The
                // count + "already prayed" UI state stay accurate during the
                // offline session because getMyIntercessions also reads
                // pending queue entries — see below.
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

            // Online path: idempotent check, then optimistic +1, then persist.
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

    /** All pending offline intercede payloads in the queue. */
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

        // ALWAYS include pending offline posts from this user. Without this
        // the user opens "My Content", sees "no prayers yet", and assumes
        // their post was lost — even though it's sitting in the offline
        // queue waiting to drain. Anonymous and named posts both included
        // because the queue stamps the real userId on the payload.
        val pending = pendingMyPrayers(uid)

        if (!networkMonitor.isOnline) {
            // Room contains only non-anonymous prayers with a userId match
            // (the anonymous safeguard mapper drops user_id to null when
            // storing). Anonymous synced posts won't appear offline — an
            // acceptable limitation since the queue layer covers pending
            // anonymous posts (the common case for "I just posted this").
            val cached = prayerDao.getAllActive().first()
                .filter { it.userId == uid }
                .map { it.toDomain() }
            return Result.Success(pending + cached)
        }

        return try {
            // Direct server query so anonymous prayers are included — server
            // stores user_id even when is_anonymous = true; the public
            // mapper strips it but here we want them back.
            val dtos = supabase.from("prayers")
                .select {
                    filter { eq("user_id", uid) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<PrayerDto>()
            Result.Success(pending + dtos.map { it.toDomain() })
        } catch (_: Exception) {
            // Soft-fail: at minimum show pending + cached so the user
            // never sees a false-empty My Content.
            val cached = prayerDao.getAllActive().first()
                .filter { it.userId == uid }
                .map { it.toDomain() }
            Result.Success(pending + cached)
        }
    }

    /**
     * Pending POST_PRAYER queue entries for the current user, materialised
     * as Prayer objects so My Content can display them alongside synced
     * prayers. Display name is filled from DataStore for non-anonymous
     * entries so the user sees their own name rather than the
     * "A Youth in Prayer" fallback.
     */
    private suspend fun pendingMyPrayers(uid: String): List<Prayer> {
        val pendingEntries = runCatching {
            offlineSyncDao.getPending()
                .filter { it.action == SyncActions.POST_PRAYER }
                .mapNotNull {
                    runCatching {
                        Json.decodeFromString<PostPrayerPayload>(it.payload)
                    }.getOrNull()
                }
                // Legacy entries (userId == null, pre-migration) fall through
                // to current uid so they still show. New entries enforce match.
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
            // Keep local cache in sync so the next refresh doesn't flash old text.
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
        // Pending queue is read in BOTH online and offline branches so the
        // user's "already prayed" visual stays accurate across app restarts
        // while offline (otherwise tapping pray → killing the app → reopening
        // would lose the heart state until network returns and worker drains).
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
            // Soft-fail: at worst the user sees pending-only state.
            Result.Success(pending)
        }
    }

    /**
     * Live pray-count via Supabase Realtime. Unique channel per prayer; the
     * channel is removed when the collecting scope is cancelled (awaitClose).
     */
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
        pushCount() // initial value

        awaitClose {
            job.cancel()
            launch { supabase.realtime.removeChannel(channel) }
        }
    }

}
