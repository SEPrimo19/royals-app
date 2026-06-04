package com.grace.app.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * One place that decides "when should the daily reminder workers next fire."
 * Schedules a PeriodicWorkRequest with a 24-hour interval and an
 * initialDelay timed to the user's chosen hour. Re-enqueue with REPLACE
 * whenever the user changes the hour in Settings — KEEP on app start so
 * we don't reset the alignment every cold launch.
 */
object ReminderScheduler {

    fun schedulePrayerReminder(
        context: Context,
        hour: Int,
        policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
    ) {
        val request = PeriodicWorkRequestBuilder<PrayerReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelayMinutes(hour), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PrayerReminderWorker.UNIQUE_NAME, policy, request
        )
    }

    fun scheduleDevoReminder(
        context: Context,
        hour: Int,
        policy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.KEEP
    ) {
        val request = PeriodicWorkRequestBuilder<DevoReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(initialDelayMinutes(hour), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DevoReminderWorker.UNIQUE_NAME, policy, request
        )
    }

    private fun initialDelayMinutes(hour: Int): Long {
        val now = LocalDateTime.now()
        var next = now.withHour(hour.coerceIn(0, 23))
            .withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMinutes().coerceAtLeast(1)
    }
}
