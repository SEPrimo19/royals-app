package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClientDevoProgressDto(
    @SerialName("user_id") val userId: String,
    @SerialName("client_devo_key") val clientDevoKey: String,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("journal_entry") val journalEntry: String? = null
)
