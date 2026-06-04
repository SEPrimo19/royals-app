package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Read shape of a row from the `checkins` table. The leader-side
 * MemberDetail screen decodes these to show the member's most recent
 * weekly check-in answers.
 */
@Serializable
data class CheckinDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("leader_id") val leaderId: String? = null,
    @SerialName("answers") val answers: Map<String, String> = emptyMap(),
    @SerialName("submitted_at") val submittedAt: String? = null
)
