package com.grace.app.data.remote.supabase.dto.mapper

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * Supabase returns `timestamptz` as ISO-8601 with an offset
 * (e.g. "2024-01-01T12:00:00.123456+00:00"). Parsing must never throw — a
 * malformed/absent value falls back to "now" so the UI never crashes on a row.
 *
 * Converts to the device's local timezone — `.toLocalDateTime()` on an
 * OffsetDateTime only strips the offset (keeps the UTC clock-time), so we
 * MUST shift via atZoneSameInstant first. Without this, a Philippine user
 * who picks 12:45 AM saves it as 16:45 UTC and sees "4:45 PM" on read-back.
 */
internal fun parseDateTime(raw: String?): LocalDateTime {
    if (raw.isNullOrBlank()) return LocalDateTime.now()
    return try {
        OffsetDateTime.parse(raw)
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(raw)
        } catch (_: DateTimeParseException) {
            LocalDateTime.now()
        }
    }
}

internal fun parseDate(raw: String?): LocalDate {
    if (raw.isNullOrBlank()) return LocalDate.now()
    return try {
        LocalDate.parse(raw)
    } catch (_: DateTimeParseException) {
        try {
            OffsetDateTime.parse(raw).toLocalDate()
        } catch (_: DateTimeParseException) {
            LocalDate.now()
        }
    }
}

internal fun parseDateOrNull(raw: String?): LocalDate? =
    if (raw.isNullOrBlank()) null else runCatching { parseDate(raw) }.getOrNull()
