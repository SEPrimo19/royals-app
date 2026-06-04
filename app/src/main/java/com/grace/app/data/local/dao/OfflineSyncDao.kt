package com.grace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grace.app.data.local.entity.OfflineSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineSyncDao {
    @Query("SELECT * FROM offline_sync_queue ORDER BY created_at ASC")
    fun getAll(): Flow<List<OfflineSyncEntity>>

    @Query("SELECT * FROM offline_sync_queue WHERE failed_at IS NULL AND retry_count < 3 ORDER BY created_at ASC")
    suspend fun getPending(): List<OfflineSyncEntity>

    @Query("SELECT * FROM offline_sync_queue WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): OfflineSyncEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OfflineSyncEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<OfflineSyncEntity>)

    @Query("UPDATE offline_sync_queue SET retry_count = :count WHERE id = :id")
    suspend fun setRetryCount(id: String, count: Int)

    @Query("UPDATE offline_sync_queue SET failed_at = :ts WHERE id = :id")
    suspend fun markFailed(id: String, ts: Long)

    @Query("DELETE FROM offline_sync_queue WHERE id = :id")
    suspend fun deleteById(id: String)

    @Delete
    suspend fun delete(entity: OfflineSyncEntity)
}
