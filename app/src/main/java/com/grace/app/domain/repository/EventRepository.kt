package com.grace.app.domain.repository

import com.grace.app.domain.model.AttendedEvent
import com.grace.app.domain.model.Attendee
import com.grace.app.domain.model.CheckInResult
import com.grace.app.domain.model.Event
import com.grace.app.domain.model.RsvpStatus
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface EventRepository {
    /** Upcoming events, soonest first, each enriched with the user's RSVP + going count. */
    fun getEvents(): Flow<Result<List<Event>>>

    /**
     * Set/clear the current user's RSVP for an event. Passing the status that
     * is already selected clears it (un-RSVP).
     */
    suspend fun rsvp(eventId: String, status: RsvpStatus): Result<Unit>

    /**
     * Record the current user's attendance for an event. Backend trigger
     * rejects calls outside the configured window, when attendance is off,
     * or for the wrong user. On success the trigger fills in status + late
     * minutes — returned here so the UI can show "Present" or "Late by N min".
     */
    suspend fun checkInToEvent(eventId: String): Result<CheckInResult>

    /**
     * Roster of attendees + (after the event ends) RSVP'd-going non-scanners
     * marked ABSENT. RLS limits this to the event creator + senior leaders;
     * members get an empty list (and that's correct).
     */
    suspend fun getAttendees(eventId: String): Result<List<Attendee>>

    /** Fetch a single event by id — needed by the check-in confirmation screen. */
    suspend fun getEvent(eventId: String): Result<Event?>

    /** Leader-only: create a new event. The caller becomes `created_by`. */
    suspend fun createEvent(
        title: String,
        description: String?,
        eventDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime?,
        location: String?,
        isRecurring: Boolean,
        requiresAttendance: Boolean
    ): Result<Event>

    /** Creator or senior-leader only: update an existing event's fields. */
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

    /** Creator or senior-leader only: delete an event (cascades RSVPs + attendance). */
    suspend fun deleteEvent(eventId: String): Result<Unit>

    /**
     * Personal attendance ledger — every event the current user has checked
     * in to, newest first. RLS guarantees no one else's rows leak in.
     */
    suspend fun getMyAttendance(): Result<List<AttendedEvent>>
}
