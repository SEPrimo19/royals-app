package com.grace.app.domain.model

import java.time.LocalDateTime

data class AdminAttendanceRecord(
    val userId: String,
    val event: Event,
    val attendedAt: LocalDateTime,
    val status: AttendanceStatus,
    val lateByMinutes: Int
)
