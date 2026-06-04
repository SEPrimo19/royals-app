package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `status` and `lateByMinutes` are server-computed by the trigger — the
 * client never sets them on insert (defaults on the DTO satisfy the
 * serializer). On select they reflect the truth the database holds.
 */
@Serializable
data class EventAttendanceDto(
    @SerialName("event_id") val eventId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("attended_at") val attendedAt: String? = null,
    @SerialName("status") val status: String = "present",
    @SerialName("late_by_minutes") val lateByMinutes: Int = 0,
    // Phase P.2 — non-null means a leader recorded this row on behalf of
    // the member. The trigger skips the QR-window check when this is set
    // and trusts the leader's chosen status verbatim.
    @SerialName("posted_by_proxy") val postedByProxy: String? = null
)
