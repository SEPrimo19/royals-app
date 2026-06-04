package com.grace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grace.app.data.local.entity.DevotionalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DevotionalDao {
    @Query("SELECT * FROM devotionals ORDER BY scheduled_date DESC")
    fun getAll(): Flow<List<DevotionalEntity>>

    @Query("SELECT * FROM devotionals WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DevotionalEntity?

    @Query("SELECT * FROM devotionals WHERE scheduled_date = :date LIMIT 1")
    fun getByDate(date: String): Flow<DevotionalEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: DevotionalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DevotionalEntity>)

    @Query("DELETE FROM devotionals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Delete
    suspend fun delete(entity: DevotionalEntity)
}
