package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Insert payload for the weekly check-in form. `answers` is a free-form
 * String → String map; the canonical question keys live in
 * `LeaderViewModel.checkInQuestions` so backend/frontend stay aligned.
 */
@Serializable
data class CheckinInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("leader_id") val leaderId: String? = null,
    // Stored as JSONB in Supabase; a String→String map serializes cleanly.
    @SerialName("answers") val answers: Map<String, String>
)
