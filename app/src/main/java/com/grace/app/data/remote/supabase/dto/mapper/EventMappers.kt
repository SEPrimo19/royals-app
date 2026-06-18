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
        "excused" -> AttendanceStatus.EXCUSED
        "absent" -> AttendanceStatus.ABSENT
        else -> AttendanceStatus.PRESENT
    }

fun AttendanceStatus.toDbValue(): String = when (this) {
    AttendanceStatus.PRESENT -> "present"
    AttendanceStatus.LATE -> "late"
    AttendanceStatus.EXCUSED -> "excused"
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

fun EventEntity.toDomain(
    myRsvp: RsvpStatus? = null,
    goingCount: Int = 0,
    iHaveAttended: Boolean = false
): Event = Event(
    id = id,
    title = title,
    description = description,
    eventDate = parseDateTime(eventDate),
    endDate = eventEndDate?.takeIf { it.isNotBlank() }?.let { parseDateTime(it) },
    location = location,
    isRecurring = isRecurring,
    requiresAttendance = requiresAttendance,
    createdBy = createdBy,
    myRsvp = myRsvp,
    goingCount = goingCount,
    iHaveAttended = iHaveAttended
)
