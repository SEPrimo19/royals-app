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

    /**
     * Whether the current user has already submitted a check-in for the
     * CURRENT ISO week (Mon → Sun). Used by the Leader screen to flip
     * the form into edit-mode rather than "you've already done this".
     */
    suspend fun hasCheckedInThisWeek(): Result<Boolean>

    /**
     * Returns the user's current ISO-week check-in answers if they've
     * submitted this week, or null if not yet. Used to pre-fill the form
     * in edit mode — same row gets UPSERTed when they tap Update.
     */
    suspend fun getCurrentWeekCheckIn(): Result<Map<String, String>?>

    /**
     * Leader-side directory: members this leader mentors, enriched with
     * the date of their most recent check-in. Cell leaders see their
     * group's members; senior leaders (youth_president / pastor / admin)
     * see all members. Returns empty list for a member calling it.
     */
    suspend fun getMyMentees(): Result<List<Mentee>>

    /**
     * The most recent check-in this member submitted to the current
     * leader (or any check-in, for senior leaders). Null if the member
     * has never checked in. Powers the leader's MemberDetail screen.
     */
    suspend fun getMemberLatestCheckIn(memberId: String): Result<CheckIn?>

    /**
     * Leader Proxy Mode (Phase P.1) — registers a member who doesn't have
     * a smartphone. Creates a `users` row with `is_proxy_only = TRUE`,
     * assigned to the calling leader's group (or any group, for senior
     * leaders). Returns the new member's id on success.
     *
     * RLS enforces that the caller is a leader+ AND inserts into a group
     * they're allowed to (own group for cell_leader, any group for
     * youth_president/pastor/admin). Email collision is checked
     * client-side before insert so users get a clear error message.
     *
     * Email is OPTIONAL: pass null for members who don't have one. Phase
     * P.5 (future) will use the email to enable a "claim this record"
     * flow when the member eventually gets a phone.
     */
    suspend fun addProxyMember(
        name: String,
        birthdate: LocalDate,
        sex: String,
        isCompassion: Boolean,
        compassionNumber: String?,
        emergencyContact: String?,
        email: String?
    ): Result<String>

    /**
     * Leader Proxy Mode (Phase P.2) — record attendance on behalf of a
     * cell member who couldn't scan the QR themselves. The backend
     * INSERT policy enforces that the caller is a leader and the target
     * member is in their cell (or any cell, for senior leaders).
     *
     * Unlike member-self attendance which is time-window-bounded, this
     * accepts the leader's chosen status verbatim and skips the window
     * check — leaders often log attendance AFTER a meeting wraps up.
     *
     * Allowed status values: PRESENT, LATE, EXCUSED. ABSENT is derived
     * and should never be passed (returns Error if it is).
     */
    suspend fun markProxyAttendance(
        eventId: String,
        memberId: String,
        status: AttendanceStatus
    ): Result<Unit>

    /**
     * Undo a previously-recorded proxy attendance row. Used when a leader
     * accidentally marked the wrong member. RLS limits this to rows the
     * leader is allowed to act on (same scope as markProxyAttendance).
     */
    suspend fun removeProxyAttendance(
        eventId: String,
        memberId: String
    ): Result<Unit>

    /**
     * Roster for the leader's cell at a specific event — every member of
     * their group with their per-event attendance state attached. Members
     * with no attendance row appear with status = ABSENT so the UI can
     * surface the "mark them" action. Senior leaders see everyone, not
     * just one cell.
     */
    suspend fun getEventRosterForLeader(eventId: String): Result<List<Attendee>>

    /**
     * Leader Proxy Mode (Phase P.3) — posts a prayer to the Prayer Wall
     * on behalf of a cell member. The prayer card will render with a
     * "(via {leader})" tag so the community knows it came through a
     * leader rather than the member directly — pastoral transparency.
     *
     * Proxy prayers CANNOT be anonymous (SQL CHECK enforces this) — the
     * "via" tag would defeat the anonymity purpose. UI hides the toggle
     * for proxy posts.
     */
    suspend fun postPrayerOnBehalf(
        memberId: String,
        content: String,
        category: PrayerCategory
    ): Result<Unit>

    /**
     * Leader Proxy Mode (Phase P.3) — logs a weekly meditation reflection
     * on behalf of a cell member (typically from a paper journal they
     * handed in). The submission counts toward the member's Compassion
     * compliance the same as a self-entered reflection.
     */
    suspend fun logReflectionOnBehalf(
        memberId: String,
        meditationId: String,
        reflectionText: String
    ): Result<Unit>

    /**
     * Phase P.4 — fetch a single member's attendance history for the
     * leader-built compliance report. RLS allows cell_leader to read
     * event_attendance rows belonging to members in their group; senior
     * leaders see everyone. Returns the same AttendedEvent shape that
     * EventRepository.getMyAttendance returns for the current user, so
     * the existing report builder can consume it unchanged.
     */
    suspend fun getMemberAttendance(memberId: String): Result<List<AttendedEvent>>
}
