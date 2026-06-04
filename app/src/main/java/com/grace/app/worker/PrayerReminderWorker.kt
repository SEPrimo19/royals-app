package com.grace.app.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.grace.app.MainActivity
import com.grace.app.R
import com.grace.app.core.NotificationChannels
import com.grace.app.data.datastore.UserPreferencesRepo
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Daily local reminder that nudges the user toward the Prayer Wall. Honors
 * the user's per-channel notification toggle so opting out is a single
 * switch in Settings rather than uninstalling the worker.
 *
 * Fires from a PeriodicWorkRequest scheduled by ReminderScheduler — the
 * worker itself only builds the notification, no scheduling logic lives
 * here.
 */
@HiltWorker
class PrayerReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val prefs: UserPreferencesRepo
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!prefs.notifPrayerEnabled.first()) return Result.success()

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("destination", "prayer")
        }
        val pi = PendingIntent.getActivity(
            appContext,
            UNIQUE_NOTIF_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(
            appContext, NotificationChannels.PRAYER
        )
            .setContentTitle(appContext.getString(R.string.prayer_reminder_title))
            .setContentText(appContext.getString(R.string.prayer_reminder_body))
            .setSmallIcon(R.drawable.ic_grace_notification)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        appContext.getSystemService(NotificationManager::class.java)
            .notify(UNIQUE_NOTIF_ID, notification)
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "grace_prayer_reminder"
        // Stable id so a new fire replaces the previous one in the tray
        // instead of stacking up a dozen identical reminders.
        const val UNIQUE_NOTIF_ID = 0x9700
    }
}
