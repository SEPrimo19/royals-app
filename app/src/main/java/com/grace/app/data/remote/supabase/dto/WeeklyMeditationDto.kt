package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeeklyMeditationDto(
    @SerialName("id") val id: String,
    @SerialName("week_number") val weekNumber: Int,
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String,
    @SerialName("theme") val theme: String,
    @SerialName("title") val title: String,
    @SerialName("scripture_ref") val scriptureRef: String,
    @SerialName("scripture_text") val scriptureText: String,
    @SerialName("reflection_prompt") val reflectionPrompt: String,
    @SerialName("further_reading_label") val furtherReadingLabel: String? = null,
    @SerialName("further_reading_url") val furtherReadingUrl: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class MeditationSubmissionDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("meditation_id") val meditationId: String,
    @SerialName("reflection_text") val reflectionText: String,
    @SerialName("submitted_at") val submittedAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("submitted_by_proxy") val submittedByProxy: String? = null
)

@Serializable
data class MeditationSubmissionInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("meditation_id") val meditationId: String,
    @SerialName("reflection_text") val reflectionText: String,
    @SerialName("submitted_by_proxy") val submittedByProxy: String? = null
)
