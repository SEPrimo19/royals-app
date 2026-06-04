package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDevoProgressDto(
    @SerialName("user_id") val userId: String,
    @SerialName("devo_id") val devoId: String,
    // Stored as AES-256-GCM ciphertext (base64). Never plaintext.
    @SerialName("journal_entry") val journalEntry: String? = null,
    // Server-defaulted (NOW()) on insert. Read back when restoring
    // progress to Room after a fresh install.
    @SerialName("completed_at") val completedAt: String? = null
)
