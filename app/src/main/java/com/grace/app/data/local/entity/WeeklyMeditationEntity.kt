package com.grace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weekly_meditations")
data class WeeklyMeditationEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "week_number") val weekNumber: Int,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "end_date") val endDate: String,
    @ColumnInfo(name = "theme") val theme: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "scripture_ref") val scriptureRef: String,
    @ColumnInfo(name = "scripture_text") val scriptureText: String,
    @ColumnInfo(name = "reflection_prompt") val reflectionPrompt: String,
    @ColumnInfo(name = "further_reading_label") val furtherReadingLabel: String?,
    @ColumnInfo(name = "further_reading_url") val furtherReadingUrl: String?,
    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Boolean = true
)

@Entity(tableName = "meditation_submissions")
data class MeditationSubmissionEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "meditation_id") val meditationId: String,
    @ColumnInfo(name = "reflection_text") val reflectionText: String,
    @ColumnInfo(name = "submitted_at") val submittedAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "submitted_by_proxy") val submittedByProxy: String? = null
)
