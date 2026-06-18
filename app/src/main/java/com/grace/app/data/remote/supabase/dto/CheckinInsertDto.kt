package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CheckinInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("leader_id") val leaderId: String? = null,
    @SerialName("answers") val answers: Map<String, String>
)
