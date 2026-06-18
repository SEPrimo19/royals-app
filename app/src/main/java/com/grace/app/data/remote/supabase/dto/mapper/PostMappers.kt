package com.grace.app.data.remote.supabase.dto.mapper

import com.grace.app.data.local.entity.PostEntity
import com.grace.app.data.remote.supabase.dto.PostDto
import com.grace.app.domain.model.Post
import com.grace.app.domain.model.PostType

private fun parseType(raw: String?): PostType = when (raw?.trim()?.lowercase()) {
    "photo" -> PostType.PHOTO
    "scripture" -> PostType.SCRIPTURE
    "prompt" -> PostType.PROMPT
    else -> PostType.TEXT
}

fun PostType.toDbValue(): String = name.lowercase()

fun PostDto.toDomain(
    reactions: Map<String, Int> = emptyMap(),
    myReaction: String? = null,
    commentCount: Int = 0
): Post = Post(
    id = id,
    userId = userId,
    userName = userName ?: "",
    userAvatarUrl = userAvatarUrl,
    type = parseType(type),
    content = content,
    imageUrl = imageUrl,
    verseRef = verseRef,
    isHighlighted = isHighlighted,
    reactions = reactions,
    myReaction = myReaction,
    commentCount = commentCount,
    createdAt = parseDateTime(createdAt)
)

fun PostDto.toEntity(): PostEntity = PostEntity(
    id = id,
    userId = userId,
    userName = userName,
    userAvatarUrl = userAvatarUrl,
    type = type,
    content = content,
    imageUrl = imageUrl,
    verseRef = verseRef,
    isHighlighted = isHighlighted,
    highlightedBy = highlightedBy,
    isFlagged = isFlagged,
    commentCount = 0,
    createdAt = createdAt
)

fun PostEntity.toDomain(
    reactions: Map<String, Int> = emptyMap(),
    myReaction: String? = null
): Post = Post(
    id = id,
    userId = userId,
    userName = userName ?: "",
    userAvatarUrl = userAvatarUrl,
    type = parseType(type),
    content = content,
    imageUrl = imageUrl,
    verseRef = verseRef,
    isHighlighted = isHighlighted,
    reactions = reactions,
    myReaction = myReaction,
    commentCount = commentCount,
    createdAt = parseDateTime(createdAt)
)
