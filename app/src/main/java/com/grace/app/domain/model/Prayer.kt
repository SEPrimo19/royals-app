package com.grace.app.domain.model

import java.time.LocalDateTime

enum class PrayerCategory { FAMILY, SCHOOL, FAITH, HEALTH, PERSONAL, NATIONS }

enum class PrayerStatus { ACTIVE, ANSWERED, ARCHIVED }

data class Prayer(
    val id: String,
    val userId: String?,
    val userName: String?,
    val content: String,
    val isAnonymous: Boolean,
    val category: PrayerCategory,
    val status: PrayerStatus,
    val prayCount: Int,
    val isFlagged: Boolean,
    val createdAt: LocalDateTime,
    val postedByProxy: String? = null,
    val proxyLeaderName: String? = null
)
