package com.grace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Offline Bible verse cache — keyed by verse reference (e.g. "John 3:16").
@Entity(tableName = "verses")
data class VerseEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "ref") val ref: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "cached_at") val cachedAt: Long = System.currentTimeMillis()
)
