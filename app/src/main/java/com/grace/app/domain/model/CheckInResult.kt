package com.grace.app.domain.model

data class CheckInResult(
    val status: AttendanceStatus,
    val lateByMinutes: Int = 0
)
