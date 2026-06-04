package com.grace.app.domain.usecase.games

import com.grace.app.domain.model.BiblePassage
import com.grace.app.domain.repository.GamesRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class SavePassageUseCase @Inject constructor(
    private val gamesRepository: GamesRepository
) {
    suspend operator fun invoke(passage: BiblePassage): Result<Unit> {
        if (passage.reference.trim().isBlank())
            return Result.Error("Reference is required (e.g. \"John 3:16\").")
        if (passage.text.trim().length < 10)
            return Result.Error("Verse text must be at least 10 characters.")
        if (passage.blankWord.trim().isBlank())
            return Result.Error("Pick a word to blank.")
        if (!passage.text.contains(passage.blankWord, ignoreCase = false) &&
            !passage.text.contains(passage.blankWord, ignoreCase = true)) {
            return Result.Error("The blank word must appear in the verse text.")
        }
        if (passage.distractors.size != 3 ||
            passage.distractors.any { it.trim().isBlank() }) {
            return Result.Error("Add exactly 3 distractor words.")
        }
        return gamesRepository.savePassage(passage)
    }
}
