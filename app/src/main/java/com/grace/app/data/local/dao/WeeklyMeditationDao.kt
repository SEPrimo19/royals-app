package com.grace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grace.app.data.local.entity.MeditationSubmissionEntity
import com.grace.app.data.local.entity.WeeklyMeditationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyMeditationDao {

    /**
     * The meditation whose [start_date, end_date] range contains [today].
     * Returns null between weeks (e.g. the gap between week 4 and week 5
     * has Jun 29 - Jul 5 with no scheduled meditation in the 2026 plan).
     */
    @Query("""
        SELECT * FROM weekly_meditations
        WHERE is_active = 1
          AND date(start_date) <= date(:today)
          AND date(end_date)   >= date(:today)
        LIMIT 1
    """)
    fun observeCurrent(today: String): Flow<WeeklyMeditationEntity?>

    @Query("SELECT * FROM weekly_meditations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WeeklyMeditationEntity?

    @Query("SELECT * FROM weekly_meditations ORDER BY start_date DESC")
    fun observeAll(): Flow<List<WeeklyMeditationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meditation: WeeklyMeditationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(meditations: List<WeeklyMeditationEntity>)
}

@Dao
interface MeditationSubmissionDao {

    /** Current user's submissions — newest first, drives My Reflections list. */
    @Query("""
        SELECT * FROM meditation_submissions
        WHERE user_id = :userId
        ORDER BY submitted_at DESC
    """)
    fun observeMine(userId: String): Flow<List<MeditationSubmissionEntity>>

    @Query("""
        SELECT * FROM meditation_submissions
        WHERE user_id = :userId AND meditation_id = :meditationId
        LIMIT 1
    """)
    suspend fun findMy(userId: String, meditationId: String):
        MeditationSubmissionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(submission: MeditationSubmissionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(submissions: List<MeditationSubmissionEntity>)

    @Query("DELETE FROM meditation_submissions WHERE user_id = :userId")
    suspend fun clearForUser(userId: String)
}
