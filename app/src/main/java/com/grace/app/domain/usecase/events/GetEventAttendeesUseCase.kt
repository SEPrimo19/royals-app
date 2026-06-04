package com.grace.app.domain.usecase.events

import com.grace.app.domain.model.Attendee
import com.grace.app.domain.repository.EventRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetEventAttendeesUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String): Result<List<Attendee>> =
        eventRepository.getAttendees(eventId)
}
