package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.BiblePassage
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetRandomFillInBlankUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(): Result<BiblePassage?> =
        gamesRepository.getRandomFillInBlank()
}
