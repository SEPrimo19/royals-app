package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DevotionalDto(
    @SerialName("id") val id: String,
    @SerialName("scheduled_date") val scheduledDate: String,
    @SerialName("title") val title: String,
    @SerialName("verse_ref") val verseRef: String,
    @SerialName("verse_text") val verseText: String,
    @SerialName("reflection") val reflection: String,
    @SerialName("prayer_starter") val prayerStarter: String,
    @SerialName("journal_prompt") val journalPrompt: String,
    @SerialName("plan_id") val planId: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
