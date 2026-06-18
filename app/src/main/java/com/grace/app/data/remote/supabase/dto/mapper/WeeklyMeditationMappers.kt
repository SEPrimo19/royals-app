package com.grace.app.data.remote.supabase.dto.mapper

import com.grace.app.data.local.entity.MeditationSubmissionEntity
import com.grace.app.data.local.entity.WeeklyMeditationEntity
import com.grace.app.data.remote.supabase.dto.MeditationSubmissionDto
import com.grace.app.data.remote.supabase.dto.WeeklyMeditationDto
import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.MeditationTheme
import com.grace.app.domain.model.WeeklyMeditation

private fun parseTheme(raw: String?): MeditationTheme = when (raw?.trim()?.uppercase()) {
    "EDUCATION" -> MeditationTheme.EDUCATION
    "FAMILY" -> MeditationTheme.FAMILY
    "FRIENDS" -> MeditationTheme.FRIENDS
    "CHURCH" -> MeditationTheme.CHURCH
    "RELATIONSHIPS" -> MeditationTheme.RELATIONSHIPS
    else -> MeditationTheme.JESUS
}

fun WeeklyMeditationDto.toEntity(): WeeklyMeditationEntity = WeeklyMeditationEntity(
    id = id,
    weekNumber = weekNumber,
    startDate = startDate,
    endDate = endDate,
    theme = theme,
    title = title,
    scriptureRef = scriptureRef,
    scriptureText = scriptureText,
    reflectionPrompt = reflectionPrompt,
    furtherReadingLabel = furtherReadingLabel,
    furtherReadingUrl = furtherReadingUrl,
    isActive = isActive
)

fun WeeklyMeditationEntity.toDomain(): WeeklyMeditation = WeeklyMeditation(
    id = id,
    weekNumber = weekNumber,
    startDate = parseDate(startDate),
    endDate = parseDate(endDate),
    theme = parseTheme(theme),
    title = title,
    scriptureRef = scriptureRef,
    scriptureText = scriptureText,
    reflectionPrompt = reflectionPrompt,
    furtherReadingLabel = furtherReadingLabel,
    furtherReadingUrl = furtherReadingUrl,
    isActive = isActive
)

fun WeeklyMeditationDto.toDomain(): WeeklyMeditation = toEntity().toDomain()

fun MeditationSubmissionDto.toEntity(): MeditationSubmissionEntity =
    MeditationSubmissionEntity(
        id = id,
        userId = userId,
        meditationId = meditationId,
        reflectionText = reflectionText,
        submittedAt = submittedAt,
        updatedAt = updatedAt,
        submittedByProxy = submittedByProxy
    )

fun MeditationSubmissionEntity.toDomain(): MeditationSubmission =
    MeditationSubmission(
        id = id,
        userId = userId,
        meditationId = meditationId,
        reflectionText = reflectionText,
        submittedAt = parseDateTime(submittedAt),
        updatedAt = parseDateTime(updatedAt),
        submittedByProxy = submittedByProxy
    )

fun MeditationSubmissionDto.toDomain(): MeditationSubmission = toEntity().toDomain()
