package com.grace.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.grace.app.data.local.entity.VerseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VerseDao {
    @Query("SELECT * FROM verses")
    fun getAll(): Flow<List<VerseEntity>>

    @Query("SELECT * FROM verses WHERE ref = :ref LIMIT 1")
    suspend fun getByRef(ref: String): VerseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: VerseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<VerseEntity>)

    @Delete
    suspend fun delete(entity: VerseEntity)
}
