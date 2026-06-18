package com.grace.app.domain.model

import java.time.LocalDateTime

data class AttendedEvent(
    val event: Event,
    val attendedAt: LocalDateTime,
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val lateByMinutes: Int = 0
)
