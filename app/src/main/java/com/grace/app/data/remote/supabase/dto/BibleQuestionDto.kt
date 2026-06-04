package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleQuestionDto(
    @SerialName("id") val id: String,
    @SerialName("category") val category: String,
    @SerialName("difficulty") val difficulty: String,
    @SerialName("question") val question: String,
    @SerialName("options") val options: List<String>,
    @SerialName("correct_index") val correctIndex: Int,
    @SerialName("explanation") val explanation: String? = null,
    @SerialName("source_ref") val sourceRef: String? = null,
    @SerialName("language") val language: String = "nkjv",
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class BibleQuestionInsertDto(
    @SerialName("category") val category: String,
    @SerialName("difficulty") val difficulty: String,
    @SerialName("question") val question: String,
    @SerialName("options") val options: List<String>,
    @SerialName("correct_index") val correctIndex: Int,
    @SerialName("explanation") val explanation: String? = null,
    @SerialName("source_ref") val sourceRef: String? = null
)
