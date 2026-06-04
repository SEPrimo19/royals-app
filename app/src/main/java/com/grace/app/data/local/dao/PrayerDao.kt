package com.grace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grace.app.data.local.entity.PrayerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayers ORDER BY created_at DESC")
    fun getAll(): Flow<List<PrayerEntity>>

    @Query("SELECT * FROM prayers WHERE status != 'archived' ORDER BY created_at DESC")
    fun getAllActive(): Flow<List<PrayerEntity>>

    @Query("SELECT * FROM prayers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PrayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PrayerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PrayerEntity>)

    @Query("UPDATE prayers SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE prayers SET pray_count = :count WHERE id = :id")
    suspend fun updatePrayCount(id: String, count: Int)

    @Query("UPDATE prayers SET content = :content WHERE id = :id")
    suspend fun updateContent(id: String, content: String)

    @Query("DELETE FROM prayers WHERE id = :id")
    suspend fun deleteById(id: String)

    // Drops any prayer no longer present in the remote refresh. The earlier
    // status='active' filter let locally-answered ghosts (a stale optimistic
    // row the user marked Answered before the next refetch) survive forever;
    // reconciling across all statuses kills those too.
    @Query("DELETE FROM prayers WHERE id NOT IN (:keepIds)")
    suspend fun deleteNotIn(keepIds: List<String>)

    @Query("DELETE FROM prayers")
    suspend fun clearAll()

    @Delete
    suspend fun delete(entity: PrayerEntity)
}
