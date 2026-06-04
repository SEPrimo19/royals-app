package com.grace.app.domain.model

/**
 * One row on a leaderboard. Used for BOTH the weekly cell-group board and
 * the monthly global board — the data shape is the same, only the source
 * RPC differs.
 *
 * `points` / `attempts` are the period totals (week for the weekly board,
 * month for the global board). `groupName` is populated only for global
 * rows so we can show a small cell-group chip next to each user.
 */
data class LeaderboardEntry(
    val userId: String,
    val userName: String,
    val groupId: String?,
    val groupName: String? = null,
    val points: Int,
    val attempts: Int,
    val isMe: Boolean = false
)
