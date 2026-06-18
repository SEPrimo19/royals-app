package com.grace.app.data.remote.supabase.dto

import com.grace.app.data.remote.supabase.dto.mapper.parseDateTime
import com.grace.app.domain.model.UserNote
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserNoteUpsertDto(
    @SerialName("user_id")    val userId: String,
    @SerialName("content")    val content: String,
    @SerialName("is_hidden")  val isHidden: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("expires_at") val expiresAt: String
)

@Serializable
data class UserNoteHeartInsertDto(
    @SerialName("note_user_id") val noteUserId: String,
    @SerialName("hearter_id")   val hearterId: String
)

@Serializable
data class UserNoteRow(
    @SerialName("user_id")      val userId: String,
    @SerialName("user_name")    val userName: String,
    @SerialName("user_avatar")  val userAvatar: String? = null,
    @SerialName("content")      val content: String,
    @SerialName("is_hidden")    val isHidden: Boolean = false,
    @SerialName("created_at")   val createdAt: String,
    @SerialName("expires_at")   val expiresAt: String,
    @SerialName("heart_count")  val heartCount: Long = 0,
    @SerialName("has_my_heart") val hasMyHeart: Boolean = false
)

fun UserNoteRow.toDomain(): UserNote = UserNote(
    userId = userId,
    userName = userName,
    userAvatarUrl = userAvatar,
    content = content,
    isHidden = isHidden,
    createdAt = parseDateTime(createdAt),
    expiresAt = parseDateTime(expiresAt),
    heartCount = heartCount.toInt(),
    hasMyHeart = hasMyHeart
)
