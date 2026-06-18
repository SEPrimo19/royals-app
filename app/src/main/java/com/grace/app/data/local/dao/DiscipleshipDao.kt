package com.grace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grace.app.data.local.entity.DiscipleshipActivityEntity
import com.grace.app.data.local.entity.DiscipleshipTodayPickEntity

@Dao
interface DiscipleshipDao {


    @Query("SELECT * FROM discipleship_activities WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun listActiveActivities(): List<DiscipleshipActivityEntity>

    @Query("SELECT * FROM discipleship_activities WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DiscipleshipActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<DiscipleshipActivityEntity>)

    @Query("DELETE FROM discipleship_activities WHERE id NOT IN (:keepIds)")
    suspend fun deleteNotIn(keepIds: List<String>)

    @Query("DELETE FROM discipleship_activities")
    suspend fun clearAll()


    @Query("SELECT * FROM discipleship_today_pick WHERE userId = :uid LIMIT 1")
    suspend fun getTodaysPick(uid: String): DiscipleshipTodayPickEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTodaysPick(pick: DiscipleshipTodayPickEntity)

    @Query("DELETE FROM discipleship_today_pick WHERE userId = :uid")
    suspend fun clearTodaysPick(uid: String)
}
