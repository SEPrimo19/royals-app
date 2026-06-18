package com.grace.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discipleship_activities")
data class DiscipleshipActivityEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val durationTag: String,
    val isActive: Boolean,
    val createdBy: String?,
    val createdAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "discipleship_today_pick")
data class DiscipleshipTodayPickEntity(
    @PrimaryKey val userId: String,
    val pickedDate: String,
    val activityId: String
)
