package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.LeaderboardEntry
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

/**
 * Monthly leaderboard across the entire church. Includes BOTH Daily
 * Challenge and Practice points. Resets on the 1st of each calendar
 * month automatically (server-side `date_trunc('month', NOW())`).
 */
class GetMonthlyGlobalLeaderboardUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(limit: Int = 25): Result<List<LeaderboardEntry>> =
        gamesRepository.getMonthlyGlobalLeaderboard(limit)
}
