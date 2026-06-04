package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.BibleEvent
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

/**
 * Loads the candidate biblical event pool for a Timeline Sorting round.
 * The ViewModel builds 3 puzzles per round, each picking 5 random events
 * from this pool with non-adjacent chronological_order values.
 */
class GetBibleEventsUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(count: Int = 50): Result<List<BibleEvent>> =
        gamesRepository.getBibleEvents(count)
}
