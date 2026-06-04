package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Row shape from the `get_weekly_group_leaderboard` RPC. Maps to the
 * shared [com.grace.app.domain.model.LeaderboardEntry] via the period
 * totals stored as `week_points` / `week_attempts`.
 */
@Serializable
data class LeaderboardEntryDto(
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("week_points") val weekPoints: Int,
    @SerialName("week_attempts") val weekAttempts: Int
)

/**
 * Row shape from the `get_monthly_global_leaderboard` RPC. Differs from
 * the weekly DTO in two ways: column names use `month_` instead of
 * `week_`, and `group_name` is joined in so the UI can render a small
 * cell-group chip next to each rank.
 */
@Serializable
data class MonthlyLeaderboardEntryDto(
    @SerialName("user_id") val userId: String,
    @SerialName("user_name") val userName: String,
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("group_name") val groupName: String? = null,
    @SerialName("month_points") val monthPoints: Int,
    @SerialName("month_attempts") val monthAttempts: Int
)
