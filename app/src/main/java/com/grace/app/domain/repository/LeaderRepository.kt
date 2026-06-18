package com.grace.app.domain.repository

import com.grace.app.domain.model.AttendanceStatus
import com.grace.app.domain.model.Attendee
import com.grace.app.domain.model.AttendedEvent
import com.grace.app.domain.model.CheckIn
import com.grace.app.domain.model.Mentee
import com.grace.app.domain.model.PrayerCategory
import com.grace.app.domain.model.User
import com.grace.app.domain.util.Result
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface LeaderRepository {
    fun getMyLeader(): Flow<Result<User?>>
    suspend fun submitCheckIn(answers: Map<String, String>): Result<Unit>
    fun getAllLeaders(): Flow<Result<List<User>>>

    suspend fun hasCheckedInThisWeek(): Result<Boolean>

    suspend fun getCurrentWeekCheckIn(): Result<Map<String, String>?>

    suspend fun getMyMentees(): Result<List<Mentee>>

    suspend fun getMemberLatestCheckIn(memberId: String): Result<CheckIn?>

    suspend fun addProxyMember(
        name: String,
        birthdate: LocalDate,
        sex: String,
        isCompassion: Boolean,
        compassionNumber: String?,
        emergencyContact: String?,
        email: String?
    ): Result<String>

    suspend fun markProxyAttendance(
        eventId: String,
        memberId: String,
        status: AttendanceStatus
    ): Result<Unit>

    suspend fun removeProxyAttendance(
        eventId: String,
        memberId: String
    ): Result<Unit>

    suspend fun getEventRosterForLeader(eventId: String): Result<List<Attendee>>

    suspend fun postPrayerOnBehalf(
        memberId: String,
        content: String,
        category: PrayerCategory
    ): Result<Unit>

    suspend fun logReflectionOnBehalf(
        memberId: String,
        meditationId: String,
        reflectionText: String
    ): Result<Unit>

    suspend fun getMemberAttendance(memberId: String): Result<List<AttendedEvent>>
}
