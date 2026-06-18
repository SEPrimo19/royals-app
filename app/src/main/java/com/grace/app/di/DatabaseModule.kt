package com.grace.app.di

import android.content.Context
import androidx.room.Room
import com.grace.app.data.local.GraceDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): GraceDatabase = Room.databaseBuilder(
        context,
        GraceDatabase::class.java,
        GraceDatabase.DB_NAME
    ).fallbackToDestructiveMigration().build()

    @Provides fun provideDevotionalDao(db: GraceDatabase): DevotionalDao = db.devotionalDao()
    @Provides fun providePrayerDao(db: GraceDatabase): PrayerDao = db.prayerDao()
    @Provides fun providePostDao(db: GraceDatabase): PostDao = db.postDao()
    @Provides fun provideUserDao(db: GraceDatabase): UserDao = db.userDao()
    @Provides fun provideVerseDao(db: GraceDatabase): VerseDao = db.verseDao()
    @Provides fun provideOfflineSyncDao(db: GraceDatabase): OfflineSyncDao = db.offlineSyncDao()
    @Provides
    fun provideUserDevoProgressDao(db: GraceDatabase): UserDevoProgressDao =
        db.userDevoProgressDao()
    @Provides fun provideEventDao(db: GraceDatabase): EventDao = db.eventDao()

    @Provides
    fun provideWeeklyMeditationDao(db: GraceDatabase): WeeklyMeditationDao =
        db.weeklyMeditationDao()

    @Provides
    fun provideMeditationSubmissionDao(db: GraceDatabase): MeditationSubmissionDao =
        db.meditationSubmissionDao()

    @Provides
    fun provideDiscipleshipDao(
        db: GraceDatabase
    ): com.grace.app.data.local.dao.DiscipleshipDao = db.discipleshipDao()
}
