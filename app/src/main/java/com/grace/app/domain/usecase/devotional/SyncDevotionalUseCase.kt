package com.grace.app.domain.usecase.devotional

import com.grace.app.domain.repository.DevotionalRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class SyncDevotionalUseCase @Inject constructor(
    private val devotionalRepository: DevotionalRepository
) {
    suspend operator fun invoke(): Result<Unit> =
        devotionalRepository.syncUpcomingDevotionals()
}
