package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.BibleVerseScramble
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

/**
 * Loads the candidate verse pool for a Verse Scramble round. The ViewModel
 * picks 5 from this pool. We constrain to ≤ 12-word verses by default so
 * the puzzle stays tractable on a phone.
 */
class GetVerseScramblesUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(
        count: Int = 25,
        maxWordCount: Int = 12
    ): Result<List<BibleVerseScramble>> =
        gamesRepository.getVerseScrambles(count, maxWordCount)
}
