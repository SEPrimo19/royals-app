package com.grace.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.local.dao.OfflineSyncDao
import com.grace.app.data.local.dao.PrayerDao
import com.grace.app.data.local.entity.OfflineSyncEntity
import com.grace.app.data.remote.supabase.dto.PrayerInsertDto
import com.grace.app.data.remote.supabase.dto.PrayerIntercessionDto
import com.grace.app.data.remote.supabase.dto.UserDevoProgressDto
import com.grace.app.data.sync.InterceedePayload
import com.grace.app.data.sync.MarkDevoCompletePayload
import com.grace.app.data.sync.PostPrayerPayload
import com.grace.app.data.sync.SyncActions
import com.grace.app.data.util.CrashReporter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * Drains the offline mutation queue when connectivity returns. Each item is
 * replayed directly against Supabase (not via the repositories) so we don't
 * re-trigger their optimistic Room writes / streak logic a second time.
 *
 * Per-item outcome is tri-state:
 *  - SUCCESS: server accepted; remove from queue and tidy any local optimistic row
 *  - SKIP:    leave in queue without bumping retry (e.g. a different user is now signed in)
 *  - FAIL:    bump retry; park as permanent failure after [MAX_RETRIES]
 *
 * Partial failures never fail the whole worker.
 */
@HiltWorker
class OfflineSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val offlineSyncDao: OfflineSyncDao,
    private val prayerDao: PrayerDao,
    private val supabase: SupabaseClient,
    private val prefs: UserPreferencesRepo
) : CoroutineWorker(appContext, params) {

    private enum class Outcome { SUCCESS, SKIP, FAIL }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        val pending = offlineSyncDao.getPending()
        if (pending.isEmpty()) return Result.success()
        CrashReporter.log("OfflineSyncWorker draining ${pending.size} item(s)")
        for (item in pending) {
            val attempt = runCatching { processItem(item) }
            val outcome = attempt.getOrElse {
                // Surface the underlying exception — without this, every
                // failure looked identical and we couldn't tell if it was
                // RLS, network, or decode that was blocking the drain.
                CrashReporter.log(
                    "OfflineSyncWorker item ${item.action}/${item.id} failed: ${it.message}"
                )
                CrashReporter.recordNonFatal(it)
                Outcome.FAIL
            }
            when (outcome) {
                Outcome.SUCCESS -> offlineSyncDao.deleteById(item.id)
                Outcome.SKIP -> {
                    // Intentionally don't touch retry_count — the item will
                    // be re-tried next drain (e.g. after the original poster
                    // signs back in).
                }
                Outcome.FAIL -> {
                    val next = item.retryCount + 1
                    offlineSyncDao.setRetryCount(item.id, next)
                    if (next >= MAX_RETRIES) {
                        offlineSyncDao.markFailed(item.id, System.currentTimeMillis())
                        CrashReporter.log(
                            "OfflineSyncWorker item ${item.action}/${item.id} permanently failed"
                        )
                    }
                }
            }
        }
        return Result.success()
    }

    private suspend fun processItem(item: OfflineSyncEntity): Outcome = when (item.action) {
        SyncActions.POST_PRAYER -> {
            val p = json.decodeFromString<PostPrayerPayload>(item.payload)
            val currentUid = supabase.auth.currentUserOrNull()?.id
                ?: prefs.userId.first()
            when {
                currentUid == null -> Outcome.FAIL
                // Defense-in-depth: if a different user is now signed in,
                // never attribute this prayer to them. Leave it in the queue
                // for whenever the original poster signs back in. Legacy
                // entries (p.userId == null, from before this field existed)
                // fall through to the current user.
                p.userId != null && p.userId != currentUid -> Outcome.SKIP
                else -> {
                    supabase.from("prayers").insert(
                        PrayerInsertDto(currentUid, p.content, p.isAnonymous, p.category)
                    )
                    // Drop the optimistic Room row now that the real one
                    // exists on the server. The next getPrayers() refetch
                    // pulls the real row with its server-generated id; without
                    // this delete the user briefly sees two cards.
                    if (p.localId != null) prayerDao.deleteById(p.localId)
                    Outcome.SUCCESS
                }
            }
        }
        SyncActions.INTERCEDE -> {
            val p = json.decodeFromString<InterceedePayload>(item.payload)
            val uid = supabase.auth.currentUserOrNull()?.id
                ?: prefs.userId.first()
            if (uid == null) Outcome.FAIL
            else {
                try {
                    supabase.from("prayer_intercessions")
                        .insert(PrayerIntercessionDto(p.prayerId, uid))
                } catch (e: Exception) {
                    // Duplicate composite-PK = the user already prayed from
                    // another session. No-op success — drop the queue item.
                    if (e.message?.contains("duplicate key", ignoreCase = true) != true) {
                        throw e
                    }
                }
                Outcome.SUCCESS
            }
        }
        SyncActions.MARK_DEVO_COMPLETE -> {
            val p = json.decodeFromString<MarkDevoCompletePayload>(item.payload)
            // Client-generated daily devotionals (id = "daily-YYYY-MM-DD")
            // don't exist in Supabase — their progress is local-only.
            if (p.devoId.startsWith("daily-")) {
                Outcome.SUCCESS
            } else {
                supabase.from("user_devo_progress").upsert(
                    UserDevoProgressDto(p.userId, p.devoId, p.encryptedJournal)
                )
                Outcome.SUCCESS
            }
        }
        // Unknown action: drop it rather than loop forever.
        else -> Outcome.SUCCESS
    }

    companion object {
        const val UNIQUE_NAME = "grace_offline_sync"
        private const val MAX_RETRIES = 3
    }
}
