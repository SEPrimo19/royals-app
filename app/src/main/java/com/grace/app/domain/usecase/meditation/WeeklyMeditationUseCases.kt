package com.grace.app.domain.usecase.meditation

import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.WeeklyMeditation
import com.grace.app.domain.repository.WeeklyMeditationRepository
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentWeekMeditationUseCase @Inject constructor(
    private val repo: WeeklyMeditationRepository
) {
    operator fun invoke(): Flow<WeeklyMeditation?> = repo.observeCurrentMeditation()
}

class GetMyMeditationSubmissionsUseCase @Inject constructor(
    private val repo: WeeklyMeditationRepository
) {
    operator fun invoke(): Flow<List<MeditationSubmission>> = repo.observeMySubmissions()
}

class FindMyMeditationSubmissionUseCase @Inject constructor(
    private val repo: WeeklyMeditationRepository
) {
    suspend operator fun invoke(meditationId: String): MeditationSubmission? =
        repo.findMySubmission(meditationId)
}

class SubmitMeditationReflectionUseCase @Inject constructor(
    private val repo: WeeklyMeditationRepository
) {
    suspend operator fun invoke(meditationId: String, text: String): Result<Unit> =
        repo.submitReflection(meditationId, text)
}

class GetMeditationSubmissionsForUserUseCase @Inject constructor(
    private val repo: WeeklyMeditationRepository
) {
    suspend operator fun invoke(userId: String): Result<List<MeditationSubmission>> =
        repo.getSubmissionsForUser(userId)
}

class GetAllMeditationsUseCase @Inject constructor(
    private val repo: WeeklyMeditationRepository
) {
    operator fun invoke(): Flow<List<WeeklyMeditation>> = repo.observeAllMeditations()
}
