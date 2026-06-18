package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDevoProgressDto(
    @SerialName("user_id") val userId: String,
    @SerialName("devo_id") val devoId: String,
    @SerialName("journal_entry") val journalEntry: String? = null,
    @SerialName("completed_at") val completedAt: String? = null
)
