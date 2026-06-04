package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BiblePassageDto(
    @SerialName("id") val id: String,
    @SerialName("reference") val reference: String,
    @SerialName("text") val text: String,
    @SerialName("blank_word") val blankWord: String,
    @SerialName("distractors") val distractors: List<String>,
    @SerialName("language") val language: String = "nkjv",
    @SerialName("is_active") val isActive: Boolean = true
)
