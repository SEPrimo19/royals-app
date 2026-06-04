package com.grace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grace.app.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    // Newest event first (audit-item #8). Was ASC; match the Supabase
    // query so the offline cache renders in the same order as a fresh fetch.
    @Query("SELECT * FROM events ORDER BY event_date DESC")
    fun getAll(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<EventEntity>)

    // Reconcile against the server (drop events deleted remotely).
    @Query("DELETE FROM events WHERE id NOT IN (:keepIds)")
    suspend fun deleteNotIn(keepIds: List<String>)

    @Query("DELETE FROM events")
    suspend fun clearAll()
}
