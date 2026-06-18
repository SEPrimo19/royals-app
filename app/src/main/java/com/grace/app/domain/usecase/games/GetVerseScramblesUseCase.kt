package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.BibleVerseScramble
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetVerseScramblesUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(
        count: Int = 25,
        maxWordCount: Int = 12
    ): Result<List<BibleVerseScramble>> =
        gamesRepository.getVerseScrambles(count, maxWordCount)
}
