package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.BibleCharacter
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

/**
 * Loads the random character pool for a Who Am I? Practice session.
 * The ViewModel walks through them in order, reshuffling on exhaustion
 * so the user never repeats a character before the whole pool is seen.
 */
class GetWhoAmICharactersUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(count: Int = 100): Result<List<BibleCharacter>> =
        gamesRepository.getWhoAmICharacters(count)
}
