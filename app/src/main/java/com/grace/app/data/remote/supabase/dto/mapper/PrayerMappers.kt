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

/**
 * Anonymous prayer safeguarding: user identity never surfaces to the UI layer
 * for anonymous posts, regardless of what the DTO contains. The DB still stores
 * user_id (pastoral safeguarding) and RLS hides it from non-leaders, but the
 * client also strips it here as defence in depth.
 */
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
    // postedByProxy is a leader's user_id when the leader posted on
    // behalf of the member. proxyLeaderName stays null here — the
    // repository fills it via a single batched users lookup so we don't
    // round-trip per prayer.
    postedByProxy = postedByProxy
)

fun PrayerDto.toEntity(): PrayerEntity = PrayerEntity(
    id = id,
    // Same safeguard at the cache layer — anonymous identity is never persisted
    // anywhere the UI can read it back.
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
    // proxyLeaderName is filled by the repository at insert time, not here —
    // PrayerDto doesn't carry the leader name from Supabase. See the
    // batched lookup in PrayerRepositoryImpl.getPrayers.
)

/**
 * Variant of [toEntity] used when the repository has already resolved the
 * proxy leader's display name via a batched users lookup. Passes the
 * name through to the local cache so the "via {leader}" tag survives
 * offline.
 */
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
