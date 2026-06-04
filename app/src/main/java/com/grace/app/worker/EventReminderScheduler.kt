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

/**
 * Schedules a OneTimeWorkRequest per upcoming event so the user gets a
 * heads-up ~1 hour before it starts. Idempotent: re-running on the same
 * event id REPLACEs the prior request — safe to call after every event
 * fetch. Past events are skipped silently.
 */
object EventReminderScheduler {

    private const val LEAD_MINUTES = 60L

    /**
     * (Re)schedule reminders for all events whose start is more than a
     * minute in the future. WorkManager guarantees only one pending
     * request per unique name; calling this repeatedly is the right thing.
     */
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

    /** Cancel a single event's pending reminder — call from delete-event. */
    fun cancel(context: Context, eventId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(EventReminderWorker.uniqueName(eventId))
    }

    /**
     * Returns minutes until we should fire, or null if it's too late
     * (already past, or starting in less than 5 min — WorkManager won't
     * fire that promptly anyway, and a "starts in 4 min" notif feels
     * stale by the time it lands).
     */
    private fun computeDelayMinutes(eventStart: LocalDateTime): Long? {
        val fireAt = eventStart.minusMinutes(LEAD_MINUTES)
        val now = LocalDateTime.now()
        if (!fireAt.isAfter(now)) return null
        val mins = Duration.between(now, fireAt).toMinutes()
        return if (mins < 5) null else mins
    }
}
