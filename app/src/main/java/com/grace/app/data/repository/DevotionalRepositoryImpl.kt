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
                    // A pastor-authored devotional exists for today — use it and
                    // wipe any client-generated fallback so we don't show two.
                    fetchAndCacheVerse(dto.verseRef)
                    devotionalDao.insert(dto.toEntity())
                    devotionalDao.deleteById(generatedIdFor(todayDate))
                } else {
                    // No server devotional for today: generate one from the
                    // verse catalogue if we haven't already today.
                    val existing = devotionalDao.getByDate(today).first()
                    if (existing == null) {
                        generateDailyDevotional(todayDate)?.let {
                            devotionalDao.insert(it)
                        }
                    }
                }
            } catch (_: Exception) {
                networkFailed = true
            }
        }

        // Room is the source of truth — its flow drives the UI from here.
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

    // ----- daily-devotional fallback ----------------------------------------

    /** Stable id so re-running for the same day is idempotent (REPLACE). */
    private fun generatedIdFor(date: LocalDate): String = "$LOCAL_DAILY_ID_PREFIX$date"

    companion object {
        /** Marker for devotionals that live only in Room (not in Supabase). */
        const val LOCAL_DAILY_ID_PREFIX = "daily-"
    }

    /** Live fetch + template a devotional around today's verse from the catalogue. */
    private suspend fun generateDailyDevotional(date: LocalDate): DevotionalEntity? {
        return try {
            val ref = VerseCatalogue.verseFor(date)
            val response = bibleApi.getVerse(reference = encodeRef(ref))
            val verseText = cleanVerseText(response.text)
            if (verseText.isBlank()) return null
            // Cache the verse so the offline-verse banner / widget can use it too.
            verseDao.insert(VerseEntity(ref = response.reference.ifBlank { ref }, text = verseText))

            DevotionalEntity(
                id = generatedIdFor(date),
                scheduledDate = date.toString(),
                title = "Today's Word",
                verseRef = response.reference.ifBlank { ref },
                verseText = verseText,
                // Auto-generated fallback content — used on days when no
                // leader has created a custom devotional. Length matters
                // here: youth perceive 1-2 sentence prompts as "the app
                // is broken / cut off" rather than as a starter, so we
                // pad with real guiding paragraphs.
                reflection = "Take a moment to read today's verse slowly, out loud if you can. Then read it a second time and " +
                    "listen for one word or phrase that catches your attention.\n\n" +
                    "Why might God be highlighting that word for YOU today? Maybe it's something you needed to hear " +
                    "this week — about your studies, your family, a friendship, a struggle you've been carrying. " +
                    "Maybe it's a reminder of who God says you are when the world tries to tell you otherwise.\n\n" +
                    "God's Word isn't meant to sit on a page. It's meant to walk with you into the rest of your " +
                    "day. What's one small way you can live out this verse before the sun sets?",
                prayerStarter = "Lord, open my heart to receive what You are saying through Your Word today. " +
                    "Quiet the noise in my mind so I can hear Your voice clearly.\n\n" +
                    "Help me not just to read this verse, but to live it — let it shape how I think, how I speak, " +
                    "and how I love the people around me this week. When I'm tempted to forget, bring this verse " +
                    "back to my heart.\n\n" +
                    "I want to walk closely with You. Teach me what I don't yet understand. Heal what's hurting. " +
                    "Use me in ways I don't expect. In Jesus' name, amen.",
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

    /** bible-api.com accepts "John 3:16" with spaces URL-encoded. */
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

        // Encrypt before anything is persisted (Room or Supabase).
        val cipher = journalCrypto.encrypt(journalEntry)
        val nowIso = java.time.OffsetDateTime.now().toString()

        // Optimistic local write.
        progressDao.upsert(
            UserDevoProgressEntity(
                userId = uid,
                devoId = devoId,
                completedAt = nowIso,
                journalEntry = cipher
            )
        )

        updateStreak()

        // Client-generated daily devotionals exist only in Room; their id is a
        // string like "daily-2026-05-21", and Supabase's user_devo_progress.devo_id
        // is UUID with an FK to devotionals(id). Pushing progress for them would
        // fail twice over (UUID syntax + missing FK target). Keep progress local
        // for those — streak still updates remotely via StreakWorker.
        val isLocalDaily = devoId.startsWith(LOCAL_DAILY_ID_PREFIX)

        if (isLocalDaily) {
            // Daily-fallback completion: push to the sidecar table so a
            // reinstall / second-device sign-in can rebuild Room from
            // the server. No FK against devotionals(id), so the
            // "daily-YYYY-MM-DD" key fits cleanly.
            if (networkMonitor.isOnline) {
                runCatching {
                    supabase.from("user_client_devo_progress").upsert(
                        ClientDevoProgressDto(
                            userId = uid,
                            clientDevoKey = devoId,
                            journalEntry = cipher
                        )
                    )
                }
                WorkManager.getInstance(context)
                    .enqueue(OneTimeWorkRequestBuilder<StreakWorker>().build())
            } else {
                // Offline: queue the same sync action — the offline
                // worker's MARK_DEVO_COMPLETE handler will re-call this
                // markComplete path when connectivity returns and the
                // sidecar upsert will run then.
                offlineSyncDao.insert(
                    OfflineSyncEntity(
                        id = UUID.randomUUID().toString(),
                        action = SyncActions.MARK_DEVO_COMPLETE,
                        payload = Json.encodeToString(
                            MarkDevoCompletePayload(uid, devoId, cipher)
                        )
                    )
                )
            }
        } else if (networkMonitor.isOnline) {
            supabase.from("user_devo_progress").upsert(
                UserDevoProgressDto(userId = uid, devoId = devoId, journalEntry = cipher)
            )
            WorkManager.getInstance(context)
                .enqueue(OneTimeWorkRequestBuilder<StreakWorker>().build())
        } else {
            offlineSyncDao.insert(
                OfflineSyncEntity(
                    id = UUID.randomUUID().toString(),
                    action = SyncActions.MARK_DEVO_COMPLETE,
                    payload = Json.encodeToString(
                        MarkDevoCompletePayload(uid, devoId, cipher)
                    )
                )
            )
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Could not save your devotional. Try again.", e)
    }

    // Idempotent: completing twice on the same day never double-counts.
    private suspend fun updateStreak() {
        val today = LocalDate.now()
        val last = prefs.lastDevoDate.first()
        val current = prefs.devoStreak.first()
        val newStreak = when (last) {
            today.toString() -> current.coerceAtLeast(1) // already counted today
            today.minusDays(1).toString() -> current + 1 // consecutive day
            else -> 1 // first ever, or streak broken
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
            // Pull all server-side progress rows for this user. The
            // composite PK (user_id, devo_id) means upsert is the right
            // operation — local rows for the same devo get replaced with
            // whatever's canonical on the server. Encrypted journal
            // entries pass through as ciphertext; we don't decrypt here.
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
                // Loop is fine — one row per devotional ever completed,
                // not thousands. Room's @Insert(REPLACE) handles upsert
                // via composite PK conflict.
                entities.forEach { progressDao.upsert(it) }
            }

            // ALSO pull from the client-daily sidecar table. These rows
            // track completion of "daily-YYYY-MM-DD" fallback devotionals
            // that aren't writable to user_devo_progress because of its
            // UUID FK. Without this, a reinstall / second device drops
            // back to 0% even after the user already completed today.
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
            // Best-effort. If sync fails the screen still works against Room.
            Result.Success(Unit)
        }
    }

    override fun getMyJournal(): Flow<Result<List<JournalEntry>>> = flow {
        val uid = prefs.userId.first()
        if (uid.isNullOrBlank()) {
            emit(Result.Success(emptyList()))
            return@flow
        }
        // Best-effort: backfill any rows the server has but Room is missing
        // (covers fresh installs where the user has past entries on the
        // server but Room is empty). Failures here are non-fatal — we still
        // show whatever Room has.
        runCatching { syncMyDevoProgress() }

        emitAll(
            progressDao.getAllForUser(uid).map { rows ->
                // Newest first.
                val sorted = rows.sortedByDescending { it.completedAt }
                val entries = sorted.mapNotNull { p ->
                    val devo = devotionalDao.getById(p.devoId) ?: return@mapNotNull null
                    val cipher = p.journalEntry.orEmpty()
                    val plain = if (cipher.isBlank()) "" else journalCrypto.decrypt(cipher)
                    // Empty plaintext from a non-empty cipher means decryption
                    // failed — usually the Keystore key is gone (reinstall).
                    val readable = cipher.isBlank() || plain.isNotBlank()
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

    /**
     * `completed_at` is stored as an ISO string. We accept either an
     * OffsetDateTime ("...T12:34:56+00:00") or a plain LocalDate. On parse
     * failure fall back to today rather than crash the whole list.
     */
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
