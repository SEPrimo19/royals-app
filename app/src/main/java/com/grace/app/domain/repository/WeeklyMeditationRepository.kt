package com.grace.app.domain.repository

import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.WeeklyMeditation
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface WeeklyMeditationRepository {

    /**
     * Today's meditation (the one whose date range contains today).
     * Offline-first: emits Room cache immediately, then refreshes from
     * Supabase if online. Emits null when no meditation is scheduled for
     * today (e.g., gap between week 4 and week 5 in the 2026 plan).
     */
    fun observeCurrentMeditation(): Flow<WeeklyMeditation?>

    /** All meditations (admin curation list / member archive). */
    fun observeAllMeditations(): Flow<List<WeeklyMeditation>>

    suspend fun getMeditationById(id: String): WeeklyMeditation?

    /**
     * The current user's submissions, newest first. Offline-first.
     * Empty list if the user hasn't reflected on any week yet.
     */
    fun observeMySubmissions(): Flow<List<MeditationSubmission>>

    /** Find current user's submission for a specific meditation, if any. */
    suspend fun findMySubmission(meditationId: String): MeditationSubmission?

    /**
     * Upsert the current user's reflection for [meditationId]. The DB UNIQUE
     * (user_id, meditation_id) ensures one row per user per week — Supabase
     * upsert handles both create-and-edit transparently.
     */
    suspend fun submitReflection(
        meditationId: String,
        reflectionText: String
    ): Result<Unit>

    /**
     * Submissions visible to the caller for a specific user (leader-view).
     * RLS enforces who can see what — the function just queries; the DB
     * returns rows only when the caller is the user themselves, their
     * cell leader (group_id match), or a senior leader.
     */
    suspend fun getSubmissionsForUser(userId: String): Result<List<MeditationSubmission>>
}
