package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Row shape for `user_client_devo_progress`. Tracks completion of the
 * client-generated daily devotionals (id format "daily-YYYY-MM-DD") that
 * can't be written to the main `user_devo_progress` table because of its
 * UUID FK to `devotionals(id)`.
 */
@Serializable
data class ClientDevoProgressDto(
    @SerialName("user_id") val userId: String,
    @SerialName("client_devo_key") val clientDevoKey: String,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("journal_entry") val journalEntry: String? = null
)
