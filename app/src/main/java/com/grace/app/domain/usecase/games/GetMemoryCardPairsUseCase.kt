package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.MemoryCardPair
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

/**
 * Loads the candidate pool of pairs for a Memory Cards board. The
 * ViewModel picks 6 from this pool to build a 12-card 3×4 grid.
 * Re-fetches fresh shuffled randomness on every "Play Again".
 */
class GetMemoryCardPairsUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(count: Int = 30): Result<List<MemoryCardPair>> =
        gamesRepository.getMemoryCardPairs(count)
}
