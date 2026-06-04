package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape for `bible_verse_scrambles`. */
@Serializable
data class BibleVerseScrambleDto(
    @SerialName("id") val id: String,
    @SerialName("reference") val reference: String,
    @SerialName("text") val text: String,
    @SerialName("word_count") val wordCount: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true
)
