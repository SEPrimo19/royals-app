package com.grace.app.domain.model

/**
 * Status of a single attendance record.
 *
 * - PRESENT — scanned (or proxy-marked) on time.
 * - LATE    — scanned (or proxy-marked) after start. `lateByMinutes` carries
 *             how late they were (only meaningful for QR scans — leader
 *             proxy entries don't track a late minute count).
 * - EXCUSED — leader-only state. Member couldn't attend for a legitimate
 *             reason (sick, traveling). Counts as "accounted for" in
 *             Compassion compliance, distinct from ABSENT.
 * - ABSENT  — derived (never stored). For the creator's roster: any user
 *             who RSVP'd "going" but has no event_attendance row after the
 *             event has ended.
 */
enum class AttendanceStatus { PRESENT, LATE, EXCUSED, ABSENT }
