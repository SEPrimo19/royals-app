package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Insert payload for a new prayer. id / created_at / expires_at / pray_count
 * are DB-defaulted, so they are intentionally omitted. user_id is ALWAYS sent
 * (even for anonymous posts) for pastoral safeguarding; RLS hides it from peers.
 */
@Serializable
data class PrayerInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("content") val content: String,
    @SerialName("is_anonymous") val isAnonymous: Boolean,
    @SerialName("category") val category: String
)
