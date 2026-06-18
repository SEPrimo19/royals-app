package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.BibleCharacter
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetWhoAmICharactersUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(count: Int = 100): Result<List<BibleCharacter>> =
        gamesRepository.getWhoAmICharacters(count)
}
