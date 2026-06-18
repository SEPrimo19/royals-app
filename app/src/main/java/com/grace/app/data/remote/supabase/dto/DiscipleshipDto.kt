package com.grace.app.data.remote.supabase.dto

import com.grace.app.data.remote.supabase.dto.mapper.parseDateTime
import com.grace.app.domain.model.ActivityCategory
import com.grace.app.domain.model.DiscipleshipActivity
import com.grace.app.domain.model.DurationTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiscipleshipActivityDto(
    @SerialName("id")           val id: String,
    @SerialName("title")        val title: String,
    @SerialName("description")  val description: String,
    @SerialName("category")     val category: String,
    @SerialName("duration_tag") val durationTag: String = "15min",
    @SerialName("is_active")    val isActive: Boolean = true,
    @SerialName("created_by")   val createdBy: String? = null,
    @SerialName("created_at")   val createdAt: String,
    @SerialName("updated_at")   val updatedAt: String? = null
)

@Serializable
data class DiscipleshipActivityInsertDto(
    @SerialName("title")        val title: String,
    @SerialName("description")  val description: String,
    @SerialName("category")     val category: String,
    @SerialName("duration_tag") val durationTag: String,
    @SerialName("created_by")   val createdBy: String
)

@Serializable
data class TodaysActivityRow(
    @SerialName("id")           val id: String,
    @SerialName("title")        val title: String,
    @SerialName("description")  val description: String,
    @SerialName("category")     val category: String,
    @SerialName("duration_tag") val durationTag: String
)

@Serializable
data class CompletionInsertDto(
    @SerialName("user_id")     val userId: String,
    @SerialName("activity_id") val activityId: String,
    @SerialName("reflection")  val reflection: String? = null
)

fun DiscipleshipActivityDto.toDomain(): DiscipleshipActivity = DiscipleshipActivity(
    id = id,
    title = title,
    description = description,
    category = ActivityCategory.fromSlug(category),
    durationTag = DurationTag.fromSlug(durationTag),
    isActive = isActive,
    createdBy = createdBy,
    createdAt = parseDateTime(createdAt)
)

fun TodaysActivityRow.toDomain(): DiscipleshipActivity = DiscipleshipActivity(
    id = id,
    title = title,
    description = description,
    category = ActivityCategory.fromSlug(category),
    durationTag = DurationTag.fromSlug(durationTag),
    isActive = true,
    createdBy = null,
    createdAt = java.time.LocalDateTime.now()
)
