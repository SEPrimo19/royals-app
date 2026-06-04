package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.BibleQuestion
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class SaveQuestionUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(question: BibleQuestion): Result<Unit> {
        if (question.question.trim().length < 5)
            return Result.Error("Question must be at least 5 characters.")
        if (question.options.size != 4)
            return Result.Error("A question must have exactly 4 options.")
        if (question.options.any { it.trim().isBlank() })
            return Result.Error("All 4 options must have text.")
        if (question.correctIndex !in 0..3)
            return Result.Error("Pick which option is correct.")
        return gamesRepository.saveQuestion(question)
    }
}
