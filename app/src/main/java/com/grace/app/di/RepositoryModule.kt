package com.grace.app.di

import com.grace.app.data.repository.AdminRepositoryImpl
import com.grace.app.data.repository.AuthRepositoryImpl
import com.grace.app.data.repository.DevotionalRepositoryImpl
import com.grace.app.data.repository.EventRepositoryImpl
import com.grace.app.data.repository.FeedRepositoryImpl
import com.grace.app.data.repository.GamesRepositoryImpl
import com.grace.app.data.repository.LeaderRepositoryImpl
import com.grace.app.data.repository.LifeGroupRepositoryImpl
import com.grace.app.data.repository.PrayerRepositoryImpl
import com.grace.app.data.repository.WeeklyMeditationRepositoryImpl
import com.grace.app.domain.repository.AdminRepository
import com.grace.app.domain.repository.AuthRepository
import com.grace.app.domain.repository.DevotionalRepository
import com.grace.app.domain.repository.EventRepository
import com.grace.app.domain.repository.FeedRepository
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.repository.LeaderRepository
import com.grace.app.domain.repository.LifeGroupRepository
import com.grace.app.domain.repository.PrayerRepository
import com.grace.app.domain.repository.WeeklyMeditationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Repository interface → implementation bindings. Each binding is added as its
 * RepositoryImpl is written (Devotional/Prayer/Feed/Leader land in Prompts 4-6).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDevotionalRepository(
        impl: DevotionalRepositoryImpl
    ): DevotionalRepository

    @Binds
    @Singleton
    abstract fun bindPrayerRepository(impl: PrayerRepositoryImpl): PrayerRepository

    @Binds
    @Singleton
    abstract fun bindFeedRepository(impl: FeedRepositoryImpl): FeedRepository

    @Binds
    @Singleton
    abstract fun bindLeaderRepository(impl: LeaderRepositoryImpl): LeaderRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds
    @Singleton
    abstract fun bindAdminRepository(impl: AdminRepositoryImpl): AdminRepository

    @Binds
    @Singleton
    abstract fun bindLifeGroupRepository(impl: LifeGroupRepositoryImpl): LifeGroupRepository

    @Binds
    @Singleton
    abstract fun bindGamesRepository(impl: GamesRepositoryImpl): GamesRepository

    @Binds
    @Singleton
    abstract fun bindWeeklyMeditationRepository(
        impl: WeeklyMeditationRepositoryImpl
    ): WeeklyMeditationRepository
}
