package com.grace.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

enum class ActivityCategory(val slug: String, val label: String, val emoji: String) {
    BIBLE_STUDY("bible_study", "Bible Study", "📖"),
    PRAYER("prayer", "Prayer", "🙏"),
    WITNESS("witness", "Witness", "📣"),
    SERVICE("service", "Service", "🤝"),
    WORSHIP("worship", "Worship", "🎶"),
    CHARACTER("character", "Character", "🌱");

    companion object {
        fun fromSlug(s: String?): ActivityCategory =
            entries.firstOrNull { it.slug == s?.trim()?.lowercase() } ?: CHARACTER
    }
}

enum class DurationTag(val slug: String, val label: String) {
    FIVE("5min", "≤ 5 min"),
    FIFTEEN("15min", "~ 15 min"),
    THIRTY_PLUS("30min_plus", "30+ min");

    companion object {
        fun fromSlug(s: String?): DurationTag =
            entries.firstOrNull { it.slug == s?.trim()?.lowercase() } ?: FIFTEEN
    }
}

data class DiscipleshipActivity(
    val id: String,
    val title: String,
    val description: String,
    val category: ActivityCategory,
    val durationTag: DurationTag,
    val isActive: Boolean,
    val createdBy: String?,
    val createdAt: LocalDateTime
)

data class DiscipleshipCompletion(
    val activityId: String,
    val completedDate: LocalDate,
    val reflection: String?
)
