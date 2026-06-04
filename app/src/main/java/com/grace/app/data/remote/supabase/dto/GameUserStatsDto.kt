package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GameUserStatsDto(
    @SerialName("user_id") val userId: String,
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
    @SerialName("total_points") val totalPoints: Long = 0L,
    @SerialName("last_played_at") val lastPlayedAt: String? = null,
    @SerialName("last_daily_easy_at") val lastEasyAt: String? = null,
    @SerialName("last_daily_medium_at") val lastMediumAt: String? = null,
    @SerialName("last_daily_hard_at") val lastHardAt: String? = null,
    @SerialName("last_daily_fitb_at") val lastFitbAt: String? = null
)
