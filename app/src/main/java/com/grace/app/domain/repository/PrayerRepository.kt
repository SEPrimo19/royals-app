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

    /**
     * Returns the set of prayer IDs the current user has already interceded
     * for. Used to render the "already prayed" state on PrayerCard so users
     * see immediately which prayers they've prayed for. Returns empty set on
     * any failure — degrades quietly to "you haven't prayed for any yet".
     */
    suspend fun getMyIntercessions(): Result<Set<String>>

    /**
     * All prayers authored by the current user — INCLUDING the ones they
     * posted anonymously (those don't surface their userId publicly but the
     * owner can still see + manage them). Used by My Content screen.
     */
    suspend fun getMyPrayers(): Result<List<Prayer>>

    /** Owner-only (RLS-enforced): update the prayer's content. */
    suspend fun updatePrayerContent(prayerId: String, content: String): Result<Unit>

    /** Owner-only (RLS-enforced): delete the prayer + cascades intercessions. */
    suspend fun deletePrayer(prayerId: String): Result<Unit>
}
