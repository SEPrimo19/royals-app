package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.MemoryCardPair
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetMemoryCardPairsUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(count: Int = 30): Result<List<MemoryCardPair>> =
        gamesRepository.getMemoryCardPairs(count)
}
