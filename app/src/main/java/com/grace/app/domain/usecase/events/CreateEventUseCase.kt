package com.grace.app.domain.usecase.events

import com.grace.app.domain.model.Event
import com.grace.app.domain.repository.EventRepository
import com.grace.app.domain.util.Result
import java.time.LocalDateTime
import javax.inject.Inject

class CreateEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String?,
        eventDate: LocalDateTime,
        endDate: LocalDateTime?,
        location: String?,
        isRecurring: Boolean,
        requiresAttendance: Boolean
    ): Result<Event> {
        val cleanTitle = title.trim()
        if (cleanTitle.length < 3) {
            return Result.Error("Title must be at least 3 characters.")
        }
        if (eventDate.isBefore(LocalDateTime.now())) {
            return Result.Error("Event date must be in the future.")
        }
        if (endDate != null && !endDate.isAfter(eventDate)) {
            return Result.Error("End time must be after the start time.")
        }
        return eventRepository.createEvent(
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
