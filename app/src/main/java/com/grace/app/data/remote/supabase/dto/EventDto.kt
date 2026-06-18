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

@Serializable
data class EventRsvpDto(
    @SerialName("event_id") val eventId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("status") val status: String
)

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
