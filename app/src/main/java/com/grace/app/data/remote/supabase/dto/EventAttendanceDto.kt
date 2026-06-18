package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventAttendanceDto(
    @SerialName("event_id") val eventId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("attended_at") val attendedAt: String? = null,
    @SerialName("status") val status: String = "present",
    @SerialName("late_by_minutes") val lateByMinutes: Int = 0,
    @SerialName("posted_by_proxy") val postedByProxy: String? = null
)
