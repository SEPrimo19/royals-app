package com.grace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of admin-curated meditation content. 30 rows per year — tiny
 * table. Cached so the current week's meditation displays instantly even
 * before Supabase responds (or while offline).
 */
@Entity(tableName = "weekly_meditations")
data class WeeklyMeditationEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "week_number") val weekNumber: Int,
    @ColumnInfo(name = "start_date") val startDate: String,   // ISO date
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

/**
 * Local cache of the CURRENT user's own submissions. Other-user submissions
 * (the leader-visibility surface) are NOT cached — they're fetched on demand
 * from the MemberDetail screen since they change frequently and leaders may
 * want fresh data.
 */
@Entity(tableName = "meditation_submissions")
data class MeditationSubmissionEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "meditation_id") val meditationId: String,
    @ColumnInfo(name = "reflection_text") val reflectionText: String,
    @ColumnInfo(name = "submitted_at") val submittedAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    // Phase P.3 — null when the member self-submitted; the leader's user_id
    // when logged via proxy. Cached locally so leader-only surfaces don't
    // need an extra fetch to show the audit attribution.
    @ColumnInfo(name = "submitted_by_proxy") val submittedByProxy: String? = null
)
