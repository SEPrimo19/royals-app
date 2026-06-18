package com.grace.app.domain.repository

import com.grace.app.domain.model.Prayer
import com.grace.app.domain.model.PrayerCategory
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface PrayerRepository {
    fun getPrayers(category: PrayerCategory?): Flow<Result<List<Prayer>>>
    suspend fun postPrayer(
        content: String,
        isAnonymous: Boolean,
        category: PrayerCategory
    ): Result<Unit>
    suspend fun intercede(prayerId: String): Result<Unit>
    suspend fun markAnswered(prayerId: String): Result<Unit>
    fun subscribeToPrayCount(prayerId: String): Flow<Int>

    suspend fun getMyIntercessions(): Result<Set<String>>

    suspend fun getMyPrayers(): Result<List<Prayer>>

    suspend fun updatePrayerContent(prayerId: String, content: String): Result<Unit>

    suspend fun deletePrayer(prayerId: String): Result<Unit>
}
