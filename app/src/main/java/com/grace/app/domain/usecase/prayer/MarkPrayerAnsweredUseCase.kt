package com.grace.app.domain.usecase.prayer

import com.grace.app.domain.repository.PrayerRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class MarkPrayerAnsweredUseCase @Inject constructor(
    private val prayerRepository: PrayerRepository
) {
    suspend operator fun invoke(prayerId: String): Result<Unit> =
        prayerRepository.markAnswered(prayerId)
}
