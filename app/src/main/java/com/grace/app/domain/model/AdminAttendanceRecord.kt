package com.grace.app.domain.model

import java.time.LocalDateTime

/**
 * One attendance record as seen by the admin compliance report.
 *
 * Unlike [AttendedEvent] (which is the "what did *I* attend" shape for
 * single-user screens), this carries the [userId] explicitly so the
 * compliance roster can group by attendee.
 *
 * Late counts as a form of present — both are recorded rows in
 * `event_attendance`. Absent is derived (RSVP'd but never checked in)
 * and would require a separate cross-reference; not modeled here.
 */
data class AdminAttendanceRecord(
    val userId: String,
    val event: Event,
    val attendedAt: LocalDateTime,
    val status: AttendanceStatus,
    val lateByMinutes: Int
)
