package com.grace.app.domain.repository

import com.grace.app.domain.model.AttendedEvent
import com.grace.app.domain.model.Attendee
import com.grace.app.domain.model.CheckInResult
import com.grace.app.domain.model.Event
import com.grace.app.domain.model.RsvpStatus
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getEvents(): Flow<Result<List<Event>>>

    suspend fun rsvp(eventId: String, status: RsvpStatus): Result<Unit>

    suspend fun checkInToEvent(eventId: String): Result<CheckInResult>

    suspend fun getAttendees(eventId: String): Result<List<Attendee>>

    suspend fun getEvent(eventId: String): Result<Event?>

    suspend fun createEvent(
        title: String,
        description: String?,
        eventDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime?,
        location: String?,
        isRecurring: Boolean,
        requiresAttendance: Boolean
    ): Result<Event>

    suspend fun updateEvent(
        eventId: String,
        title: String,
        description: String?,
        eventDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime?,
        location: String?,
        isRecurring: Boolean,
        requiresAttendance: Boolean
    ): Result<Unit>

    suspend fun deleteEvent(eventId: String): Result<Unit>

    suspend fun getMyAttendance(): Result<List<AttendedEvent>>
}
