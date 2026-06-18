package com.grace.app.domain.model

import java.time.LocalDateTime

data class UserNote(
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val content: String,
    val isHidden: Boolean,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime,
    val heartCount: Int,
    val hasMyHeart: Boolean
)
