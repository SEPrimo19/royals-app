package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrayerInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("content") val content: String,
    @SerialName("is_anonymous") val isAnonymous: Boolean,
    @SerialName("category") val category: String
)
