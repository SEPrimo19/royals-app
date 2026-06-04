package com.grace.app.data.repository

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.grace.app.data.datastore.SecureTokenStore
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.remote.supabase.dto.GroupDto
import com.grace.app.data.remote.supabase.dto.UserDto
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

    /**
     * Kick off a one-shot drain. Called after sign-in so a same-session
     * sign-out → sign-in flow doesn't leave queued offline posts orphaned
     * (NetworkMonitor only fires on connectivity TRANSITIONS, which never
     * happens during in-app account switching).
     */
    private fun enqueueOfflineDrain() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            OfflineSyncWorker.UNIQUE_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<OfflineSyncWorker>().build()
        )
    }

    private fun parseRole(raw: String?): UserRole = when (raw?.trim()?.lowercase()) {
        "cell_leader" -> UserRole.CELL_LEADER
        "youth_president" -> UserRole.YOUTH_PRESIDENT
        "pastor" -> UserRole.PASTOR
        "admin" -> UserRole.ADMIN
        else -> UserRole.MEMBER
    }

    // Maps raw Supabase/network failures to messages safe to show a youth user.
    // The "else" branches now include the raw status + message — keeps the
    // UI honest about unmapped errors instead of swallowing them. Trim long
    // messages so the screen doesn't overflow.
    private fun friendlyError(e: Throwable): String = when (e) {
        is RestException -> when (e.statusCode) {
            400 -> "Invalid email or password."
            401 -> "Invalid email or password."
            404 -> "Account not found."
            422 -> {
                val raw = e.message.orEmpty()
                // Some 422 cases are "already registered"; others are password
                // strength / weak-password rejections. Distinguish on message.
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
        // Pull streak + last-devo-date back from the server so a fresh
        // install doesn't reset the user's progress to 0. The server is
        // the authoritative copy when local has nothing yet.
        prefs.reconcileStreakFromServer(
            serverStreak = user.streak,
            serverLastDevoIso = user.lastDevoAt?.toString()
        )
        secureTokenStore.saveRefreshToken(supabase.auth.currentSessionOrNull()?.refreshToken)
        // Tag every future crash report with this user's id so we can
        // correlate dashboard entries back to a specific account.
        CrashReporter.setUserId(user.id)
        CrashReporter.setKey("role", user.role.name.lowercase())
        // Drain any prayers queued offline by THIS user from a prior
        // session — without this trigger the queue sits forever, since
        // NetworkMonitor only reacts to offline→online flips.
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
                // Phase P.5 (claim flow) safety: a leader may have pre-
                // registered this person as a proxy with this same email.
                // The proxy row holds the email under the UNIQUE constraint,
                // so a plain INSERT would collide.
                //
                // RLS hides other users' rows from the just-signed-up user,
                // so a normal SELECT returns nothing. Use the SECURITY DEFINER
                // RPC find_proxy_by_email that bypasses RLS for this check
                // — only returns minimal data (id + name + is_compassion).
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
                    // No proxy collision — safe to insert the public.users
                    // row normally. (If somehow an existing real-account row
                    // exists with this email, the INSERT will 23505/409 and
                    // the catch block surfaces the friendly error below.)
                    supabase.from("users").insert(
                        UserDto(id = uid, email = email, name = name, role = "member")
                    )
                }
                // For proxy-collision case we skip the INSERT — ClaimRecord
                // will populate the public.users row when the user claims.
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
        // Triggers the Chrome Custom Tab. The SDK's Android platform helper
        // uses Application context + FLAG_ACTIVITY_NEW_TASK, so this is safe
        // to call from a coroutine without an Activity reference.
        // Session is set later via MainActivity.handleDeeplinks(intent).
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
        // Drop the identity so any subsequent crash isn't attributed to
        // the now-signed-out user.
        CrashReporter.setUserId(null)
        Result.Success(Unit)
    } catch (e: Exception) {
        // Even if the remote sign-out fails, clear local state so the user is out.
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
                role = parseRole(role),
                groupId = groupId,
                streak = 0,
                lastDevoAt = null,
                fcmToken = null
            )
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        // The Edge Function performs the privileged delete (public.users +
        // auth.users) with service role. If it fails (network down, server
        // error, deploy missing), DO NOT proceed to clear local state —
        // otherwise the user thinks their data is gone but it's still on
        // the server. That's a privacy bug + Play Store compliance
        // violation. Surface the failure so they can retry.
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
        groupId: String,
        isCompassion: Boolean,
        compassionNumber: String?,
        emergencyContact: String?,
        birthdate: java.time.LocalDate?,
        sex: String?
    ): Result<Unit> = try {
        val uid = supabase.auth.currentUserOrNull()?.id
            ?: return Result.Error("Your session expired. Please sign in again.")
        // Single update so role/group/compassion/birthdate/sex land atomically.
        // Compassion number is set only when isCompassion=true; nulled
        // otherwise so a member who toggles off doesn't keep a stale ID.
        // birthdate + sex are nullable for non-Compassion users.
        supabase.from("users").update(
            {
                set("role", role.name.lowercase())
                set("group_id", groupId)
                set("is_compassion", isCompassion)
                set("compassion_number",
                    if (isCompassion) compassionNumber else null)
                set("emergency_contact",
                    emergencyContact?.trim()?.takeIf { it.isNotEmpty() })
                // birthdate stored as ISO yyyy-MM-dd to match the DB DATE column.
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
            ?: return Result.Success(Unit) // signed out — nothing to sync
        val dto = supabase.from("users")
            .select { filter { eq("id", uid) } }
            .decodeSingleOrNull<UserDto>()
            ?: return Result.Success(Unit) // row missing; leave cached state alone
        prefs.saveSession(
            userId = dto.id,
            name = dto.name,
            // Real signed-in users always have a non-null email; the field
            // is only nullable to support proxy-only members who can't sign
            // in. Fall back to empty for type-safety.
            email = dto.email.orEmpty(),
            role = dto.role,
            groupId = dto.groupId
        )
        // Same streak reconciliation as signIn — covers the cold-start
        // session-restore path where the user never explicitly signs in
        // but DataStore was wiped (uninstall + reinstall + auto-restore
        // via stored refresh token).
        prefs.reconcileStreakFromServer(
            serverStreak = dto.streak,
            serverLastDevoIso = dto.lastDevoAt
        )
        Result.Success(Unit)
    } catch (_: Exception) {
        // Best-effort background sync. Stale DataStore is acceptable.
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
            // Same null-on-toggle-off discipline as completeProfile so a member
            // who turns off Compassion doesn't leave a stale ID behind.
            set("is_compassion", isCompassion)
            set("compassion_number",
                if (isCompassion) compassionNumber else null)
            set("emergency_contact",
                emergencyContact?.trim()?.takeIf { it.isNotEmpty() })
            // birthdate + sex — accepted on every edit, including for
            // non-Compassion users (backfill the existing population).
            set("birthdate", birthdate?.toString())
            set("sex", sex?.takeIf { it == "M" || it == "F" })
        }) {
            filter { eq("id", uid) }
        }
        // Mirror name into DataStore so Settings/header reflect it immediately.
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
        // Supabase modifies the authenticated user in place. Server enforces
        // password rules (length, complexity) and returns 422 if violated.
        supabase.auth.modifyUser { password = newPassword }
        Result.Success(Unit)
    } catch (e: Exception) {
        CrashReporter.recordNonFatal(e)
        Result.Error(friendlyError(e), e)
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = try {
        // Supabase Auth ships its own email path for password reset —
        // separate from our Resend setup (which is sandbox-locked for
        // bulk + welcome emails). This works for any email address.
        // The user receives a one-time link that opens Supabase's
        // hosted "Set a new password" page in their browser.
        //
        // Account-enumeration defense: if [email] isn't registered, the
        // SDK still returns success so an attacker can't probe which
        // emails have accounts. The UI always shows "Check your email."
        supabase.auth.resetPasswordForEmail(email.trim())
        Result.Success(Unit)
    } catch (e: Exception) {
        CrashReporter.recordNonFatal(e)
        Result.Error(friendlyError(e), e)
    }

    override suspend fun isEmailPasswordUser(): Boolean = try {
        // Each Supabase identity carries a provider ("email" for password
        // sign-up, "google" for OAuth). Users can have BOTH if they linked
        // accounts, so any "email" identity counts as eligible for Change
        // Password — even a Google-first user who later set a password.
        val info = supabase.auth.currentUserOrNull()
        info?.identities?.any { it.provider == "email" } ?: false
    } catch (_: Exception) { false }

    override suspend fun findClaimableProxy(): Result<User?> = try {
        val info = supabase.auth.currentUserOrNull()
            ?: return Result.Error("Not signed in.")
        val myEmail = info.email?.trim()?.lowercase()
            ?: return Result.Success(null)   // OAuth users without email — skip

        // RLS hides other users' rows from the just-signed-up user, so we
        // can't SELECT proxy rows directly. Use the SECURITY DEFINER RPC
        // (added by feature-leader-proxy-claim.sql) which bypasses RLS for
        // a controlled email lookup and returns minimal data only.
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
                // Synthesize a minimal User from the RPC's reply — that's
                // all the ClaimRecord screen needs to render the prompt.
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
        // Calls the public.claim_proxy_record SQL function — see
        // supabase/feature-leader-proxy-claim.sql for the contract. The
        // function self-validates the email match server-side; we just
        // post the proxy_id.
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
        // Re-sync the now-merged profile into DataStore so role/group/etc
        // reflect the inherited proxy data.
        runCatching { syncProfileFromServer() }
        Result.Success(Unit)
    } catch (e: Exception) {
        CrashReporter.recordNonFatal(e)
        Result.Error(friendlyError(e), e)
    }

    /**
     * Walks the cause chain looking for an IOException. Returns true if
     * the failure is rooted in a network problem (no connectivity, DNS
     * fail, connection reset, timeout) rather than an auth problem
     * (HTTP 401/403, malformed token, etc.).
     *
     * Critical for offline UX: when a cold start happens with no
     * connectivity, supabase-kt's refresh call throws an IOException.
     * Without this distinction we'd wipe a perfectly valid session +
     * boot the user to Login on every offline cold start.
     */
    private fun isNetworkError(e: Throwable): Boolean {
        var cur: Throwable? = e
        while (cur != null) {
            if (cur is java.io.IOException) return true
            cur = cur.cause
        }
        return false
    }

    override suspend fun refreshSessionIfNeeded(): Boolean {
        // OFFLINE-FIRST GUARD: if we're not online, don't even attempt the
        // refresh. Refreshing requires a network round-trip; offline → it
        // would throw IOException (or some Ktor variant) → the old code
        // treated that as "token revoked" and wiped DataStore + the
        // stored refresh token, kicking the user to Login on every
        // offline cold start.
        //
        // Skipping the network call avoids the failure entirely:
        //   - Path A (in-memory session): keep it; return true.
        //   - Path B (cold start, stored token): leave the stored token
        //     intact; return false so MainActivity knows there's no
        //     ACTIVE session yet — but DataStore.userId is still set, so
        //     routing lands on MAIN_GRAPH and Room serves cached content.
        //   - The networkMonitor observer in MainActivity will retry
        //     refreshSessionIfNeeded() when connectivity flips back on.
        if (!networkMonitor.isOnline) {
            CrashReporter.log(
                "refreshSessionIfNeeded: offline; skipping refresh, keeping local state"
            )
            return supabase.auth.currentSessionOrNull() != null
        }

        // Path A — we already have a session in memory. Refresh the access
        // token to extend its validity.
        if (supabase.auth.currentSessionOrNull() != null) {
            return try {
                supabase.auth.refreshCurrentSession()
                secureTokenStore.saveRefreshToken(
                    supabase.auth.currentSessionOrNull()?.refreshToken
                )
                true
            } catch (e: Exception) {
                // OFFLINE-FIRST: a network error here doesn't mean the
                // session is invalid — the user is just offline. Keep
                // the in-memory session alive so they can use cached
                // content. The next online refresh attempt will succeed.
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
        // Path B — cold start (process was killed / app restarted). No
        // in-memory session, but if we have a stored refresh token from a
        // previous sign-in, swap it for a fresh session. WITHOUT this,
        // DataStore says we're signed in but the Supabase client stays
        // anonymous — every RLS-gated read silently returns []. This is
        // the events-empty-after-reinstall bug.
        val storedRefresh = secureTokenStore.refreshToken()
        if (storedRefresh.isNullOrBlank()) {
            // No stored token. If DataStore still claims the user is signed
            // in (auto-backup restored DataStore but the keystore self-heal
            // cleared SecureTokenStore), force a clean re-login. Otherwise
            // the app sits on Main graph as 'anonymous' and every RLS query
            // silently returns []. The user thinks the app is broken when
            // really they just need to sign in again.
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
            // refreshSession() returns the new UserSession but does NOT
            // always auto-set it as the current session in supabase-kt 2.5.4
            // — depends on the install. Explicitly importSession() so the
            // Auth state machine flips to Authenticated and subsequent
            // RLS-gated queries actually carry the JWT. Without this the
            // call appears to succeed but every fetch silently goes out
            // anonymous and returns [] — the "log out + sign in to fix"
            // symptom users were hitting.
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
                // importSession silently failed somehow. Clear and force
                // re-login rather than let the user sit on Main with no
                // working session.
                CrashReporter.log(
                    "refreshSessionIfNeeded: import did not stick — forcing re-login"
                )
                secureTokenStore.clear()
                prefs.clearAll()
                CrashReporter.setUserId(null)
                return false
            }
            // Session restored from a stored refresh token — drain any
            // queued offline work now that we have a valid JWT.
            enqueueOfflineDrain()
            true
        } catch (e: Exception) {
            // OFFLINE-FIRST: network error on cold-start refresh = user
            // is just offline. Keep stored token + DataStore intact so
            // they land on MAIN_GRAPH with cached Room content. The
            // next online refresh attempt will succeed (the Supabase
            // client auto-refreshes on the first online API call too).
            // Without this, swiping the app from recents while offline
            // boots the user to Login — exactly the bug the offline-
            // first audit asked us to fix.
            if (isNetworkError(e)) {
                CrashReporter.log(
                    "refreshSession: network error on cold start; keeping stored token"
                )
                return false  // false = "session not currently active"
                              // BUT we kept the token + prefs, so
                              // MainActivity will route to MAIN_GRAPH
                              // because DataStore.userId is still set.
            }
            // Refresh token actually expired or revoked — force a clean
            // re-login so the user sees a working Login screen instead
            // of an empty Home/Events.
            CrashReporter.log("refreshSession with stored token failed (auth)")
            CrashReporter.recordNonFatal(e)
            secureTokenStore.clear()
            prefs.clearAll()
            CrashReporter.setUserId(null)
            false
        }
    }
}
