package com.grace.app.data.remote.supabase.dto.mapper

import com.grace.app.data.local.entity.DevotionalEntity
import com.grace.app.data.remote.supabase.dto.DevotionalDto
import com.grace.app.domain.model.Devotional

fun DevotionalDto.toDomain(): Devotional = Devotional(
    id = id,
    scheduledDate = parseDate(scheduledDate),
    title = title,
    verseRef = verseRef,
    verseText = verseText,
    reflection = reflection,
    prayerStarter = prayerStarter,
    journalPrompt = journalPrompt,
    planId = planId
)

fun DevotionalDto.toEntity(): DevotionalEntity = DevotionalEntity(
    id = id,
    scheduledDate = scheduledDate,
    title = title,
    verseRef = verseRef,
    verseText = verseText,
    reflection = reflection,
    prayerStarter = prayerStarter,
    journalPrompt = journalPrompt,
    planId = planId,
    createdBy = createdBy,
    createdAt = createdAt
)

fun DevotionalEntity.toDomain(): Devotional = Devotional(
    id = id,
    scheduledDate = parseDate(scheduledDate),
    title = title,
    verseRef = verseRef,
    verseText = verseText,
    reflection = reflection,
    prayerStarter = prayerStarter,
    journalPrompt = journalPrompt,
    planId = planId
)
