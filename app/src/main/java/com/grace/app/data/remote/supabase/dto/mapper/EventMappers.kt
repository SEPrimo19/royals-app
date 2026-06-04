package com.grace.app.data.remote.supabase.dto.mapper

import com.grace.app.data.local.entity.EventEntity
import com.grace.app.data.remote.supabase.dto.EventDto
import com.grace.app.domain.model.AttendanceStatus
import com.grace.app.domain.model.Event
import com.grace.app.domain.model.RsvpStatus

fun parseRsvpStatus(raw: String?): RsvpStatus = when (raw?.trim()?.lowercase()) {
    "maybe" -> RsvpStatus.MAYBE
    "not_going" -> RsvpStatus.NOT_GOING
    else -> RsvpStatus.GOING
}

fun RsvpStatus.toDbValue(): String = name.lowercase()

fun parseAttendanceStatus(raw: String?): AttendanceStatus =
    when (raw?.trim()?.lowercase()) {
        "late" -> AttendanceStatus.LATE
        // "excused" was added by feature-leader-proxy-attendance.sql — only
        // settable by a leader proxy insert, never by a member QR scan.
        "excused" -> AttendanceStatus.EXCUSED
        // "absent" is derived on read; the column itself only ever stores
        // 'present' / 'late' / 'excused', but we accept the value defensively.
        "absent" -> AttendanceStatus.ABSENT
        else -> AttendanceStatus.PRESENT
    }

fun AttendanceStatus.toDbValue(): String = when (this) {
    AttendanceStatus.PRESENT -> "present"
    AttendanceStatus.LATE -> "late"
    AttendanceStatus.EXCUSED -> "excused"
    // ABSENT is derived and never written back to the DB. Insert callers
    // shouldn't hand us this value, but fall through safely if they do.
    AttendanceStatus.ABSENT -> "present"
}

fun EventDto.toEntity(): EventEntity = EventEntity(
    id = id,
    title = title,
    description = description,
    eventDate = eventDate,
    eventEndDate = eventEndDate,
    location = location,
    createdBy = createdBy,
    isRecurring = isRecurring,
    recurRule = recurRule,
    requiresAttendance = requiresAttendance,
    createdAt = createdAt
)

// myRsvp / goingCount / iHaveAttended come from aggregating event_rsvp and
// event_attendance — see EventRepositoryImpl.
fun EventEntity.toDomain(
    myRsvp: RsvpStatus? = null,
    goingCount: Int = 0,
    iHaveAttended: Boolean = false
): Event = Event(
    id = id,
    title = title,
    description = description,
    eventDate = parseDateTime(eventDate),
    // Optional end. parseDateTime returns now() on null, so route through
    // a takeIf to preserve true null semantics ("no explicit end set").
    endDate = eventEndDate?.takeIf { it.isNotBlank() }?.let { parseDateTime(it) },
    location = location,
    isRecurring = isRecurring,
    requiresAttendance = requiresAttendance,
    createdBy = createdBy,
    myRsvp = myRsvp,
    goingCount = goingCount,
    iHaveAttended = iHaveAttended
)
