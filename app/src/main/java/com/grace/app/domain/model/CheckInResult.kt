package com.grace.app.domain.model

/**
 * What the server recorded when the user checked in. `lateByMinutes` is 0
 * when status == PRESENT; otherwise it's the gap from event start to now,
 * as the trigger computed it.
 *
 * Status is never ABSENT here — absent is a derived concept that only
 * applies to non-scanners on a creator's roster.
 */
data class CheckInResult(
    val status: AttendanceStatus,
    val lateByMinutes: Int = 0
)
