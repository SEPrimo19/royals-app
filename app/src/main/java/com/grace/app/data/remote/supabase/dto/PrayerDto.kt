package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrayerDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("content") val content: String,
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,
    @SerialName("category") val category: String,
    @SerialName("status") val status: String = "active",
    @SerialName("pray_count") val prayCount: Int = 0,
    @SerialName("is_flagged") val isFlagged: Boolean = false,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("posted_by_proxy") val postedByProxy: String? = null
)
