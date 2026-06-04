package com.grace.app.domain.usecase.progress

import com.grace.app.domain.model.AttendedEvent
import com.grace.app.domain.repository.EventRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetMyAttendanceUseCase @Inject constructor(
    private val eventRepository: EventRepository
) {
    suspend operator fun invoke(): Result<List<AttendedEvent>> =
        eventRepository.getMyAttendance()
}
