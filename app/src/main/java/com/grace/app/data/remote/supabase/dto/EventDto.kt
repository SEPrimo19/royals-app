package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("event_date") val eventDate: String,
    @SerialName("event_end_date") val eventEndDate: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("is_recurring") val isRecurring: Boolean = false,
    @SerialName("recur_rule") val recurRule: String? = null,
    @SerialName("requires_attendance") val requiresAttendance: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null
)

// Insert/upsert payload for an RSVP. Composite PK (event_id, user_id).
@Serializable
data class EventRsvpDto(
    @SerialName("event_id") val eventId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("status") val status: String
)

/**
 * Insert payload — no `id` (Postgres generates it; sending "" causes the
 * "invalid input syntax for uuid" disguise-as-server-error bug we hit on
 * Life Groups). `created_by` is required so the creator-only QR + edit
 * gates can resolve ownership.
 *
 * `eventEndDate` and `requiresAttendance` are nullable/defaulted so the
 * server can fall back to legacy behavior (+2h, attendance on) if the
 * client doesn't send them.
 */
@Serializable
data class EventInsertDto(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("event_date") val eventDate: String,
    @SerialName("event_end_date") val eventEndDate: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("created_by") val createdBy: String,
    @SerialName("is_recurring") val isRecurring: Boolean = false,
    @SerialName("requires_attendance") val requiresAttendance: Boolean = true
)
