package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.BibleQuestion
import com.grace.app.domain.model.QuestionCategory
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetPracticeQuestionsUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(
        category: QuestionCategory? = null,
        count: Int = 10
    ): Result<List<BibleQuestion>> =
        gamesRepository.getPracticeQuestions(category, count)
}
