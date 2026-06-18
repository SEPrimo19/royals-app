package com.grace.app.data.repository

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.grace.app.core.JournalCrypto
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.local.dao.DevotionalDao
import com.grace.app.data.local.dao.OfflineSyncDao
import com.grace.app.data.local.dao.UserDevoProgressDao
import com.grace.app.data.local.dao.VerseDao
import com.grace.app.data.local.entity.DevotionalEntity
import com.grace.app.data.local.entity.OfflineSyncEntity
import com.grace.app.data.local.entity.UserDevoProgressEntity
import com.grace.app.data.local.entity.VerseEntity
import com.grace.app.data.remote.bible.BibleApiService
import com.grace.app.data.remote.bible.VerseCatalogue
import com.grace.app.data.remote.supabase.dto.ClientDevoProgressDto
import com.grace.app.data.remote.supabase.dto.DevotionalDto
import com.grace.app.data.remote.supabase.dto.UserDevoProgressDto
import com.grace.app.data.remote.supabase.dto.mapper.toDomain
import com.grace.app.data.remote.supabase.dto.mapper.toEntity
import com.grace.app.data.sync.MarkDevoCompletePayload
import com.grace.app.data.sync.SyncActions
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.model.Devotional
import com.grace.app.domain.model.JournalEntry
import com.grace.app.domain.repository.DevotionalRepository
import com.grace.app.domain.util.Result
import com.grace.app.worker.StreakWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DevotionalRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val devotionalDao: DevotionalDao,
    private val verseDao: VerseDao,
    private val progressDao: UserDevoProgressDao,
    private val offlineSyncDao: OfflineSyncDao,
    private val supabase: SupabaseClient,
    private val bibleApi: BibleApiService,
    private val networkMonitor: NetworkMonitor,
    private val prefs: UserPreferencesRepo,
    private val journalCrypto: JournalCrypto
) : DevotionalRepository {

    private val htmlTagRegex = Regex("<[^>]*>")

    override fun getTodayDevotional(): Flow<Result<Devotional>> = flow {
        val todayDate = LocalDate.now()
        val today = todayDate.toString()
        var networkFailed = false

        if (networkMonitor.isOnline) {
            try {
                val dto = supabase.from("devotionals")
                    .select { filter { eq("scheduled_date", today) } }
                    .decodeSingleOrNull<DevotionalDto>()
                if (dto != null) {
                    fetchAndCacheVerse(dto.verseRef)
                    devotionalDao.insert(dto.toEntity())
                    devotionalDao.deleteById(generatedIdFor(todayDate))
                }
            } catch (_: Exception) {
                networkFailed = true
            }
        }

        val alreadyHave = devotionalDao.getByDate(today).first()
        if (alreadyHave == null) {
            generateDailyDevotional(todayDate)?.let { devotionalDao.insert(it) }
        }

        emitAll(
            devotionalDao.getByDate(today).map { entity ->
                when {
                    entity != null -> Result.Success(entity.toDomain())
                    networkFailed -> Result.Error(
                        "Could not load today's devotional. Showing cached version."
                    )
                    else -> Result.Error("No devotional available yet. Check back soon.")
                }
            }
        )
    }.flowOn(Dispatchers.IO)

    suspend fun fetchAndCacheVerse(ref: String): String {
        return try {
            val cached = verseDao.getByRef(ref)
            if (cached != null) return cached.text

            if (networkMonitor.isOnline) {
                val response = bibleApi.getVerse(reference = encodeRef(ref))
                val clean = cleanVerseText(response.text)
                verseDao.insert(VerseEntity(ref = ref, text = clean))
                clean
            } else {
                "This verse will appear once you connect to the internet."
            }
        } catch (_: Exception) {
            "This verse will appear once you connect to the internet."
        }
    }


    private fun generatedIdFor(date: LocalDate): String = "$LOCAL_DAILY_ID_PREFIX$date"

    companion object {
        const val LOCAL_DAILY_ID_PREFIX = "daily-"
    }

    private suspend fun generateDailyDevotional(date: LocalDate): DevotionalEntity? {
        return try {
            val (ref, verseText) = VerseCatalogue.verseAndTextFor(date)
            if (verseText.isBlank()) return null
            verseDao.insert(VerseEntity(ref = ref, text = verseText))

            val (reflectionText, prayerText) =
                VerseCatalogue.devotionalContentFor(ref) ?: (
                    "Read today's verse slowly. What one word or phrase catches " +
                        "your attention, and why might God be highlighting it for " +
                        "you today? How could you live it out before the day ends?" to
                    "Father, open my heart to receive what You're saying through " +
                        "Your Word today. Help me not just to read this verse, but " +
                        "to live it. In Jesus' name, amen."
                )

            DevotionalEntity(
                id = generatedIdFor(date),
                scheduledDate = date.toString(),
                title = "Today's Word",
                verseRef = ref,
                verseText = verseText,
                reflection = reflectionText,
                prayerStarter = prayerText,
                journalPrompt = "Write down one word or phrase from today's verse and why it stood out to you. " +
                    "Then write a short prayer to God about it — what you're thankful for, what you're struggling " +
                    "with, or what you're asking Him to do in your life.",
                planId = null,
                createdBy = null,
                createdAt = OffsetDateTime.now().toString()
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun encodeRef(ref: String): String =
        java.net.URLEncoder.encode(ref, Charsets.UTF_8.name())

    private fun cleanVerseText(raw: String): String =
        htmlTagRegex.replace(raw, "").replace("\n", " ").replace(Regex("\\s+"), " ").trim()

    override suspend fun markComplete(
        devoId: String,
        journalEntry: String
    ): Result<Unit> = try {
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: prefs.userId.first()
            ?: return Result.Error("Your session expired. Please sign in again.")

        val stored = journalEntry
        val nowIso = java.time.OffsetDateTime.now().toString()

        progressDao.upsert(
            UserDevoProgressEntity(
                userId = uid,
                devoId = devoId,
                completedAt = nowIso,
                journalEntry = stored
            )
        )

        updateStreak()

        val isLocalDaily = devoId.startsWith(LOCAL_DAILY_ID_PREFIX)

        if (isLocalDaily) {
            val pushed = networkMonitor.isOnline && runCatching {
                supabase.from("user_client_devo_progress").upsert(
                    ClientDevoProgressDto(
                        userId = uid,
                        clientDevoKey = devoId,
                        journalEntry = stored
                    )
                )
            }.isSuccess
            if (pushed) {
                WorkManager.getInstance(context)
                    .enqueue(OneTimeWorkRequestBuilder<StreakWorker>().build())
            } else {
                offlineSyncDao.insert(
                    OfflineSyncEntity(
                        id = UUID.randomUUID().toString(),
                        action = SyncActions.MARK_DEVO_COMPLETE,
                        payload = Json.encodeToString(
                            MarkDevoCompletePayload(uid, devoId, stored)
                        )
                    )
                )
            }
        } else if (networkMonitor.isOnline) {
            supabase.from("user_devo_progress").upsert(
                UserDevoProgressDto(userId = uid, devoId = devoId, journalEntry = stored)
            )
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<StreakWorker>().build())
        } else {
            offlineSyncDao.insert(
                OfflineSyncEntity(
                    id = UUID.randomUUID().toString(),
                    action = SyncActions.MARK_DEVO_COMPLETE,
                    payload = Json.encodeToString(
                        MarkDevoCompletePayload(uid, devoId, stored)
                    )
                )
            )
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Could not save your devotional. Try again.", e)
    }

    private suspend fun updateStreak() {
        val today = LocalDate.now()
        val last = prefs.lastDevoDate.first()
        val current = prefs.devoStreak.first()
        val newStreak = when (last) {
            today.toString() -> current.coerceAtLeast(1)
            today.minusDays(1).toString() -> current + 1
            else -> 1
        }
        prefs.setStreak(newStreak)
        prefs.setLastDevoDate(today.toString())
    }

    override fun getStreak(): Flow<Int> = prefs.devoStreak

    override fun getCompletedCount(): Flow<Int> = flow {
        val uid = prefs.userId.first()
        if (uid.isNullOrBlank()) {
            emit(0)
            return@flow
        }
        emitAll(progressDao.getAllForUser(uid).map { it.size })
    }.flowOn(Dispatchers.IO)

    override suspend fun syncMyDevoProgress(): Result<Unit> {
        val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()
        if (uid.isNullOrBlank() || !networkMonitor.isOnline) {
            return Result.Success(Unit)
        }
        return try {
            val rows = supabase.from("user_devo_progress")
                .select { filter { eq("user_id", uid) } }
                .decodeList<UserDevoProgressDto>()
            if (rows.isNotEmpty()) {
                val entities = rows.map { dto ->
                    UserDevoProgressEntity(
                        userId = dto.userId,
                        devoId = dto.devoId,
                        completedAt = dto.completedAt ?: "",
                        journalEntry = dto.journalEntry
                    )
                }
                entities.forEach { progressDao.upsert(it) }
            }

            runCatching {
                val clientRows = supabase.from("user_client_devo_progress")
                    .select { filter { eq("user_id", uid) } }
                    .decodeList<ClientDevoProgressDto>()
                clientRows.forEach { dto ->
                    progressDao.upsert(
                        UserDevoProgressEntity(
                            userId = dto.userId,
                            devoId = dto.clientDevoKey,
                            completedAt = dto.completedAt ?: "",
                            journalEntry = dto.journalEntry
                        )
                    )
                }
            }
            Result.Success(Unit)
        } catch (_: Exception) {
            Result.Success(Unit)
        }
    }

    override fun getMyJournal(): Flow<Result<List<JournalEntry>>> = flow {
        val uid = prefs.userId.first()
        if (uid.isNullOrBlank()) {
            emit(Result.Success(emptyList()))
            return@flow
        }
        runCatching { syncMyDevoProgress() }

        emitAll(
            progressDao.getAllForUser(uid).map { rows ->
                val sorted = rows.sortedByDescending { it.completedAt }
                val entries = sorted.mapNotNull { p ->
                    val devo = devotionalDao.getById(p.devoId) ?: return@mapNotNull null
                    val raw = p.journalEntry.orEmpty()
                    val legacyCipher = journalCrypto.looksEncrypted(raw)
                    val plain = when {
                        raw.isBlank() -> ""
                        legacyCipher -> journalCrypto.decrypt(raw)
                        else -> raw
                    }
                    val readable = raw.isBlank() || !legacyCipher || plain.isNotBlank()
                    JournalEntry(
                        devoId = devo.id,
                        completedAt = parseCompletedDate(p.completedAt),
                        devoTitle = devo.title,
                        verseRef = devo.verseRef,
                        verseText = devo.verseText,
                        journalPrompt = devo.journalPrompt,
                        entry = plain,
                        isReadable = readable
                    )
                }
                Result.Success(entries)
            }
        )
    }.flowOn(Dispatchers.IO)

    private fun parseCompletedDate(raw: String): LocalDate = try {
        if (raw.contains('T')) OffsetDateTime.parse(raw).toLocalDate()
        else LocalDate.parse(raw)
    } catch (_: Exception) {
        LocalDate.now()
    }

    override suspend fun syncUpcomingDevotionals(): Result<Unit> = try {
        if (!networkMonitor.isOnline) {
            Result.Error("You're offline. Devotionals will sync when you reconnect.")
        } else {
            val today = LocalDate.now().toString()
            val dtos = supabase.from("devotionals")
                .select {
                    filter { gte("scheduled_date", today) }
                    order("scheduled_date", Order.ASCENDING)
                    limit(count = 7L)
                }
                .decodeList<DevotionalDto>()
            dtos.forEach { fetchAndCacheVerse(it.verseRef) }
            devotionalDao.insertAll(dtos.map { it.toEntity() })
            Result.Success(Unit)
        }
    } catch (e: Exception) {
        Result.Error("Devotional sync failed.", e)
    }

}
