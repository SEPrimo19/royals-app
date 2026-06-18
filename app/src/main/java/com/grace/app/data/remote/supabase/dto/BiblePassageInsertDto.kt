package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BiblePassageInsertDto(
    @SerialName("reference") val reference: String,
    @SerialName("text") val text: String,
    @SerialName("blank_word") val blankWord: String,
    @SerialName("distractors") val distractors: List<String>
)
