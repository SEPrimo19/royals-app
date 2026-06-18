package com.grace.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.grace.app.data.datastore.UserPreferencesRepo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.first

@HiltWorker
class StreakWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val supabase: SupabaseClient,
    private val prefs: UserPreferencesRepo
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: prefs.userId.first()
        if (uid.isNullOrBlank()) {
            Result.success()
        } else {
            val streak = prefs.devoStreak.first()
            val lastDevoDate = prefs.lastDevoDate.first()
            supabase.from("users").update({
                set("streak", streak)
                if (!lastDevoDate.isNullOrBlank()) {
                    set("last_devo_at", lastDevoDate)
                }
            }) {
                filter { eq("id", uid) }
            }
            Result.success()
        }
    } catch (_: Exception) {
        Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "grace_streak_sync"
    }
}
