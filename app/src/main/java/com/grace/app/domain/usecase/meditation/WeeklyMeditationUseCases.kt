package com.grace.app.domain.usecase.meditation

import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.WeeklyMeditation
import com.grace.app.domain.repository.WeeklyMeditationRepository
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Drives the "This Week's Meditation" tab inside the Devotional screen. */
class GetCurrentWeekMeditationUseCase @Inject constructor(
    private val repo: WeeklyMeditationRepository
) {
    operator fun invoke(): Flow<WeeklyMeditation?> = repo.observeCurrentMeditation()
}

/** Drives the "My Reflections" list (in the user's profile / progress). */
class GetMyMeditationSubmissionsUseCase @Inject constructor(
    private val repo: WeeklyMeditationRepository
) {
    operator fun invoke(): Flow<List<MeditationSubmission>> = repo.observeMySubmissions()
}

/** Find the current user's own submission for a specific meditation. */
class FindMyMeditationSubmissionUseCase @Inject constructor(
    private val repo: WeeklyMeditationRepository
) {
    suspend operator fun invoke(meditationId: String): MeditationSubmission? =
        repo.findMySubmission(meditationId)
}

/** Upsert the current user's reflection for a specific meditation. */
class SubmitMeditationReflectionUseCase @Inject constructor(
    private val repo: WeeklyMeditationRepository
) {
    suspend operator fun invoke(meditationId: String, text: String): Result<Unit> =
        repo.submitReflection(meditationId, text)
}

/**
 * Leader-view: fetch a specific member's submissions. RLS in Supabase
 * gates who actually receives rows — non-allowed callers get an empty list,
 * not an error, by design.
 */
class GetMeditationSubmissionsForUserUseCase @Inject constructor(
    private val repo: WeeklyMeditationRepository
) {
    suspend operator fun invoke(userId: String): Result<List<MeditationSubmission>> =
        repo.getSubmissionsForUser(userId)
}

/** Admin curation list — every meditation, newest first. */
class GetAllMeditationsUseCase @Inject constructor(
    private val repo: WeeklyMeditationRepository
) {
    operator fun invoke(): Flow<List<WeeklyMeditation>> = repo.observeAllMeditations()
}
