package com.grace.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.grace.app.core.NotificationChannels
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.worker.DevoSyncWorker
import com.grace.app.worker.OfflineSyncWorker
import com.grace.app.worker.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class GraceApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var prefs: UserPreferencesRepo

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Hilt-aware WorkManager so @HiltWorker constructor injection works.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        enableStrictModeInDebug()
        createNotificationChannels()
        scheduleNightlyDevoSync()
        scheduleDailyReminders()
        drainOfflineQueueOnStart()
        schedulePeriodicOfflineDrain()
    }

    /**
     * Surfaces accidental main-thread I/O during development. penaltyLog only
     * (no penaltyDeath) — we want noisy warnings in Logcat, not random
     * crashes on existing code paths that may still have stray violations.
     *
     * Look for "StrictMode policy violation" in Logcat after any UI hang;
     * the stack trace points to the exact disk read / network call / DB
     * query that ran on the main thread. Each one is an ANR waiting to
     * happen on a slow device.
     *
     * Release builds skip this entirely (zero overhead).
     */
    private fun enableStrictModeInDebug() {
        if (!BuildConfig.DEBUG) return
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .detectCustomSlowCalls()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectActivityLeaks()
                .penaltyLog()
                .build()
        )
    }

    /**
     * One-shot drain on every cold start. NetworkMonitor only fires on
     * offline→online TRANSITIONS, so a user who launches the app already
     * online (or signs back in without a connectivity flip) would otherwise
     * never see their queued offline posts sync. KEEP policy means we don't
     * stomp an in-flight worker scheduled by the network callback.
     */
    private fun drainOfflineQueueOnStart() {
        WorkManager.getInstance(this).enqueueUniqueWork(
            OfflineSyncWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<OfflineSyncWorker>().build()
        )
    }

    /**
     * Belt-and-suspenders: even if the device is on a flaky network where
     * neither onAvailable nor onCapabilitiesChanged ever fires usefully
     * (broken WiFi captive portal, Doze, etc), this guarantees the queue
     * is checked every 15 minutes. 15 min is WorkManager's hard minimum
     * for periodic work. Without this, a user on a stuck network would
     * have to cold-start the app or sign out / sign in to trigger a drain.
     */
    private fun schedulePeriodicOfflineDrain() {
        val periodic = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
            15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            PERIODIC_OFFLINE_DRAIN_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodic
        )
    }

    private companion object {
        const val PERIODIC_OFFLINE_DRAIN_NAME = "grace_offline_sync_periodic"
    }

    /**
     * Reads the user's chosen reminder hours (DataStore is async, hence the
     * coroutine launch) and schedules the two daily Periodic workers with
     * KEEP — so a cold start never resets alignment with the previously
     * scheduled fire time. Settings re-enqueues with REPLACE when the user
     * changes the hour.
     */
    private fun scheduleDailyReminders() {
        appScope.launch {
            val prayerHour = prefs.prayerReminderHour.first()
            val devoHour = prefs.devoReminderHour.first()
            ReminderScheduler.schedulePrayerReminder(this@GraceApplication, prayerHour)
            ReminderScheduler.scheduleDevoReminder(this@GraceApplication, devoHour)
        }
    }

    // All channels are created before any notification is ever posted.
    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        val channels = listOf(
            NotificationChannel(
                NotificationChannels.PRAYER,
                getString(R.string.channel_prayer_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = getString(R.string.channel_prayer_desc) },
            NotificationChannel(
                NotificationChannels.DEVOTIONAL,
                getString(R.string.channel_devotional_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = getString(R.string.channel_devotional_desc) },
            NotificationChannel(
                NotificationChannels.MESSAGES,
                getString(R.string.channel_messages_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = getString(R.string.channel_messages_desc) },
            NotificationChannel(
                NotificationChannels.COMMUNITY,
                getString(R.string.channel_community_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = getString(R.string.channel_community_desc) }
        )
        manager.createNotificationChannels(channels)
    }

    private fun scheduleNightlyDevoSync() {
        val request = PeriodicWorkRequestBuilder<DevoSyncWorker>(
            24, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DevoSyncWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
