package com.grace.app.domain.model

import java.time.LocalDateTime

enum class RsvpStatus { GOING, MAYBE, NOT_GOING }

data class Event(
    val id: String,
    val title: String,
    val description: String?,
    val eventDate: LocalDateTime,
    val endDate: LocalDateTime?,
    val location: String?,
    val isRecurring: Boolean,
    val requiresAttendance: Boolean,
    val createdBy: String?,
    val myRsvp: RsvpStatus?,
    val goingCount: Int,
    val iHaveAttended: Boolean = false
)
