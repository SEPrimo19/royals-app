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

    private fun drainOfflineQueueOnStart() {
        WorkManager.getInstance(this).enqueueUniqueWork(
            OfflineSyncWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<OfflineSyncWorker>().build()
        )
    }

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

    private fun scheduleDailyReminders() {
        appScope.launch {
            val prayerHour = prefs.prayerReminderHour.first()
            val devoHour = prefs.devoReminderHour.first()
            val gamesHour = prefs.bibleGamesReminderHour.first()
            ReminderScheduler.schedulePrayerReminder(this@GraceApplication, prayerHour)
            ReminderScheduler.scheduleDevoReminder(this@GraceApplication, devoHour)
            ReminderScheduler.scheduleBibleGamesReminder(this@GraceApplication, gamesHour)
        }
    }

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
