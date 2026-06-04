package com.grace.app.domain.usecase.profile

import com.grace.app.domain.repository.PrayerRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class UpdatePrayerUseCase @Inject constructor(
    private val prayerRepository: PrayerRepository
) {
    suspend operator fun invoke(prayerId: String, content: String): Result<Unit> {
        val trimmed = content.trim()
        if (trimmed.length < 3) {
            return Result.Error("Prayer must be at least 3 characters.")
        }
        return prayerRepository.updatePrayerContent(prayerId, trimmed)
    }
}
