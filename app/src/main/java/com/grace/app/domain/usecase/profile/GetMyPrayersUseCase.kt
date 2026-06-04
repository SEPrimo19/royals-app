package com.grace.app.domain.usecase.profile

import com.grace.app.domain.model.Prayer
import com.grace.app.domain.repository.PrayerRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetMyPrayersUseCase @Inject constructor(
    private val prayerRepository: PrayerRepository
) {
    suspend operator fun invoke(): Result<List<Prayer>> =
        prayerRepository.getMyPrayers()
}
