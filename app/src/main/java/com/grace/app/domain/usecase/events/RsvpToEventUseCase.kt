package com.grace.app.domain.usecase.events

import com.grace.app.domain.model.RsvpStatus
import com.grace.app.domain.repository.EventRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class RsvpToEventUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String, status: RsvpStatus): Result<Unit> =
        eventRepository.rsvp(eventId, status)
}
