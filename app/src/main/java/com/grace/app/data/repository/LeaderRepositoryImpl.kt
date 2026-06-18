package com.grace.app.data.repository

import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.remote.supabase.dto.CheckinDto
import com.grace.app.data.remote.supabase.dto.CheckinInsertDto
import com.grace.app.data.remote.supabase.dto.GroupDto
import com.grace.app.data.remote.supabase.dto.UserDto
import com.grace.app.data.remote.supabase.dto.mapper.parseAttendanceStatus
import com.grace.app.data.remote.supabase.dto.mapper.toDomain
import com.grace.app.data.remote.supabase.dto.mapper.toEntity
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.model.CheckIn
import com.grace.app.domain.model.Mentee
import com.grace.app.domain.model.User
import com.grace.app.domain.repository.LeaderRepository
import com.grace.app.domain.util.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val networkMonitor: NetworkMonitor,
    private val prefs: UserPreferencesRepo
) : LeaderRepository {

    private val leaderRoles = listOf("cell_leader", "youth_president", "pastor", "admin")

    private suspend fun currentUid(): String? =
        supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()

    override fun getMyLeader(): Flow<Result<User?>> = flow {
        val groupId = prefs.groupId.first()
        if (groupId.isNullOrBlank()) {
            emit(Result.Success(null)); return@flow
        }
        if (!networkMonitor.isOnline) {
            emit(Result.Error("You're offline. Connect to see your leader."))
            return@flow
        }
        try {
            val group = supabase.from("groups")
                .select { filter { eq("id", groupId) } }
                .decodeSingleOrNull<GroupDto>()
            val leaderId = group?.leaderId
            if (leaderId.isNullOrBlank()) {
                emit(Result.Success(null))
            } else {
                val leader = supabase.from("users")
                    .select { filter { eq("id", leaderId) } }
                    .decodeSingleOrNull<UserDto>()
                emit(Result.Success(leader?.toDomain()))
            }
        } catch (e: Exception) {
            emit(Result.Error("Couldn't load your leader.", e))
        }
    }.flowOn(Dispatchers.IO)

    override fun getAllLeaders(): Flow<Result<List<User>>> = flow {
        if (!networkMonitor.isOnline) {
            emit(Result.Error("You're offline. Connect to see leaders."))
            return@flow
        }
        try {
            val leaders = supabase.from("users")
                .select { filter { isIn("role", leaderRoles) } }
                .decodeList<UserDto>()
                .map { it.toDomain() }
            emit(Result.Success(leaders))
        } catch (e: Exception) {
            emit(Result.Error("Couldn't load leaders.", e))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun submitCheckIn(answers: Map<String, String>): Result<Unit> = try {
        val me = currentUid()
            ?: return Result.Error("Your session expired. Please sign in again.")
        val groupId = prefs.groupId.first()
        val leaderId = if (groupId.isNullOrBlank()) null else runCatching {
            supabase.from("groups")
                .select { filter { eq("id", groupId) } }
                .decodeSingleOrNull<GroupDto>()?.leaderId
        }.getOrNull()
        supabase.from("checkins").upsert(
            CheckinInsertDto(me, leaderId, answers),
            onConflict = "user_id,week_start"
        )
        Result.Success(Unit)
    } catch (e: Exception) {
        val raw = e.message.orEmpty()
        val hint = when {
            raw.contains("42P10") || raw.contains("ON CONFLICT") ->
                " (DB missing the week_start unique constraint — " +
                    "ask admin to run feature-checkins-once-per-week.sql.)"
            raw.contains("row-level security", true) ||
                raw.contains("42501") ->
                " (Permission denied by RLS.)"
            else -> ""
        }
        val tail = raw.take(160).ifBlank { "no detail from server" }
        Result.Error("Couldn't submit your check-in: $tail$hint", e)
    }

    override suspend fun hasCheckedInThisWeek(): Result<Boolean> {
        return try {
            val me = currentUid() ?: return Result.Success(false)
            if (!networkMonitor.isOnline) return Result.Success(false)
            val mondayIso = currentMondayIsoDate()
            val rows = supabase.from("checkins")
                .select {
                    filter {
                        eq("user_id", me)
                        eq("week_start", mondayIso)
                    }
                    limit(1)
                }
                .decodeList<kotlinx.serialization.json.JsonObject>()
            Result.Success(rows.isNotEmpty())
        } catch (_: Exception) {
            Result.Success(false)
        }
    }

    override suspend fun getCurrentWeekCheckIn(): Result<Map<String, String>?> {
        return try {
            val me = currentUid() ?: return Result.Success(null)
            if (!networkMonitor.isOnline) return Result.Success(null)
            val mondayIso = currentMondayIsoDate()
            val row = supabase.from("checkins")
                .select {
                    filter {
                        eq("user_id", me)
                        eq("week_start", mondayIso)
                    }
                    limit(1)
                }
                .decodeSingleOrNull<CheckinDto>()
            Result.Success(row?.answers)
        } catch (_: Exception) {
            Result.Success(null)
        }
    }

    private fun currentMondayIsoDate(): String {
        val monday = java.time.LocalDate.now()
            .with(java.time.temporal.TemporalAdjusters.previousOrSame(
                java.time.DayOfWeek.MONDAY
            ))
        return monday.toString()
    }


    override suspend fun getMyMentees(): Result<List<Mentee>> {
        val me = currentUid()
            ?: return Result.Error("Your session expired. Please sign in again.")
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to load your mentees.")
        }
        return try {
            val myRole = (prefs.userRole.first() ?: "member").lowercase()
            val mentees: List<UserDto> = when (myRole) {
                "cell_leader" -> {
                    val myGroups = supabase.from("groups")
                        .select { filter { eq("leader_id", me) } }
                        .decodeList<GroupDto>()
                        .map { it.id }
                    if (myGroups.isEmpty()) emptyList()
                    else supabase.from("users")
                        .select {
                            filter {
                                isIn("group_id", myGroups)
                                eq("role", "member")
                            }
                        }
                        .decodeList<UserDto>()
                }
                "youth_president", "pastor", "admin" -> {
                    supabase.from("users")
                        .select { filter { eq("role", "member") } }
                        .decodeList<UserDto>()
                }
                else -> emptyList()
            }
            if (mentees.isEmpty()) return Result.Success(emptyList())

            val allCheckins = runCatching {
                supabase.from("checkins")
                    .select {
                        filter {
                            if (myRole == "cell_leader") eq("leader_id", me)
                        }
                        order("submitted_at", Order.DESCENDING)
                    }
                    .decodeList<CheckinDto>()
            }.getOrDefault(emptyList())
            val checkInByMentee: Map<String, CheckinDto> = allCheckins
                .groupBy { it.userId }
                .mapValues { it.value.first() }

            val result = mentees.map { dto ->
                val checkin = checkInByMentee[dto.id]
                Mentee(
                    user = dto.toDomain(),
                    lastCheckInAt = checkin?.submittedAt?.let { parseIsoToLocal(it) }
                )
            }.sortedWith(
                compareByDescending<Mentee> { it.lastCheckInAt }
                    .thenBy { it.user.name }
            )
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error("Couldn't load your mentees.", e)
        }
    }

    override suspend fun getMemberLatestCheckIn(memberId: String): Result<CheckIn?> {
        val me = currentUid()
            ?: return Result.Error("Your session expired. Please sign in again.")
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to view check-ins.")
        }
        return try {
            val myRole = (prefs.userRole.first() ?: "member").lowercase()
            val rows = supabase.from("checkins")
                .select {
                    filter {
                        eq("user_id", memberId)
                        if (myRole == "cell_leader") eq("leader_id", me)
                    }
                    order("submitted_at", Order.DESCENDING)
                    limit(1)
                }
                .decodeList<CheckinDto>()
            val dto = rows.firstOrNull()
            Result.Success(dto?.let {
                CheckIn(
                    id = it.id,
                    userId = it.userId,
                    leaderId = it.leaderId,
                    answers = it.answers,
                    submittedAt = it.submittedAt?.let { iso -> parseIsoToLocal(iso) }
                        ?: java.time.LocalDateTime.now()
                )
            })
        } catch (e: Exception) {
            Result.Error("Couldn't load the check-in.", e)
        }
    }

    private fun parseIsoToLocal(iso: String): java.time.LocalDateTime? = runCatching {
        OffsetDateTime.parse(iso)
            .atZoneSameInstant(java.time.ZoneId.systemDefault())
            .toLocalDateTime()
    }.getOrNull()

    override suspend fun markProxyAttendance(
        eventId: String,
        memberId: String,
        status: com.grace.app.domain.model.AttendanceStatus
    ): Result<Unit> {
        if (status == com.grace.app.domain.model.AttendanceStatus.ABSENT) {
            return Result.Error("ABSENT is derived, not stored — remove the row instead.")
        }
        val me = currentUid() ?: return Result.Error("Your session expired.")
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to record attendance.")
        }
        return try {
            val dbStatus = when (status) {
                com.grace.app.domain.model.AttendanceStatus.PRESENT -> "present"
                com.grace.app.domain.model.AttendanceStatus.LATE -> "late"
                com.grace.app.domain.model.AttendanceStatus.EXCUSED -> "excused"
                else -> "present"
            }
            supabase.from("event_attendance").insert(
                com.grace.app.data.remote.supabase.dto.EventAttendanceDto(
                    eventId = eventId,
                    userId = memberId,
                    status = dbStatus,
                    lateByMinutes = 0,
                    postedByProxy = me
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            val friendly = when {
                msg.contains("duplicate key", ignoreCase = true) ->
                    "This member already has an attendance record for this event."
                msg.contains("violates row-level security", ignoreCase = true) ->
                    "You don't have permission to mark this member."
                else -> "Couldn't record attendance. Try again."
            }
            Result.Error(friendly, e)
        }
    }

    override suspend fun removeProxyAttendance(
        eventId: String,
        memberId: String
    ): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to undo.")
        }
        return try {
            supabase.from("event_attendance").delete {
                filter {
                    eq("event_id", eventId)
                    eq("user_id", memberId)
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Couldn't undo. Try again.", e)
        }
    }

    override suspend fun getEventRosterForLeader(
        eventId: String
    ): Result<List<com.grace.app.domain.model.Attendee>> {
        val me = currentUid() ?: return Result.Error("Your session expired.")
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to load the roster.")
        }
        return try {
            val myRole = (prefs.userRole.first() ?: "member").lowercase()
            val myGroup = prefs.groupId.first()
            val members: List<UserDto> = when (myRole) {
                "cell_leader" -> {
                    if (myGroup.isNullOrBlank()) emptyList()
                    else supabase.from("users")
                        .select {
                            filter {
                                eq("group_id", myGroup)
                                eq("role", "member")
                            }
                        }
                        .decodeList<UserDto>()
                }
                "youth_president", "pastor", "admin" -> {
                    supabase.from("users")
                        .select { filter { eq("role", "member") } }
                        .decodeList<UserDto>()
                }
                else -> emptyList()
            }
            if (members.isEmpty()) return Result.Success(emptyList())

            val memberIds = members.map { it.id }
            val attendance = supabase.from("event_attendance")
                .select {
                    filter {
                        eq("event_id", eventId)
                        isIn("user_id", memberIds)
                    }
                }
                .decodeList<com.grace.app.data.remote.supabase.dto.EventAttendanceDto>()
            val attendanceByUser = attendance.associateBy { it.userId }

            val result = members.map { dto ->
                val row = attendanceByUser[dto.id]
                com.grace.app.domain.model.Attendee(
                    user = dto.toDomain(),
                    status = if (row == null)
                        com.grace.app.domain.model.AttendanceStatus.ABSENT
                    else
                        com.grace.app.data.remote.supabase.dto.mapper
                            .parseAttendanceStatus(row.status),
                    lateByMinutes = row?.lateByMinutes ?: 0
                )
            }.sortedWith(
                compareBy<com.grace.app.domain.model.Attendee> {
                    when (it.status) {
                        com.grace.app.domain.model.AttendanceStatus.ABSENT -> 0
                        else -> 1
                    }
                }.thenBy { it.user.name }
            )
            Result.Success(result)
        } catch (e: Exception) {
            Result.Error("Couldn't load the roster.", e)
        }
    }

    override suspend fun getMemberAttendance(
        memberId: String
    ): Result<List<com.grace.app.domain.model.AttendedEvent>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to load attendance.")
        }
        return try {
            val rows = supabase.from("event_attendance")
                .select { filter { eq("user_id", memberId) } }
                .decodeList<com.grace.app.data.remote.supabase.dto.EventAttendanceDto>()
            if (rows.isEmpty()) return Result.Success(emptyList())

            val eventIds = rows.map { it.eventId }
            val events = supabase.from("events")
                .select { filter { isIn("id", eventIds) } }
                .decodeList<com.grace.app.data.remote.supabase.dto.EventDto>()
                .associateBy { it.id }

            val attended = rows.mapNotNull { row ->
                val dto = events[row.eventId] ?: return@mapNotNull null
                val event = dto.toEntity().toDomain(
                    myRsvp = null,
                    goingCount = 0,
                    iHaveAttended = true
                )
                val attendedAt = row.attendedAt
                    ?.let {
                        runCatching {
                            OffsetDateTime.parse(it)
                                .atZoneSameInstant(java.time.ZoneId.systemDefault())
                                .toLocalDateTime()
                        }.getOrNull()
                    } ?: event.eventDate
                com.grace.app.domain.model.AttendedEvent(
                    event = event,
                    attendedAt = attendedAt,
                    status = com.grace.app.data.remote.supabase.dto.mapper
                        .parseAttendanceStatus(row.status),
                    lateByMinutes = row.lateByMinutes
                )
            }.sortedByDescending { it.attendedAt }
            Result.Success(attended)
        } catch (e: Exception) {
            Result.Error("Couldn't load this member's attendance.", e)
        }
    }

    override suspend fun postPrayerOnBehalf(
        memberId: String,
        content: String,
        category: com.grace.app.domain.model.PrayerCategory
    ): Result<Unit> {
        val me = currentUid() ?: return Result.Error("Your session expired.")
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to post the prayer.")
        }
        val cleanContent = content.trim()
        if (cleanContent.length < 3) {
            return Result.Error("Prayer text is too short.")
        }
        return try {
            supabase.from("prayers").insert(
                com.grace.app.data.remote.supabase.dto.PrayerDto(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = memberId,
                    userName = null,
                    content = cleanContent,
                    isAnonymous = false,
                    category = category.name.lowercase(),
                    status = "active",
                    prayCount = 0,
                    isFlagged = false,
                    expiresAt = null,
                    createdAt = null,
                    postedByProxy = me
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            val friendly = when {
                msg.contains("violates row-level security", ignoreCase = true) ->
                    "You don't have permission to post for this member."
                else -> "Couldn't post the prayer. Try again."
            }
            Result.Error(friendly, e)
        }
    }

    override suspend fun logReflectionOnBehalf(
        memberId: String,
        meditationId: String,
        reflectionText: String
    ): Result<Unit> {
        val me = currentUid() ?: return Result.Error("Your session expired.")
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to log the reflection.")
        }
        val cleanText = reflectionText.trim()
        if (cleanText.length < 3) {
            return Result.Error("Reflection text is too short.")
        }
        return try {
            supabase.from("user_meditation_submissions").insert(
                com.grace.app.data.remote.supabase.dto.MeditationSubmissionInsertDto(
                    userId = memberId,
                    meditationId = meditationId,
                    reflectionText = cleanText,
                    submittedByProxy = me
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            val friendly = when {
                msg.contains("duplicate key", ignoreCase = true) ->
                    "This member already has a reflection logged for this week."
                msg.contains("violates row-level security", ignoreCase = true) ->
                    "You don't have permission to log for this member."
                else -> "Couldn't log the reflection. Try again."
            }
            Result.Error(friendly, e)
        }
    }

    override suspend fun addProxyMember(
        name: String,
        birthdate: java.time.LocalDate,
        sex: String,
        isCompassion: Boolean,
        compassionNumber: String?,
        emergencyContact: String?,
        email: String?
    ): Result<String> {
        val me = currentUid() ?: return Result.Error("Your session expired. Sign in again.")
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to register a member.")
        }
        val myGroup = prefs.groupId.first()
        if (myGroup.isNullOrBlank() &&
            (prefs.userRole.first() ?: "member").lowercase() == "cell_leader") {
            return Result.Error("You need an assigned cell to register members.")
        }
        return try {
            val cleanEmail = email?.trim()?.takeIf { it.isNotEmpty() }
            if (cleanEmail != null) {
                val existing = supabase.from("users")
                    .select { filter { eq("email", cleanEmail) } }
                    .decodeList<UserDto>()
                if (existing.isNotEmpty()) {
                    return Result.Error(
                        "An account with that email already exists. " +
                            "Leave email blank, or ask them to sign in instead."
                    )
                }
            }
            val newId = java.util.UUID.randomUUID().toString()
            val dto = UserDto(
                id = newId,
                email = cleanEmail,
                name = name.trim(),
                role = "member",
                groupId = myGroup,
                isCompassion = isCompassion,
                compassionNumber = if (isCompassion) compassionNumber else null,
                emergencyContact = emergencyContact?.trim()?.takeIf { it.isNotEmpty() },
                isProxyOnly = true,
                createdByProxy = me,
                birthdate = birthdate.toString(),
                sex = sex
            )
            supabase.from("users").insert(dto)
            Result.Success(newId)
        } catch (e: Exception) {
            Result.Error("Couldn't register member. " + (e.message ?: "Try again."), e)
        }
    }
}
