package com.grace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "user_devo_progress", primaryKeys = ["user_id", "devo_id"])
data class UserDevoProgressEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "devo_id") val devoId: String,
    @ColumnInfo(name = "completed_at") val completedAt: String,
    @ColumnInfo(name = "journal_entry") val journalEntry: String?
)
