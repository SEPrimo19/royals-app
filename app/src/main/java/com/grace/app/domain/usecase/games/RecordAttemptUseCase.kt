package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.GameMode
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class RecordAttemptUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(
        mode: GameMode,
        questionId: String? = null,
        passageId: String? = null,
        characterId: String? = null,
        pairId: String? = null,
        scrambleId: String? = null,
        correct: Boolean,
        pointsEarned: Int,
        isDaily: Boolean
    ): Result<Unit> = gamesRepository.recordAttempt(
        mode, questionId, passageId, characterId, pairId, scrambleId,
        correct, pointsEarned, isDaily
    )
}
