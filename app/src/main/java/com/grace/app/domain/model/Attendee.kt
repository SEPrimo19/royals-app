package com.grace.app.domain.model

/**
 * A single row on the creator's attendance roster — a user plus their
 * status for that event. The list also includes derived ABSENT entries
 * (RSVP=going minus actual check-ins) once the event has ended.
 */
data class Attendee(
    val user: User,
    val status: AttendanceStatus,
    val lateByMinutes: Int = 0
)
