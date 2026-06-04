package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.GameDifficulty
import com.grace.app.domain.model.GameStats
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class CompleteDailyChallengeUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(difficulty: GameDifficulty): Result<GameStats> =
        gamesRepository.completeDailyChallenge(difficulty)
}
