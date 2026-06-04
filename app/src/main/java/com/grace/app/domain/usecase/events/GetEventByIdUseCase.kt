package com.grace.app.domain.usecase.events

import com.grace.app.domain.model.Event
import com.grace.app.domain.repository.EventRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetEventByIdUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(eventId: String): Result<Event?> =
        eventRepository.getEvent(eventId)
}
