package com.grace.app.domain.repository

import com.grace.app.domain.model.AdminAttendanceRecord
import com.grace.app.domain.model.Group
import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.User
import com.grace.app.domain.model.UserRole
import com.grace.app.domain.util.Result

sealed class EmailAudience {
    data object All : EmailAudience()
    data class Roles(val roles: Set<UserRole>) : EmailAudience()
    data class Group(val groupId: String) : EmailAudience()
}

data class BulkEmailResult(val recipients: Int, val sent: Int)

interface AdminRepository {

    suspend fun getAllUsers(): Result<List<User>>

    suspend fun updateUserRole(targetUserId: String, newRole: UserRole): Result<Unit>

    suspend fun listGroups(): Result<List<Group>>

    suspend fun sendBulkEmail(
        subject: String,
        message: String,
        audience: EmailAudience
    ): Result<BulkEmailResult>

    suspend fun getAllAttendance(): Result<List<AdminAttendanceRecord>>

    suspend fun getAllMeditationSubmissions(): Result<List<MeditationSubmission>>
}
