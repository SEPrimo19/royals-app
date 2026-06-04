package com.grace.app.domain.usecase.devotional

import com.grace.app.domain.model.JournalEntry
import com.grace.app.domain.repository.DevotionalRepository
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMyJournalUseCase @Inject constructor(
    private val devotionalRepository: DevotionalRepository
) {
    operator fun invoke(): Flow<Result<List<JournalEntry>>> =
        devotionalRepository.getMyJournal()
}
