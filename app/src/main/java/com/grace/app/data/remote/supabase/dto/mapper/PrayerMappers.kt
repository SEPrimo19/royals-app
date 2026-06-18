package com.grace.app.data.remote.supabase.dto.mapper

import com.grace.app.data.local.entity.PrayerEntity
import com.grace.app.data.remote.supabase.dto.PrayerDto
import com.grace.app.domain.model.Prayer
import com.grace.app.domain.model.PrayerCategory
import com.grace.app.domain.model.PrayerStatus

private fun parseCategory(raw: String?): PrayerCategory =
    when (raw?.trim()?.lowercase()) {
        "family" -> PrayerCategory.FAMILY
        "school" -> PrayerCategory.SCHOOL
        "faith" -> PrayerCategory.FAITH
        "health" -> PrayerCategory.HEALTH
        "nations" -> PrayerCategory.NATIONS
        else -> PrayerCategory.PERSONAL
    }

private fun parseStatus(raw: String?): PrayerStatus =
    when (raw?.trim()?.lowercase()) {
        "answered" -> PrayerStatus.ANSWERED
        "archived" -> PrayerStatus.ARCHIVED
        else -> PrayerStatus.ACTIVE
    }

fun PrayerCategory.toDbValue(): String = name.lowercase()
fun PrayerStatus.toDbValue(): String = name.lowercase()

fun PrayerDto.toDomain(): Prayer = Prayer(
    id = id,
    userId = if (isAnonymous) null else userId,
    userName = if (isAnonymous) null else userName,
    content = content,
    isAnonymous = isAnonymous,
    category = parseCategory(category),
    status = parseStatus(status),
    prayCount = prayCount,
    isFlagged = isFlagged,
    createdAt = parseDateTime(createdAt),
    postedByProxy = postedByProxy
)

fun PrayerDto.toEntity(): PrayerEntity = PrayerEntity(
    id = id,
    userId = if (isAnonymous) null else userId,
    userName = if (isAnonymous) null else userName,
    content = content,
    isAnonymous = isAnonymous,
    category = category,
    status = status,
    prayCount = prayCount,
    isFlagged = isFlagged,
    expiresAt = expiresAt,
    createdAt = createdAt,
    postedByProxy = postedByProxy
)

fun PrayerDto.toEntity(proxyLeaderName: String?): PrayerEntity =
    toEntity().copy(proxyLeaderName = proxyLeaderName)

fun PrayerEntity.toDomain(): Prayer = Prayer(
    id = id,
    userId = if (isAnonymous) null else userId,
    userName = if (isAnonymous) null else userName,
    content = content,
    isAnonymous = isAnonymous,
    category = parseCategory(category),
    status = parseStatus(status),
    prayCount = prayCount,
    isFlagged = isFlagged,
    createdAt = parseDateTime(createdAt),
    postedByProxy = postedByProxy,
    proxyLeaderName = proxyLeaderName
)
