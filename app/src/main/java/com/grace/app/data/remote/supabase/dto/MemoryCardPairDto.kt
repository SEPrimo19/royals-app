package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemoryCardPairDto(
    @SerialName("id") val id: String,
    @SerialName("reference") val reference: String,
    @SerialName("verse_snippet") val verseSnippet: String,
    @SerialName("full_text") val fullText: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)
