package com.grace.app.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.grace.app.domain.model.Event
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object EventReminderScheduler {

    private const val LEAD_MINUTES = 60L

    fun scheduleAll(context: Context, events: List<Event>) {
        val wm = WorkManager.getInstance(context)
        events.forEach { event ->
            val delay = computeDelayMinutes(event.eventDate) ?: return@forEach
            val data = Data.Builder()
                .putString(EventReminderWorker.KEY_EVENT_ID, event.id)
                .putString(EventReminderWorker.KEY_TITLE, event.title)
                .putString(EventReminderWorker.KEY_LOCATION, event.location)
                .putString(EventReminderWorker.KEY_START_ISO, event.eventDate.toString())
                .build()
            val request = OneTimeWorkRequestBuilder<EventReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .setInputData(data)
                .build()
            wm.enqueueUniqueWork(
                EventReminderWorker.uniqueName(event.id),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    fun cancel(context: Context, eventId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(EventReminderWorker.uniqueName(eventId))
    }

    private fun computeDelayMinutes(eventStart: LocalDateTime): Long? {
        val fireAt = eventStart.minusMinutes(LEAD_MINUTES)
        val now = LocalDateTime.now()
        if (!fireAt.isAfter(now)) return null
        val mins = Duration.between(now, fireAt).toMinutes()
        return if (mins < 5) null else mins
    }
}
