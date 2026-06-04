package com.grace.app.domain.usecase.prayer

import com.grace.app.domain.model.Prayer
import com.grace.app.domain.model.PrayerCategory
import com.grace.app.domain.repository.PrayerRepository
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPrayersUseCase @Inject constructor(
    private val prayerRepository: PrayerRepository
) {
    operator fun invoke(category: PrayerCategory?): Flow<Result<List<Prayer>>> =
        prayerRepository.getPrayers(category)
}
