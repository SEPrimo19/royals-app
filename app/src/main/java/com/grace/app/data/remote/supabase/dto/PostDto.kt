package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String? = null,
    @SerialName("user_avatar_url") val userAvatarUrl: String? = null,
    @SerialName("type") val type: String,
    @SerialName("content") val content: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("verse_ref") val verseRef: String? = null,
    @SerialName("is_highlighted") val isHighlighted: Boolean = false,
    @SerialName("highlighted_by") val highlightedBy: String? = null,
    @SerialName("is_flagged") val isFlagged: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)
