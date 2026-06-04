package com.grace.app.domain.usecase.leader

import com.grace.app.domain.repository.LeaderRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class SubmitCheckInUseCase @Inject constructor(
    private val leaderRepository: LeaderRepository
) {
    suspend operator fun invoke(answers: Map<String, String>): Result<Unit> {
        if (answers.values.all { it.isBlank() }) {
            return Result.Error("Please answer at least one question.")
        }
        return leaderRepository.submitCheckIn(answers)
    }
}
