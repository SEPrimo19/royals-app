package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.LifelineKind
import com.grace.app.domain.model.LifelinesState
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class UseLifelineUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(kind: LifelineKind): Result<LifelinesState> =
        gamesRepository.useLifeline(kind)
}
