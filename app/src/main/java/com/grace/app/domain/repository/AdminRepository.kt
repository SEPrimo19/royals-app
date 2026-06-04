package com.grace.app.domain.repository

import com.grace.app.domain.model.AdminAttendanceRecord
import com.grace.app.domain.model.Group
import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.User
import com.grace.app.domain.model.UserRole
import com.grace.app.domain.util.Result

/**
 * Audience selector for the bulk-email feature. Drives both UI presentation
 * and the request body sent to the send-bulk-email Edge Function.
 */
sealed class EmailAudience {
    data object All : EmailAudience()
    data class Roles(val roles: Set<UserRole>) : EmailAudience()
    data class Group(val groupId: String) : EmailAudience()
}

/** Server returns counts so the UI can tell the admin "sent X of Y". */
data class BulkEmailResult(val recipients: Int, val sent: Int)

interface AdminRepository {

    /**
     * Fetches every user. Admin-only screen — RLS rejects this call unless the
     * caller is pastor/admin. No Room cache: lists are short and accuracy
     * matters more than offline support.
     */
    suspend fun getAllUsers(): Result<List<User>>

    /**
     * Updates a user's role. The Postgres trigger `trg_log_role_change` writes
     * an audit_log row automatically — no client-side log call needed.
     */
    suspend fun updateUserRole(targetUserId: String, newRole: UserRole): Result<Unit>

    /** Cell groups available as audience pickers in the bulk-email screen. */
    suspend fun listGroups(): Result<List<Group>>

    /**
     * Fire-and-forget bulk email via the send-bulk-email Edge Function.
     * Returns the recipients/sent counts so the UI can show "sent X of Y".
     * The function itself enforces leader-role auth — bad callers get a
     * 403 mapped here to a user-friendly string.
     */
    suspend fun sendBulkEmail(
        subject: String,
        message: String,
        audience: EmailAudience
    ): Result<BulkEmailResult>

    /**
     * Fetches every attendance row across every user. Used by the compliance
     * report screen to build the per-user roster. RLS rejects this call
     * unless the caller is cell_leader+ (cell_leader sees their group only,
     * senior roles see everything) — the report screen is admin-gated upstream.
     *
     * Returned rows already include the joined event so the caller doesn't
     * need a second round trip.
     */
    suspend fun getAllAttendance(): Result<List<AdminAttendanceRecord>>

    /**
     * Fetches every meditation reflection submission across every user.
     * Same RLS gating as [getAllAttendance].
     */
    suspend fun getAllMeditationSubmissions(): Result<List<MeditationSubmission>>
}
