package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.LifelinesState
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

/** Loads the caller's remaining lifeline balance (auto-refilled nightly). */
class GetLifelinesUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(): Result<LifelinesState> = gamesRepository.getLifelines()
}
