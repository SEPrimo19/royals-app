package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.BiblePassage
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetAllPassagesUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(): Result<List<BiblePassage>> =
        gamesRepository.listAllPassages()
}
