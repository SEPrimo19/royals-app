package com.grace.app.domain.usecase.prayer

import com.grace.app.domain.model.PrayerCategory
import com.grace.app.domain.repository.PrayerRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class PostPrayerUseCase @Inject constructor(
    private val prayerRepository: PrayerRepository
) {
    suspend operator fun invoke(
        content: String,
        isAnonymous: Boolean,
        category: PrayerCategory
    ): Result<Unit> {
        if (content.isBlank()) {
            return Result.Error("Please write your prayer request first.")
        }
        return prayerRepository.postPrayer(content.trim(), isAnonymous, category)
    }
}
