package com.grace.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.grace.app.data.local.dao.DevotionalDao
import com.grace.app.data.local.dao.EventDao
import com.grace.app.data.local.dao.MeditationSubmissionDao
import com.grace.app.data.local.dao.OfflineSyncDao
import com.grace.app.data.local.dao.PostDao
import com.grace.app.data.local.dao.PrayerDao
import com.grace.app.data.local.dao.UserDao
import com.grace.app.data.local.dao.UserDevoProgressDao
import com.grace.app.data.local.dao.VerseDao
import com.grace.app.data.local.dao.WeeklyMeditationDao
import com.grace.app.data.local.entity.DevotionalEntity
import com.grace.app.data.local.entity.EventEntity
import com.grace.app.data.local.entity.MeditationSubmissionEntity
import com.grace.app.data.local.entity.OfflineSyncEntity
import com.grace.app.data.local.entity.PostEntity
import com.grace.app.data.local.entity.PrayerEntity
import com.grace.app.data.local.entity.UserDevoProgressEntity
import com.grace.app.data.local.entity.UserEntity
import com.grace.app.data.local.entity.VerseEntity
import com.grace.app.data.local.entity.WeeklyMeditationEntity

@Database(
    entities = [
        DevotionalEntity::class,
        PrayerEntity::class,
        PostEntity::class,
        UserEntity::class,
        VerseEntity::class,
        OfflineSyncEntity::class,
        UserDevoProgressEntity::class,
        EventEntity::class,
        WeeklyMeditationEntity::class,
        MeditationSubmissionEntity::class
    ],
    // Bump v8 → v9: MeditationSubmissionEntity + PrayerEntity gained
    // proxy attribution columns for Phase P.3 of Leader Proxy Mode.
    // fallbackToDestructiveMigration wipes local caches — Supabase
    // resyncs both tables on next entry.
    version = 9,
    exportSchema = true
)
abstract class GraceDatabase : RoomDatabase() {
    abstract fun devotionalDao(): DevotionalDao
    abstract fun prayerDao(): PrayerDao
    abstract fun postDao(): PostDao
    abstract fun userDao(): UserDao
    abstract fun verseDao(): VerseDao
    abstract fun offlineSyncDao(): OfflineSyncDao
    abstract fun userDevoProgressDao(): UserDevoProgressDao
    abstract fun eventDao(): EventDao
    abstract fun weeklyMeditationDao(): WeeklyMeditationDao
    abstract fun meditationSubmissionDao(): MeditationSubmissionDao

    companion object {
        const val DB_NAME = "grace.db"
    }
}
