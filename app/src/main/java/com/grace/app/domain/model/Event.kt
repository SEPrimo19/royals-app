package com.grace.app.domain.model

import java.time.LocalDateTime

enum class RsvpStatus { GOING, MAYBE, NOT_GOING }

data class Event(
    val id: String,
    val title: String,
    val description: String?,
    val eventDate: LocalDateTime,
    // Optional explicit end time. When null, the legacy "+2h after start"
    // window applies — both UI and the SQL trigger fall back to that.
    val endDate: LocalDateTime?,
    val location: String?,
    val isRecurring: Boolean,
    // When false, this event is an info-only reminder (Sunday Service,
    // bulletin item). No QR, no check-in, no present/absent tracking.
    val requiresAttendance: Boolean,
    // User ID of the event creator — drives "is this *my* event?" UI gating
    // for the QR-attendance feature (only creators see the QR).
    val createdBy: String?,
    // The current user's RSVP, or null if they haven't responded.
    val myRsvp: RsvpStatus?,
    val goingCount: Int,
    // Has the current user already checked in to this event?
    val iHaveAttended: Boolean = false
)
