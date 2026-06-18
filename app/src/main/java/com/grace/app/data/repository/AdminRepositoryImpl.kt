package com.grace.app.data.repository

import com.grace.app.data.remote.supabase.dto.EventDto
import com.grace.app.data.remote.supabase.dto.GroupDto
import com.grace.app.data.remote.supabase.dto.MeditationSubmissionDto
import com.grace.app.data.remote.supabase.dto.UserDto
import com.grace.app.data.remote.supabase.dto.mapper.parseAttendanceStatus
import com.grace.app.data.remote.supabase.dto.mapper.parseDateTime
import com.grace.app.data.remote.supabase.dto.mapper.toDomain
import com.grace.app.data.remote.supabase.dto.mapper.toEntity
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.model.AdminAttendanceRecord
import com.grace.app.domain.model.Group
import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.User
import com.grace.app.domain.model.UserRole
import com.grace.app.domain.repository.AdminRepository
import com.grace.app.domain.repository.BulkEmailResult
import com.grace.app.domain.repository.EmailAudience
import com.grace.app.domain.util.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val networkMonitor: NetworkMonitor
) : AdminRepository {

    override suspend fun getAllUsers(): Result<List<User>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to manage users.")
        }
        return try {
            val users = supabase.from("users")
                .select { order("name", Order.ASCENDING) }
                .decodeList<UserDto>()
                .map { it.toDomain() }
            Result.Success(users)
        } catch (e: Exception) {
            Result.Error(adminFriendly(e), e)
        }
    }

    override suspend fun updateUserRole(
        targetUserId: String,
        newRole: UserRole
    ): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to update roles.")
        }
        return try {
            supabase.from("users").update({
                set("role", newRole.name.lowercase())
            }) {
                filter { eq("id", targetUserId) }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(adminFriendly(e), e)
        }
    }

    override suspend fun listGroups(): Result<List<Group>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to load groups.")
        }
        return try {
            val groups = supabase.from("groups")
                .select { order("name", Order.ASCENDING) }
                .decodeList<GroupDto>()
                .map { Group(it.id, it.name, it.leaderId, it.description) }
            Result.Success(groups)
        } catch (e: Exception) {
            Result.Error(adminFriendly(e), e)
        }
    }

    override suspend fun sendBulkEmail(
        subject: String,
        message: String,
        audience: EmailAudience
    ): Result<BulkEmailResult> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to send the email.")
        }
        if (subject.isBlank() || message.isBlank()) {
            return Result.Error("Subject and message are required.")
        }
        return try {
            val audienceJson: JsonObject = when (audience) {
                EmailAudience.All -> buildJsonObject { put("kind", "all") }
                is EmailAudience.Roles -> buildJsonObject {
                    put("kind", "roles")
                    putJsonArray("roles") {
                        audience.roles.forEach { add(JsonPrimitive(it.name.lowercase())) }
                    }
                }
                is EmailAudience.Group -> buildJsonObject {
                    put("kind", "group")
                    put("group_id", audience.groupId)
                }
            }
            val payload = buildJsonObject {
                put("subject", subject)
                put("message", message)
                put("audience", audienceJson)
            }
            val response = supabase.functions.invoke("send-bulk-email", payload)
            if (response.status != HttpStatusCode.OK) {
                val raw = response.bodyAsText()
                return Result.Error(
                    when (response.status.value) {
                        403 -> "Only leaders/admins can send announcements."
                        else -> "Email failed: $raw"
                    }
                )
            }
            val parsed = Json.decodeFromString<BulkEmailResponseDto>(response.bodyAsText())
            Result.Success(BulkEmailResult(parsed.recipients, parsed.sent))
        } catch (e: Exception) {
            Result.Error(adminFriendly(e), e)
        }
    }

    @Serializable
    private data class BulkEmailResponseDto(
        @SerialName("recipients") val recipients: Int,
        @SerialName("sent") val sent: Int
    )


    @Serializable
    private data class AttendanceWithEventDto(
        @SerialName("user_id") val userId: String,
        @SerialName("attended_at") val attendedAt: String,
        @SerialName("status") val status: String = "present",
        @SerialName("late_by_minutes") val lateByMinutes: Int = 0,
        @SerialName("events") val event: EventDto? = null
    )

    override suspend fun getAllAttendance():
        Result<List<AdminAttendanceRecord>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to load compliance data.")
        }
        return try {
            val rows = supabase.from("event_attendance")
                .select(io.github.jan.supabase.postgrest.query.Columns.raw(
                    "user_id, attended_at, status, late_by_minutes, events(*)"
                ))
                .decodeList<AttendanceWithEventDto>()
            val records = rows.mapNotNull { row ->
                val ev = row.event ?: return@mapNotNull null
                AdminAttendanceRecord(
                    userId = row.userId,
                    event = ev.toEntity().toDomain(),
                    attendedAt = parseDateTime(row.attendedAt),
                    status = parseAttendanceStatus(row.status),
                    lateByMinutes = row.lateByMinutes
                )
            }
            Result.Success(records)
        } catch (e: Exception) {
            Result.Error(adminFriendly(e), e)
        }
    }

    override suspend fun getAllMeditationSubmissions():
        Result<List<MeditationSubmission>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to load reflections.")
        }
        return try {
            val subs = supabase.from("user_meditation_submissions")
                .select()
                .decodeList<MeditationSubmissionDto>()
                .map { it.toDomain() }
            Result.Success(subs)
        } catch (e: Exception) {
            Result.Error(adminFriendly(e), e)
        }
    }

    private fun adminFriendly(e: Exception): String = when {
        e.message?.contains("row-level security", ignoreCase = true) == true ->
            "You don't have permission to do that. Ask the Youth Leader or admin."
        else -> "Couldn't reach the server. Try again."
    }
}
