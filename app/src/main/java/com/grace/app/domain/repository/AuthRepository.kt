package com.grace.app.domain.repository

import com.grace.app.domain.model.Group
import com.grace.app.domain.model.User
import com.grace.app.domain.model.UserRole
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<User>
    suspend fun signUp(email: String, password: String, name: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
    fun currentUser(): Flow<User?>
    suspend fun deleteAccount(): Result<Unit>

    suspend fun signInWithGoogle(): Result<Unit>

    suspend fun getGroups(): Result<List<Group>>
    suspend fun completeProfile(
        role: UserRole,
        groupId: String?,
        isCompassion: Boolean = false,
        compassionNumber: String? = null,
        emergencyContact: String? = null,
        birthdate: java.time.LocalDate? = null,
        sex: String? = null
    ): Result<Unit>

    suspend fun refreshSessionIfNeeded(): Boolean

    suspend fun syncProfileFromServer(): Result<Unit>

    suspend fun getMyProfile(): Result<User?>

    suspend fun updateMyProfile(
        name: String,
        bio: String?,
        messengerUrl: String?,
        messengerPublic: Boolean,
        isCompassion: Boolean = false,
        compassionNumber: String? = null,
        emergencyContact: String? = null,
        birthdate: java.time.LocalDate? = null,
        sex: String? = null
    ): Result<Unit>

    suspend fun changePassword(newPassword: String): Result<Unit>

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun isEmailPasswordUser(): Boolean

    suspend fun findClaimableProxy(): Result<User?>

    suspend fun claimProxyRecord(proxyUserId: String): Result<Unit>
}
