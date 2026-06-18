package com.grace.app.data.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.grace.app.data.datastore.SecureTokenStore
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.remote.supabase.dto.GroupDto
import com.grace.app.data.remote.supabase.dto.UserDto
import com.grace.app.data.remote.supabase.dto.mapper.parseRoleString
import com.grace.app.data.remote.supabase.dto.mapper.toDomain
import com.grace.app.data.util.CrashReporter
import com.grace.app.domain.model.Group
import com.grace.app.domain.model.User
import com.grace.app.domain.model.UserRole
import com.grace.app.domain.repository.AuthRepository
import com.grace.app.domain.util.Result
import com.grace.app.worker.OfflineSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient,
    private val prefs: UserPreferencesRepo,
    private val secureTokenStore: SecureTokenStore,
    private val networkMonitor: com.grace.app.data.util.NetworkMonitor
) : AuthRepository {

    private fun enqueueOfflineDrain() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            OfflineSyncWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<OfflineSyncWorker>().build()
        )
    }


    private fun friendlyError(e: Throwable): String = when (e) {
        is RestException -> when (e.statusCode) {
            400 -> "Invalid email or password."
            401 -> "Invalid email or password."
            404 -> "Account not found."
            422 -> {
                val raw = e.message.orEmpty()
                when {
                    raw.contains("already", true) ||
                        raw.contains("registered", true) ->
                        "That email is already registered."
                    raw.contains("password", true) ->
                        "Password is too weak — try a longer or less-common one."
                    else -> "Couldn't sign up: ${raw.take(140)}"
                }
            }
            429 -> "Too many tries. Wait a minute and try again."
            in 500..599 ->
                "Server error (${e.statusCode}). Try again in a moment."
            else -> "Error ${e.statusCode}: ${e.message.orEmpty().take(140).ifBlank { "unknown" }}"
        }
        else -> e.message?.take(180) ?: "Network error. Check your connection and try again."
    }

    override suspend fun signIn(email: String, password: String): Result<User> = try {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val info = supabase.auth.currentUserOrNull()
            ?: return Result.Error("Sign in failed. Please try again.")
        val uid = info.id
        val dto = supabase.from("users")
            .select { filter { eq("id", uid) } }
            .decodeSingleOrNull<UserDto>()
        val user = dto?.toDomain() ?: User(
            id = uid,
            email = info.email ?: email,
            name = info.email ?: email,
            avatarUrl = null,
            role = UserRole.MEMBER,
            groupId = null,
            streak = 0,
            lastDevoAt = null,
            fcmToken = null
        )
        prefs.saveSession(
            userId = user.id,
            name = user.name,
            email = user.email,
            role = user.role.name.lowercase(),
            groupId = user.groupId
        )
        prefs.reconcileStreakFromServer(
            serverStreak = user.streak,
            serverLastDevoIso = user.lastDevoAt?.toString()
        )
        secureTokenStore.saveRefreshToken(supabase.auth.currentSessionOrNull()?.refreshToken)
        CrashReporter.setUserId(user.id)
        CrashReporter.setKey("role", user.role.name.lowercase())
        enqueueOfflineDrain()
        Result.Success(user)
    } catch (e: Exception) {
        CrashReporter.recordNonFatal(e)
        Result.Error(friendlyError(e), e)
    }

    override suspend fun signUp(
        email: String,
        password: String,
        name: String
    ): Result<Unit> {
        return try {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val uid = supabase.auth.currentUserOrNull()?.id
            if (uid != null) {
                val proxyCheckJson: kotlinx.serialization.json.JsonElement =
                    runCatching {
                        supabase
                            .pluginManager
                            .getPlugin(io.github.jan.supabase.postgrest.Postgrest)
                            .rpc(
                                function = "find_proxy_by_email",
                                parameters = kotlinx.serialization.json.buildJsonObject {
                                    put("p_email",
                                        kotlinx.serialization.json.JsonPrimitive(email))
                                }
                            )
                            .decodeAs<kotlinx.serialization.json.JsonElement>()
                    }.getOrNull() ?: kotlinx.serialization.json.JsonNull
                val proxyFound = (proxyCheckJson as? kotlinx.serialization.json.JsonObject)
                    ?.get("found")
                    ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull }
                    ?: false
                if (!proxyFound) {
                    supabase.from("users").insert(
                        UserDto(id = uid, email = email, name = name, role = "member")
                    )
                }
                prefs.saveSession(uid, name, email, "member", null)
                secureTokenStore.saveRefreshToken(
                    supabase.auth.currentSessionOrNull()?.refreshToken
                )
                CrashReporter.setUserId(uid)
                CrashReporter.setKey("role", "member")
                enqueueOfflineDrain()
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            CrashReporter.recordNonFatal(e)
            Result.Error(friendlyError(e), e)
        }
    }

    override suspend fun signInWithGoogle(): Result<Unit> = try {
        supabase.auth.signInWith(Google) {
            scopes.add("email")
            scopes.add("profile")
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Couldn't open Google sign-in. Try again.", e)
    }

    override suspend fun signOut(): Result<Unit> = try {
        supabase.auth.signOut()
        prefs.clearAll()
        secureTokenStore.clear()
        CrashReporter.setUserId(null)
        Result.Success(Unit)
    } catch (e: Exception) {
        prefs.clearAll()
        secureTokenStore.clear()
        CrashReporter.setUserId(null)
        Result.Success(Unit)
    }

    override fun currentUser(): Flow<User?> = combine(
        prefs.userId,
        prefs.userName,
        prefs.userEmail,
        prefs.userRole,
        prefs.groupId
    ) { id, name, email, role, groupId ->
        if (id.isNullOrBlank()) {
            null
        } else {
            User(
                id = id,
                email = email ?: "",
                name = name ?: "",
                avatarUrl = null,
                role = parseRoleString(role),
                groupId = groupId,
                streak = 0,
                lastDevoAt = null,
                fcmToken = null
            )
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            supabase.functions.invoke("delete-account")
            supabase.auth.signOut()
            prefs.clearAll()
            secureTokenStore.clear()
            CrashReporter.setUserId(null)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(
                friendlyError(e).ifBlank {
                    "Couldn't delete your account. Please try again."
                },
                e
            )
        }
    }

    override suspend fun getGroups(): Result<List<Group>> = try {
        val groups = supabase.from("groups")
            .select()
            .decodeList<GroupDto>()
            .map { it.toDomain() }
        Result.Success(groups)
    } catch (e: Exception) {
        Result.Error(friendlyError(e), e)
    }

    override suspend fun completeProfile(
        role: UserRole,
        groupId: String?,
        isCompassion: Boolean,
        compassionNumber: String?,
        emergencyContact: String?,
        birthdate: java.time.LocalDate?,
        sex: String?
    ): Result<Unit> = try {
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: return Result.Error("Your session expired. Please sign in again.")
        supabase.from("users").update(
            {
                set("role", role.name.lowercase())
                set("group_id", groupId)
                set("is_compassion", isCompassion)
                set("compassion_number",
                    if (isCompassion) compassionNumber else null)
                set("emergency_contact",
                    emergencyContact?.trim()?.takeIf { it.isNotEmpty() })
                set("birthdate", birthdate?.toString())
                set("sex", sex?.takeIf { it == "M" || it == "F" })
            }
        ) {
            filter { eq("id", uid) }
        }
        prefs.setRoleAndGroup(role.name.lowercase(), groupId)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(friendlyError(e), e)
    }

    override suspend fun syncProfileFromServer(): Result<Unit> = try {
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: return Result.Success(Unit)
        val dto = supabase.from("users")
            .select { filter { eq("id", uid) } }
            .decodeSingleOrNull<UserDto>()
            ?: return Result.Success(Unit)
        prefs.saveSession(
            userId = dto.id,
            name = dto.name,
            email = dto.email.orEmpty(),
            role = dto.role,
            groupId = dto.groupId
        )
        prefs.reconcileStreakFromServer(
            serverStreak = dto.streak,
            serverLastDevoIso = dto.lastDevoAt
        )
        Result.Success(Unit)
    } catch (_: Exception) {
        Result.Success(Unit)
    }

    override suspend fun getMyProfile(): Result<User?> = try {
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: return Result.Error("Your session expired. Please sign in again.")
        val dto = supabase.from("users")
            .select { filter { eq("id", uid) } }
            .decodeSingleOrNull<UserDto>()
        Result.Success(dto?.toDomain())
    } catch (e: Exception) {
        Result.Error("Couldn't load your profile. Try again.", e)
    }

    override suspend fun updateMyProfile(
        name: String,
        bio: String?,
        messengerUrl: String?,
        messengerPublic: Boolean,
        isCompassion: Boolean,
        compassionNumber: String?,
        emergencyContact: String?,
        birthdate: java.time.LocalDate?,
        sex: String?
    ): Result<Unit> = try {
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: return Result.Error("Your session expired. Please sign in again.")
        supabase.from("users").update({
            set("name", name)
            set("bio", bio)
            set("messenger_url", messengerUrl)
            set("messenger_public", messengerPublic)
            set("is_compassion", isCompassion)
            set("compassion_number",
                if (isCompassion) compassionNumber else null)
            set("emergency_contact",
                emergencyContact?.trim()?.takeIf { it.isNotEmpty() })
            set("birthdate", birthdate?.toString())
            set("sex", sex?.takeIf { it == "M" || it == "F" })
        }) {
            filter { eq("id", uid) }
        }
        runCatching {
            prefs.saveSession(
                userId = uid,
                name = name,
                email = prefs.userEmail.first() ?: "",
                role = prefs.userRole.first() ?: "member",
                groupId = prefs.groupId.first()
            )
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Couldn't save your profile. Try again.", e)
    }

    override suspend fun changePassword(newPassword: String): Result<Unit> = try {
        supabase.auth.modifyUser { password = newPassword }
        Result.Success(Unit)
    } catch (e: Exception) {
        CrashReporter.recordNonFatal(e)
        Result.Error(friendlyError(e), e)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        supabase.auth.resetPasswordForEmail(email.trim())
        Result.Success(Unit)
    } catch (e: Exception) {
        CrashReporter.recordNonFatal(e)
        Result.Error(friendlyError(e), e)
    }

    override suspend fun isEmailPasswordUser(): Boolean = try {
        val info = supabase.auth.currentUserOrNull()
        info?.identities?.any { it.provider == "email" } ?: false
    } catch (_: Exception) { false }

    override suspend fun findClaimableProxy(): Result<User?> = try {
        val info = supabase.auth.currentUserOrNull()
            ?: return Result.Error("Not signed in.")
        val myEmail = info.email?.trim()?.lowercase()
            ?: return Result.Success(null)

        val resultJson = supabase
            .pluginManager
            .getPlugin(io.github.jan.supabase.postgrest.Postgrest)
            .rpc(
                function = "find_proxy_by_email",
                parameters = kotlinx.serialization.json.buildJsonObject {
                    put("p_email",
                        kotlinx.serialization.json.JsonPrimitive(myEmail))
                }
            )
            .decodeAs<kotlinx.serialization.json.JsonObject>()

        val found = (resultJson["found"] as? kotlinx.serialization.json.JsonPrimitive)
            ?.booleanOrNull == true
        val multiple = (resultJson["multiple"] as? kotlinx.serialization.json.JsonPrimitive)
            ?.booleanOrNull == true
        when {
            !found -> Result.Success(null)
            multiple -> Result.Error(
                "Multiple proxy records share this email. " +
                    "Ask your cell leader to resolve the duplicate."
            )
            else -> {
                val proxyId = (resultJson["proxy_id"]
                    as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
                    ?: return Result.Success(null)
                val name = (resultJson["name"]
                    as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull.orEmpty()
                val isCompassion = (resultJson["is_compassion"]
                    as? kotlinx.serialization.json.JsonPrimitive)?.booleanOrNull == true
                Result.Success(
                    User(
                        id = proxyId,
                        email = myEmail,
                        name = name,
                        avatarUrl = null,
                        role = com.grace.app.domain.model.UserRole.MEMBER,
                        groupId = null,
                        streak = 0,
                        lastDevoAt = null,
                        fcmToken = null,
                        isCompassion = isCompassion,
                        isProxyOnly = true
                    )
                )
            }
        }
    } catch (e: Exception) {
        Result.Error("Couldn't check for an existing record.", e)
    }

    override suspend fun claimProxyRecord(proxyUserId: String): Result<Unit> = try {
        supabase
            .pluginManager
            .getPlugin(io.github.jan.supabase.postgrest.Postgrest)
            .rpc(
                function = "claim_proxy_record",
                parameters = kotlinx.serialization.json.buildJsonObject {
                    put("proxy_id",
                        kotlinx.serialization.json.JsonPrimitive(proxyUserId))
                }
            )
        runCatching { syncProfileFromServer() }
        Result.Success(Unit)
    } catch (e: Exception) {
        CrashReporter.recordNonFatal(e)
        Result.Error(friendlyError(e), e)
    }

    private fun isNetworkError(e: Throwable): Boolean {
        var cur: Throwable? = e
        while (cur != null) {
            if (cur is java.io.IOException) return true
            cur = cur.cause
        }
        return false
    }

    override suspend fun refreshSessionIfNeeded(): Boolean {
        if (!networkMonitor.isOnline) {
            CrashReporter.log(
                "refreshSessionIfNeeded: offline; skipping refresh, keeping local state"
            )
            return supabase.auth.currentSessionOrNull() != null
        }

        if (supabase.auth.currentSessionOrNull() != null) {
            return try {
                supabase.auth.refreshCurrentSession()
                secureTokenStore.saveRefreshToken(
                    supabase.auth.currentSessionOrNull()?.refreshToken
                )
                true
            } catch (e: Exception) {
                if (isNetworkError(e)) {
                    CrashReporter.log(
                        "refreshCurrentSession: network error; keeping session"
                    )
                    return true
                }
                CrashReporter.log("refreshCurrentSession failed (auth); clearing")
                CrashReporter.recordNonFatal(e)
                secureTokenStore.clear()
                prefs.clearAll()
                CrashReporter.setUserId(null)
                false
            }
        }
        val storedRefresh = secureTokenStore.refreshToken()
        if (storedRefresh.isNullOrBlank()) {
            val prefsUid = prefs.userId.first()
            if (!prefsUid.isNullOrBlank()) {
                CrashReporter.log(
                    "refreshSessionIfNeeded: userId present but token missing — forcing re-login"
                )
                prefs.clearAll()
                CrashReporter.setUserId(null)
            } else {
                CrashReporter.log("refreshSessionIfNeeded: no stored token, leaving anon")
            }
            return false
        }
        return try {
            val newSession = supabase.auth.refreshSession(storedRefresh)
            supabase.auth.importSession(newSession)
            secureTokenStore.saveRefreshToken(
                supabase.auth.currentSessionOrNull()?.refreshToken
            )
            val nowAuth = supabase.auth.currentSessionOrNull() != null
            CrashReporter.log(
                "refreshSessionIfNeeded: session restored from stored token; " +
                    "currentSession=$nowAuth"
            )
            if (!nowAuth) {
                CrashReporter.log(
                    "refreshSessionIfNeeded: import did not stick — forcing re-login"
                )
                secureTokenStore.clear()
                prefs.clearAll()
                CrashReporter.setUserId(null)
                return false
            }
            enqueueOfflineDrain()
            true
        } catch (e: Exception) {
            if (isNetworkError(e)) {
                CrashReporter.log(
                    "refreshSession: network error on cold start; keeping stored token"
                )
                return false
            }
            CrashReporter.log("refreshSession with stored token failed (auth)")
            CrashReporter.recordNonFatal(e)
            secureTokenStore.clear()
            prefs.clearAll()
            CrashReporter.setUserId(null)
            false
        }
    }
}
