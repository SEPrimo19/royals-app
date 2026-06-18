package com.grace.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.local.dao.OfflineSyncDao
import com.grace.app.data.local.dao.PrayerDao
import com.grace.app.data.local.entity.OfflineSyncEntity
import com.grace.app.data.remote.supabase.dto.ClientDevoProgressDto
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
                CrashReporter.log(
                    "OfflineSyncWorker item ${item.action}/${item.id} failed: ${it.message}"
                )
                CrashReporter.recordNonFatal(it)
                Outcome.FAIL
            }
            when (outcome) {
                Outcome.SUCCESS -> offlineSyncDao.deleteById(item.id)
                Outcome.SKIP -> {
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
                p.userId != null && p.userId != currentUid -> Outcome.SKIP
                else -> {
                    supabase.from("prayers").insert(
                        PrayerInsertDto(currentUid, p.content, p.isAnonymous, p.category)
                    )
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
                    if (e.message?.contains("duplicate key", ignoreCase = true) != true) {
                        throw e
                    }
                }
                Outcome.SUCCESS
            }
        }
        SyncActions.MARK_DEVO_COMPLETE -> {
            val p = json.decodeFromString<MarkDevoCompletePayload>(item.payload)
            if (p.devoId.startsWith("daily-")) {
                supabase.from("user_client_devo_progress").upsert(
                    ClientDevoProgressDto(
                        userId = p.userId,
                        clientDevoKey = p.devoId,
                        journalEntry = p.encryptedJournal
                    )
                )
                Outcome.SUCCESS
            } else {
                supabase.from("user_devo_progress").upsert(
                    UserDevoProgressDto(p.userId, p.devoId, p.encryptedJournal)
                )
                Outcome.SUCCESS
            }
        }
        else -> Outcome.SUCCESS
    }

    companion object {
        const val UNIQUE_NAME = "grace_offline_sync"
        private const val MAX_RETRIES = 3
    }
}
