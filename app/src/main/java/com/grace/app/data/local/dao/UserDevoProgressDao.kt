package com.grace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grace.app.data.local.entity.UserDevoProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDevoProgressDao {
    @Query("SELECT * FROM user_devo_progress WHERE user_id = :userId")
    fun getAllForUser(userId: String): Flow<List<UserDevoProgressEntity>>

    @Query(
        "SELECT * FROM user_devo_progress WHERE user_id = :userId AND devo_id = :devoId LIMIT 1"
    )
    suspend fun getProgress(userId: String, devoId: String): UserDevoProgressEntity?

    @Query(
        "SELECT * FROM user_devo_progress WHERE user_id = :userId AND devo_id = :devoId LIMIT 1"
    )
    fun observeProgress(userId: String, devoId: String): Flow<UserDevoProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserDevoProgressEntity)

    @Query("DELETE FROM user_devo_progress")
    suspend fun clear()
}
