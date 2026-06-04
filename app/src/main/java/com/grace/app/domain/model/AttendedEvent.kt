package com.grace.app.domain.model

import java.time.LocalDateTime

/**
 * A row from the user's personal attendance ledger — the event the user
 * checked in to, plus the moment the check-in landed and the server-computed
 * status (PRESENT or LATE; ABSENT never shows up here — that's a creator-
 * roster concept).
 *
 * Kept as a thin pairing (instead of just reusing Event) so the timestamp
 * shown on My Attendance is always the *check-in time*, not the event start
 * — those can differ (members tap "I'm here" when they arrive late, etc.).
 */
data class AttendedEvent(
    val event: Event,
    val attendedAt: LocalDateTime,
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val lateByMinutes: Int = 0
)
