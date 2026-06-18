package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.BibleEvent
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetBibleEventsUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(count: Int = 50): Result<List<BibleEvent>> =
        gamesRepository.getBibleEvents(count)
}
