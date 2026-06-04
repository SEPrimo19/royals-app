package com.grace.app.domain.usecase.events

import com.grace.app.domain.model.Event
import com.grace.app.domain.repository.EventRepository
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEventsUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    operator fun invoke(): Flow<Result<List<Event>>> = eventRepository.getEvents()
}
