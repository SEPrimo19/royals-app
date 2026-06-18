package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntryDto(
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("week_points") val weekPoints: Int,
    @SerialName("week_attempts") val weekAttempts: Int
)

@Serializable
data class MonthlyLeaderboardEntryDto(
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("month_points") val monthPoints: Int,
    @SerialName("month_attempts") val monthAttempts: Int
)

@Serializable
data class TeamLeaderboardEntryDto(
    @SerialName("group_id") val groupId: String,
    @SerialName("group_name") val groupName: String,
    @SerialName("member_count") val memberCount: Int,
    @SerialName("month_points") val monthPoints: Int
)
