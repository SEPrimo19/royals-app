package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.LeaderboardEntry
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetTeamLeaderboardUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(limit: Int = 25): Result<List<LeaderboardEntry>> =
        gamesRepository.getTeamLeaderboard(limit)
}
