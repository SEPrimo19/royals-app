package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape for `bible_characters`. */
@Serializable
data class BibleCharacterDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("category") val category: String,
    @SerialName("difficulty") val difficulty: String,
    @SerialName("clue_1") val clue1: String,
    @SerialName("clue_2") val clue2: String,
    @SerialName("clue_3") val clue3: String,
    @SerialName("clue_4") val clue4: String,
    @SerialName("distractors") val distractors: List<String>,
    @SerialName("source_ref") val sourceRef: String? = null,
    @SerialName("explanation") val explanation: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)
