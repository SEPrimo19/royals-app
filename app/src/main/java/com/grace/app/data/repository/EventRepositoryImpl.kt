package com.grace.app.data.repository

import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.local.dao.EventDao
import com.grace.app.data.remote.supabase.dto.EventAttendanceDto
import com.grace.app.data.remote.supabase.dto.EventDto
import com.grace.app.data.remote.supabase.dto.EventInsertDto
import com.grace.app.data.remote.supabase.dto.EventRsvpDto
import com.grace.app.data.remote.supabase.dto.UserDto
import com.grace.app.data.remote.supabase.dto.mapper.parseAttendanceStatus
import com.grace.app.data.remote.supabase.dto.mapper.parseRsvpStatus
import com.grace.app.data.remote.supabase.dto.mapper.toDbValue
import com.grace.app.data.remote.supabase.dto.mapper.toDomain
import com.grace.app.data.remote.supabase.dto.mapper.toEntity
import com.grace.app.data.util.CrashReporter
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.model.AttendanceStatus
import com.grace.app.domain.model.AttendedEvent
import com.grace.app.domain.model.Attendee
import com.grace.app.domain.model.CheckInResult
import com.grace.app.domain.model.Event
import com.grace.app.domain.model.RsvpStatus
import com.grace.app.domain.repository.EventRepository
import com.grace.app.domain.util.Result
import com.grace.app.worker.EventReminderScheduler
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val eventDao: EventDao,
    private val supabase: SupabaseClient,
    private val networkMonitor: NetworkMonitor,
    private val prefs: UserPreferencesRepo
) : EventRepository {

    override fun getEvents(): Flow<Result<List<Event>>> = flow {
        CrashReporter.log("getEvents: start; online=${networkMonitor.isOnline}")
        // Cached emit first — fast paint; RSVP data unknown (count 0) offline.
        val cached = eventDao.getAll().first().map { it.toDomain() }
        CrashReporter.log("getEvents: cached=${cached.size}")
        if (cached.isNotEmpty()) emit(Result.Success(cached))

        if (!networkMonitor.isOnline) {
            CrashReporter.log("getEvents: offline — bailing")
            if (cached.isEmpty()) emit(Result.Success(emptyList()))
            return@flow
        }

        // Diagnostic: confirm the Supabase client thinks we're signed in.
        // If session is null here, the SELECT below will hit RLS as 'anon'
        // and silently return [] — that's the classic "fresh install,
        // events exist in DB, empty screen" symptom.
        val sessionPresent = supabase.auth.currentSessionOrNull() != null
        val sessionUid = supabase.auth.currentUserOrNull()?.id
        val prefsUid = prefs.userId.first()
        CrashReporter.log(
            "getEvents: session=$sessionPresent " +
                "sessionUid=${sessionUid ?: "null"} prefsUid=${prefsUid ?: "null"}"
        )

        // -- Phase 1: pull events from Supabase and persist them. If this
        //    throws, the user has nothing — log + surface error. --
        val remote = try {
            supabase.from("events")
                // Newest event first (audit-item #8). Was ASC ("upcoming
                // first"); user wants the most recently-scheduled event at
                // the top of the list regardless of past/future.
                .select { order("event_date", Order.DESCENDING) }
                .decodeList<EventDto>()
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            CrashReporter.log("getEvents primary fetch failed")
            CrashReporter.recordNonFatal(e)
            if (cached.isEmpty()) {
                emit(Result.Error("Couldn't load events. Check your connection."))
            }
            return@flow
        }
        CrashReporter.log("getEvents: supabase returned ${remote.size} events")

        // If Supabase returned zero events but we're signed in, that's
        // either an RLS denial (session/JWT problem) or an actually-empty
        // table. Record a non-fatal so we can tell which one when it
        // happens in the field.
        if (remote.isEmpty() && sessionPresent) {
            CrashReporter.recordNonFatal(
                IllegalStateException(
                    "getEvents: signed-in fetch returned 0 events " +
                        "(RLS denial or table empty)"
                )
            )
        }

        if (remote.isEmpty()) eventDao.clearAll()
        else eventDao.deleteNotIn(remote.map { it.id })
        eventDao.insertAll(remote.map { it.toEntity() })

        // -- Phase 2: emit basic events immediately. If enrichment fails
        //    below, the user still sees the list. This is the fix for
        //    "events in Supabase but missing on first install" — previously
        //    a throw in the rsvp/attendance query skipped this emit. --
        val basic = remote.map { it.toEntity().toDomain() }
        CrashReporter.log("getEvents: emitting basic list with ${basic.size}")
        emit(Result.Success(basic))

        // Re-align local heads-up reminders to whatever the server says is
        // upcoming. Idempotent (REPLACE per-event), so calling this on every
        // fetch is safe. Past events are skipped inside the scheduler.
        runCatching { EventReminderScheduler.scheduleAll(appContext, basic) }
            .onFailure { CrashReporter.recordNonFatal(it) }

        // -- Phase 3: best-effort enrichment. Each query is independently
        //    wrapped so a partial failure doesn't lose the rest. --
        val eventIds = remote.map { it.id }
        val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()

        val rsvps = if (eventIds.isEmpty()) emptyList()
        else runCatching {
            supabase.from("event_rsvp")
                .select { filter { isIn("event_id", eventIds) } }
                .decodeList<EventRsvpDto>()
        }.onFailure {
            CrashReporter.log("getEvents rsvp enrichment failed")
            CrashReporter.recordNonFatal(it)
        }.getOrDefault(emptyList())

        val myAttended = if (uid == null) emptySet()
        else runCatching {
            supabase.from("event_attendance")
                .select { filter { eq("user_id", uid) } }
                .decodeList<EventAttendanceDto>()
                .map { it.eventId }
                .toSet()
        }.onFailure {
            CrashReporter.log("getEvents attendance enrichment failed")
            CrashReporter.recordNonFatal(it)
        }.getOrDefault(emptySet())

        val byEvent = rsvps.groupBy { it.eventId }
        val enriched = remote.map { dto ->
            val rs = byEvent[dto.id].orEmpty()
            val goingCount = rs.count { parseRsvpStatus(it.status) == RsvpStatus.GOING }
            val mine = rs.firstOrNull { it.userId == uid }
                ?.let { parseRsvpStatus(it.status) }
            dto.toEntity().toDomain(
                myRsvp = mine,
                goingCount = goingCount,
                iHaveAttended = dto.id in myAttended
            )
        }
        // Only re-emit if enrichment actually added something — saves a
        // recomposition for the common "no RSVPs / not attended" case.
        if (enriched != basic) emit(Result.Success(enriched))
    }.flowOn(Dispatchers.IO)

    override suspend fun rsvp(eventId: String, status: RsvpStatus): Result<Unit> {
        return try {
            val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()
            ?: return Result.Error("Your session expired. Please sign in again.")
            if (!networkMonitor.isOnline) {
                return Result.Error("You're offline. Connect to RSVP.")
            }

            val existing = supabase.from("event_rsvp")
                .select {
                    filter {
                        eq("event_id", eventId)
                        eq("user_id", uid)
                    }
                }
                .decodeList<EventRsvpDto>()
                .firstOrNull()

            when {
                // Tapping the already-selected status clears the RSVP.
                existing != null && existing.status == status.toDbValue() ->
                    supabase.from("event_rsvp").delete {
                        filter {
                            eq("event_id", eventId)
                            eq("user_id", uid)
                        }
                    }

                existing != null ->
                    supabase.from("event_rsvp")
                        .update({ set("status", status.toDbValue()) }) {
                            filter {
                                eq("event_id", eventId)
                                eq("user_id", uid)
                            }
                        }

                else ->
                    supabase.from("event_rsvp")
                        .insert(EventRsvpDto(eventId, uid, status.toDbValue()))
            }
            Result.Success(Unit)
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            val msg = when {
                e.message?.contains("row-level security", ignoreCase = true) == true ->
                    "RSVP isn't enabled yet. Please contact your church admin."
                else -> "Couldn't save your RSVP. Try again."
            }
            Result.Error(msg, e)
        }
    }

    override suspend fun checkInToEvent(eventId: String): Result<CheckInResult> {
        val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()
        ?: return Result.Error("Your session expired. Please sign in again.")
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to check in.")
        }
        return try {
            // Insert with select() so we read back the trigger-computed
            // status + late_by_minutes. The trigger sets them on NEW before
            // the row lands, so the returned row already has them.
            val saved = withContext(Dispatchers.IO) {
                supabase.from("event_attendance")
                    .insert(EventAttendanceDto(eventId = eventId, userId = uid)) {
                        select()
                    }
                    .decodeSingle<EventAttendanceDto>()
            }
            Result.Success(
                CheckInResult(
                    status = parseAttendanceStatus(saved.status),
                    lateByMinutes = saved.lateByMinutes
                )
            )
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            CrashReporter.log("checkInToEvent failed for $eventId")
            CrashReporter.recordNonFatal(e)
            // Map the trigger's RAISE EXCEPTION messages to friendlier copy
            // — they bubble up through Postgrest with the exact text in the
            // body. Keeping the user-visible string focused.
            val raw = e.message.orEmpty()
            val msg = when {
                raw.contains("opens 1 hour before", ignoreCase = true) ->
                    "Check-in isn't open yet — comes online 1 hour before the event."
                raw.contains("is closed for this event", ignoreCase = true) ->
                    "Check-in is closed for this event."
                raw.contains("Attendance is off", ignoreCase = true) ->
                    "This event doesn't track attendance."
                raw.contains("duplicate key", ignoreCase = true) ->
                    "You're already checked in. See you there!"
                raw.contains("row-level security", ignoreCase = true) ->
                    "You don't have permission to check in here."
                raw.contains("Event not found", ignoreCase = true) ->
                    "That event link is no longer valid."
                else -> "Couldn't check you in. Try again."
            }
            Result.Error(msg, e)
        }
    }

    override suspend fun getAttendees(eventId: String): Result<List<Attendee>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to view attendees.")
        }
        // IO-pin the whole pipeline so JSON decoding + filtering never lands
        // on the main thread. Wrap in a 10s timeout — a stuck socket should
        // surface as a friendly error, not ANR the QR screen.
        return runCatching {
            withContext(Dispatchers.IO) {
                withTimeout(ATTENDEES_TIMEOUT_MS) {
                    coroutineScope {
                        // Fan out the two independent first-hop queries
                        // (attendees + event lookup for absent-derivation)
                        // so total latency = max(a,b), not a+b.
                        val attendeesDeferred = async {
                            supabase.from("event_attendance")
                                .select { filter { eq("event_id", eventId) } }
                                .decodeList<EventAttendanceDto>()
                        }
                        val eventDeferred = async {
                            supabase.from("events")
                                .select { filter { eq("id", eventId) } }
                                .decodeSingleOrNull<EventDto>()
                        }
                        val rows = attendeesDeferred.await()
                        val event = eventDeferred.await()

                        val attendeeIds = rows.map { it.userId }.toSet()
                        val endsAt = event?.eventEndDate
                            ?.takeIf { it.isNotBlank() }
                            ?.let {
                                runCatching {
                                    java.time.OffsetDateTime.parse(it)
                                }.getOrNull()
                            }
                            ?: event?.eventDate?.let {
                                runCatching {
                                    java.time.OffsetDateTime.parse(it)
                                        .plusHours(2)
                                }.getOrNull()
                            }
                        val eventEnded = endsAt
                            ?.isBefore(java.time.OffsetDateTime.now()) == true
                        val shouldDeriveAbsent = eventEnded &&
                            event?.requiresAttendance != false

                        // Second fan-out: users-for-present and (optionally)
                        // RSVP-going-for-absent. Still parallel.
                        val presentUsersDeferred = async {
                            if (attendeeIds.isEmpty()) emptyMap()
                            else supabase.from("users")
                                .select {
                                    filter {
                                        isIn("id", attendeeIds.toList())
                                    }
                                }
                                .decodeList<UserDto>()
                                .map { it.toDomain() }
                                .associateBy { it.id }
                        }
                        val absentIdsDeferred = async {
                            if (!shouldDeriveAbsent) emptyList<String>()
                            else supabase.from("event_rsvp")
                                .select {
                                    filter {
                                        eq("event_id", eventId)
                                        eq("status", "going")
                                    }
                                }
                                .decodeList<EventRsvpDto>()
                                .map { it.userId }
                                .filter { it !in attendeeIds }
                        }
                        val users = presentUsersDeferred.await()
                        val absentIds = absentIdsDeferred.await()

                        val present = rows.mapNotNull { row ->
                            val user = users[row.userId] ?: return@mapNotNull null
                            Attendee(
                                user = user,
                                status = parseAttendanceStatus(row.status),
                                lateByMinutes = row.lateByMinutes
                            )
                        }
                        val absent = if (absentIds.isEmpty()) emptyList()
                        else supabase.from("users")
                            .select { filter { isIn("id", absentIds) } }
                            .decodeList<UserDto>()
                            .map { it.toDomain() }
                            .map {
                                Attendee(
                                    user = it,
                                    status = AttendanceStatus.ABSENT
                                )
                            }
                        // Present/late first (sorted by lateness),
                        // absent at the bottom.
                        present.sortedBy { it.lateByMinutes } + absent
                    }
                }
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            // Catch Throwable — covers TimeoutCancellationException,
            // OutOfMemoryError, NPEs from bad data, etc. The only thing
            // we deliberately let through is coroutine cancellation
            // (caller cancelled us — that's not a failure).
            onFailure = { t ->
                if (t is kotlinx.coroutines.CancellationException &&
                    t !is kotlinx.coroutines.TimeoutCancellationException) throw t
                CrashReporter.log("getAttendees failed for $eventId")
                CrashReporter.recordNonFatal(t)
                Result.Error(
                    if (t is kotlinx.coroutines.TimeoutCancellationException)
                        "Loading the roster took too long. Try again."
                    else "Couldn't load the attendee list."
                )
            }
        )
    }

    private companion object {
        const val ATTENDEES_TIMEOUT_MS = 10_000L
    }

    override suspend fun createEvent(
        title: String,
        description: String?,
        eventDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime?,
        location: String?,
        isRecurring: Boolean,
        requiresAttendance: Boolean
    ): Result<Event> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to create events.")
        }
        return try {
            val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()
            ?: return Result.Error("Your session expired. Please sign in again.")
            // ISO-8601 with timezone offset — Postgres timestamptz parses it.
            val isoStart = eventDate.atZone(java.time.ZoneId.systemDefault())
                .toOffsetDateTime().toString()
            val isoEnd = endDate?.atZone(java.time.ZoneId.systemDefault())
                ?.toOffsetDateTime()?.toString()
            val created = supabase.from("events")
                .insert(
                    EventInsertDto(
                        title = title,
                        description = description,
                        eventDate = isoStart,
                        eventEndDate = isoEnd,
                        location = location,
                        createdBy = uid,
                        isRecurring = isRecurring,
                        requiresAttendance = requiresAttendance
                    )
                ) { select() }
                .decodeSingle<EventDto>()
            // Mirror to Room so the list updates without waiting for refresh.
            eventDao.insertAll(listOf(created.toEntity()))
            val domain = created.toEntity().toDomain()
            // Schedule the one-shot heads-up for this brand-new event so the
            // creator doesn't have to wait for the next getEvents() pass.
            runCatching { EventReminderScheduler.scheduleAll(appContext, listOf(domain)) }
            Result.Success(domain)
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.Error(crudFriendly(e), e)
        }
    }

    override suspend fun updateEvent(
        eventId: String,
        title: String,
        description: String?,
        eventDate: java.time.LocalDateTime,
        endDate: java.time.LocalDateTime?,
        location: String?,
        isRecurring: Boolean,
        requiresAttendance: Boolean
    ): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to update events.")
        }
        return try {
            val isoStart = eventDate.atZone(java.time.ZoneId.systemDefault())
                .toOffsetDateTime().toString()
            val isoEnd = endDate?.atZone(java.time.ZoneId.systemDefault())
                ?.toOffsetDateTime()?.toString()
            supabase.from("events").update({
                set("title", title)
                set("description", description)
                set("event_date", isoStart)
                set("event_end_date", isoEnd)
                set("location", location)
                set("is_recurring", isRecurring)
                set("requires_attendance", requiresAttendance)
            }) {
                filter { eq("id", eventId) }
            }
            Result.Success(Unit)
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.Error(crudFriendly(e), e)
        }
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to delete events.")
        }
        return try {
            supabase.from("events").delete { filter { eq("id", eventId) } }
            // Drop the pending reminder — keeps a deleted event from
            // pinging the user an hour before the now-non-existent start.
            runCatching { EventReminderScheduler.cancel(appContext, eventId) }
            // Remove from Room so the card disappears immediately on next refresh.
            eventDao.deleteNotIn(eventDao.getAll().first()
                .map { it.id }.filter { it != eventId })
            Result.Success(Unit)
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.Error(crudFriendly(e), e)
        }
    }

    /** Maps backend errors specific to event mutations to user-safe copy. */
    private fun crudFriendly(e: Throwable): String = when {
        e.message?.contains("row-level security", ignoreCase = true) == true ->
            "You don't have permission. Leaders only."
        else -> "Couldn't save your changes. Try again."
    }

    override suspend fun getMyAttendance(): Result<List<AttendedEvent>> {
        val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()
        ?: return Result.Error("Your session expired. Please sign in again.")
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to see your attendance.")
        }
        // IO-pin + 10s timeout so a slow upstream can't ANR the screen.
        return runCatching {
            withContext(Dispatchers.IO) {
                withTimeout(ATTENDEES_TIMEOUT_MS) {
                    // RLS already filters to my rows; the explicit user_id
                    // filter is belt-and-braces in case policies are ever
                    // relaxed.
                    val rows = supabase.from("event_attendance")
                        .select { filter { eq("user_id", uid) } }
                        .decodeList<EventAttendanceDto>()
                    if (rows.isEmpty()) return@withTimeout emptyList()

                    val eventIds = rows.map { it.eventId }
                    val events = supabase.from("events")
                        .select { filter { isIn("id", eventIds) } }
                        .decodeList<EventDto>()
                        .associateBy { it.id }

                    rows.mapNotNull { row ->
                        val dto = events[row.eventId] ?: return@mapNotNull null
                        val event = dto.toEntity().toDomain(
                            myRsvp = null,
                            goingCount = 0,
                            iHaveAttended = true
                        )
                        val attendedAt = row.attendedAt
                            ?.let {
                                runCatching {
                                    // Shift to local TZ first — see
                                    // parseDateTime() comment for the
                                    // OffsetDateTime.toLocalDateTime gotcha.
                                    java.time.OffsetDateTime.parse(it)
                                        .atZoneSameInstant(
                                            java.time.ZoneId.systemDefault()
                                        )
                                        .toLocalDateTime()
                                }.getOrNull()
                            } ?: event.eventDate
                        AttendedEvent(
                            event = event,
                            attendedAt = attendedAt,
                            status = parseAttendanceStatus(row.status),
                            lateByMinutes = row.lateByMinutes
                        )
                    }.sortedByDescending { it.attendedAt }
                }
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { t ->
                if (t is kotlinx.coroutines.CancellationException &&
                    t !is kotlinx.coroutines.TimeoutCancellationException) throw t
                CrashReporter.log("getMyAttendance failed")
                CrashReporter.recordNonFatal(t)
                Result.Error(
                    if (t is kotlinx.coroutines.TimeoutCancellationException)
                        "Loading your attendance took too long. Try again."
                    else "Couldn't load your attendance."
                )
            }
        )
    }

    override suspend fun getEvent(eventId: String): Result<Event?> {
        return try {
            if (!networkMonitor.isOnline) {
                // Fall back to Room cache if offline — better than nothing.
                val cached = eventDao.getAll().first()
                    .firstOrNull { it.id == eventId }
                    ?.toDomain()
                return Result.Success(cached)
            }
            val dto = supabase.from("events")
                .select { filter { eq("id", eventId) } }
                .decodeSingleOrNull<EventDto>()
                ?: return Result.Success(null)
            Result.Success(dto.toEntity().toDomain())
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Result.Error("Couldn't load the event.")
        }
    }
}
