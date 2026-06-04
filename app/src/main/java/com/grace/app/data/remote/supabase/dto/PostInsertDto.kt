package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// id / created_at are DB-defaulted, so omitted from the insert payload.
@Serializable
data class PostInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("type") val type: String,
    @SerialName("content") val content: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("verse_ref") val verseRef: String? = null
)
