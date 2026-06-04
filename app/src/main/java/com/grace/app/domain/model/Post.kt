package com.grace.app.domain.model

import java.time.LocalDateTime

enum class PostType { TEXT, PHOTO, SCRIPTURE, PROMPT }

data class Post(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val type: PostType,
    val content: String,
    val imageUrl: String?,
    val verseRef: String?,
    val isHighlighted: Boolean,
    val reactions: Map<String, Int>,
    val myReaction: String?,
    val commentCount: Int,
    val createdAt: LocalDateTime
)
