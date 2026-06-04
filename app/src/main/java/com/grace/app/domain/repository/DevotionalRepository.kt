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

    /**
     * Reactive count of devotionals the current user has marked complete.
     * Backs My Progress. Emits 0 if no user is signed in or no rows exist.
     */
    fun getCompletedCount(): Flow<Int>

    /**
     * Pulls all user_devo_progress rows for the current user from Supabase
     * and upserts into Room. Closes the "100% devotional resets to 0%
     * after reinstall" gap — without this, Room is empty on fresh install
     * even though the server has the completion rows. Safe to call any
     * time; idempotent. Best-effort: returns Success even if offline.
     */
    suspend fun syncMyDevoProgress(): Result<Unit>

    /**
     * Reverse-chronological list of every devotional the current user has
     * completed, with the decrypted journal entry merged in. Reactive so
     * the screen refreshes when a new devotional is marked complete in
     * another session. Rows with no matching devotional in Room (e.g. a
     * local-daily devotional that aged out) are dropped silently.
     */
    fun getMyJournal(): Flow<Result<List<JournalEntry>>>
}
