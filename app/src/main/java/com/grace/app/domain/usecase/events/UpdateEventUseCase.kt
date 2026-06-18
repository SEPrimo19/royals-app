package com.grace.app.domain.usecase.events

import com.grace.app.domain.repository.EventRepository
import com.grace.app.domain.util.Result
import java.time.LocalDateTime
import javax.inject.Inject

class UpdateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(
        eventId: String,
        title: String,
        description: String?,
        eventDate: LocalDateTime,
        endDate: LocalDateTime?,
        location: String?,
        isRecurring: Boolean,
        requiresAttendance: Boolean
    ): Result<Unit> {
        val cleanTitle = title.trim()
        if (cleanTitle.length < 3) {
            return Result.Error("Title must be at least 3 characters.")
        }
        if (endDate != null && !endDate.isAfter(eventDate)) {
            return Result.Error("End time must be after the start time.")
        }
        return eventRepository.updateEvent(
            eventId = eventId,
            title = cleanTitle,
            description = description?.trim()?.takeIf { it.isNotBlank() },
            eventDate = eventDate,
            endDate = endDate,
            location = location?.trim()?.takeIf { it.isNotBlank() },
            isRecurring = isRecurring,
            requiresAttendance = requiresAttendance
        )
    }
}
