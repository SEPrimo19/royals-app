package com.grace.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

enum class MeditationTheme {
    JESUS, EDUCATION, FAMILY, FRIENDS, CHURCH, RELATIONSHIPS;

    val label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

data class WeeklyMeditation(
    val id: String,
    val weekNumber: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val theme: MeditationTheme,
    val title: String,
    val scriptureRef: String,
    val scriptureText: String,
    val reflectionPrompt: String,
    val furtherReadingLabel: String? = null,
    val furtherReadingUrl: String? = null,
    val isActive: Boolean = true
) {
    fun isCurrent(today: LocalDate = LocalDate.now()): Boolean =
        !today.isBefore(startDate) && !today.isAfter(endDate)
}

data class MeditationSubmission(
    val id: String,
    val userId: String,
    val meditationId: String,
    val reflectionText: String,
    val submittedAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val submittedByProxy: String? = null
)
