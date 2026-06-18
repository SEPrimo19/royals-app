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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@HiltWorker
class EventReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val prefs: UserPreferencesRepo
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!prefs.notifCommunityEnabled.first()) return Result.success()

        val eventId = params.inputData.getString(KEY_EVENT_ID) ?: return Result.success()
        val title = params.inputData.getString(KEY_TITLE) ?: "Upcoming event"
        val location = params.inputData.getString(KEY_LOCATION)
        val startIso = params.inputData.getString(KEY_START_ISO)

        val startLocal = runCatching { LocalDateTime.parse(startIso) }.getOrNull()
        val startText = startLocal?.format(
            DateTimeFormatter.ofPattern("EEE MMM d · h:mm a", Locale.getDefault())
        ) ?: ""

        val body = if (location.isNullOrBlank()) {
            appContext.getString(R.string.event_reminder_body_no_location, startText)
        } else {
            appContext.getString(R.string.event_reminder_body, startText, location)
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("destination", "events")
            putExtra("id", eventId)
        }
        val pi = PendingIntent.getActivity(
            appContext,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(
            appContext, NotificationChannels.COMMUNITY
        )
            .setContentTitle(appContext.getString(R.string.event_reminder_title, title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSmallIcon(R.drawable.ic_grace_notification)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        appContext.getSystemService(NotificationManager::class.java)
            .notify(eventId.hashCode(), notif)
        return Result.success()
    }

    companion object {
        const val KEY_EVENT_ID = "event_id"
        const val KEY_TITLE = "title"
        const val KEY_LOCATION = "location"
        const val KEY_START_ISO = "start_iso"
        fun uniqueName(eventId: String) = "grace_event_reminder_$eventId"
    }
}
