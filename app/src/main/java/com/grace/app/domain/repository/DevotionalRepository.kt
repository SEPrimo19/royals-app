package com.grace.app.domain.repository

import com.grace.app.domain.model.Devotional
import com.grace.app.domain.model.JournalEntry
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface DevotionalRepository {
    fun getTodayDevotional(): Flow<Result<Devotional>>
    suspend fun markComplete(devoId: String, journalEntry: String): Result<Unit>
    fun getStreak(): Flow<Int>
    suspend fun syncUpcomingDevotionals(): Result<Unit>

    fun getCompletedCount(): Flow<Int>

    suspend fun syncMyDevoProgress(): Result<Unit>

    fun getMyJournal(): Flow<Result<List<JournalEntry>>>
}
