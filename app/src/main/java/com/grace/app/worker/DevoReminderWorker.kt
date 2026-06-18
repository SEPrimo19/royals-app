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

@HiltWorker
class DevoReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val prefs: UserPreferencesRepo
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!prefs.notifDevoEnabled.first()) return Result.success()

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("destination", "devotional")
        }
        val pi = PendingIntent.getActivity(
            appContext,
            UNIQUE_NOTIF_ID,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(
            appContext, NotificationChannels.DEVOTIONAL
        )
            .setContentTitle(appContext.getString(R.string.devo_reminder_title))
            .setContentText(appContext.getString(R.string.devo_reminder_body))
            .setSmallIcon(R.drawable.ic_grace_notification)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        appContext.getSystemService(NotificationManager::class.java)
            .notify(UNIQUE_NOTIF_ID, notification)
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "grace_devo_reminder"
        const val UNIQUE_NOTIF_ID = 0x9701
    }
}
