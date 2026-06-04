package com.grace.app.domain.usecase.games

import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class SetPassageActiveUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(id: String, isActive: Boolean): Result<Unit> =
        gamesRepository.setPassageActive(id, isActive)
}
