package com.grace.app.data.remote.supabase.dto.mapper

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

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
