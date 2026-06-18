package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameAttemptInsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("mode") val mode: String,
    @SerialName("question_id") val questionId: String? = null,
    @SerialName("passage_id") val passageId: String? = null,
    @SerialName("character_id") val characterId: String? = null,
    @SerialName("pair_id") val pairId: String? = null,
    @SerialName("scramble_id") val scrambleId: String? = null,
    @SerialName("correct") val correct: Boolean,
    @SerialName("points_earned") val pointsEarned: Int,
    @SerialName("is_daily") val isDaily: Boolean
)
