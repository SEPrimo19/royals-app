package com.grace.app.domain.model

data class LeaderboardEntry(
    val userId: String,
    val userName: String,
    val groupId: String?,
    val groupName: String? = null,
    val points: Int,
    val attempts: Int,
    val isMe: Boolean = false
)
