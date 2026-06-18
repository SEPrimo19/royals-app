package com.grace.app.domain.model

data class Attendee(
    val user: User,
    val status: AttendanceStatus,
    val lateByMinutes: Int = 0
)
