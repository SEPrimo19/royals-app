package com.grace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "event_date") val eventDate: String,
    @ColumnInfo(name = "event_end_date") val eventEndDate: String?,
    @ColumnInfo(name = "location") val location: String?,
    @ColumnInfo(name = "created_by") val createdBy: String?,
    @ColumnInfo(name = "is_recurring") val isRecurring: Boolean = false,
    @ColumnInfo(name = "recur_rule") val recurRule: String?,
    @ColumnInfo(name = "requires_attendance") val requiresAttendance: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: String?
)
