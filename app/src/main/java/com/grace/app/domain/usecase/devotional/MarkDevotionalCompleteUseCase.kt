package com.grace.app.domain.usecase.devotional

import com.grace.app.domain.repository.DevotionalRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class MarkDevotionalCompleteUseCase @Inject constructor(
    private val devotionalRepository: DevotionalRepository
) {
    suspend operator fun invoke(devoId: String, journalEntry: String): Result<Unit> {
        if (journalEntry.isBlank()) {
            return Result.Error("Write a short reflection before completing.")
        }
        return devotionalRepository.markComplete(devoId, journalEntry.trim())
    }
}
