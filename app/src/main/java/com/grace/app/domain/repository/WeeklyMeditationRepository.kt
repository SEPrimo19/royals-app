package com.grace.app.domain.repository

import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.WeeklyMeditation
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface WeeklyMeditationRepository {

    fun observeCurrentMeditation(): Flow<WeeklyMeditation?>

    fun observeAllMeditations(): Flow<List<WeeklyMeditation>>

    suspend fun getMeditationById(id: String): WeeklyMeditation?

    fun observeMySubmissions(): Flow<List<MeditationSubmission>>

    suspend fun findMySubmission(meditationId: String): MeditationSubmission?

    suspend fun submitReflection(
        meditationId: String,
        reflectionText: String
    ): Result<Unit>

    suspend fun getSubmissionsForUser(userId: String): Result<List<MeditationSubmission>>
}
