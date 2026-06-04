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

    /**
     * Kicks off Google OAuth via Chrome Custom Tab. The actual session is
     * established when the deep-link callback fires — handled in MainActivity
     * via `supabase.handleDeeplinks(intent)`. Returns Success once the browser
     * has been launched; the UI should wait for `currentUser()` to emit a
     * non-null user.
     */
    suspend fun signInWithGoogle(): Result<Unit>

    // Profile setup support (kept in the repo so ViewModels stay Supabase-free).
    suspend fun getGroups(): Result<List<Group>>
    suspend fun completeProfile(
        role: UserRole,
        groupId: String,
        isCompassion: Boolean = false,
        compassionNumber: String? = null,
        emergencyContact: String? = null,
        // birthdate + sex were added to users by feature-leader-proxy.sql and
        // are required for accurate Compassion compliance reports — applies
        // to app users too, not just proxy-only members. Nullable here so
        // existing call sites that don't pass them still compile.
        birthdate: java.time.LocalDate? = null,
        sex: String? = null
    ): Result<Unit>

    /** Refreshes an expired session on launch. Returns true if a valid session remains. */
    suspend fun refreshSessionIfNeeded(): Boolean

    /**
     * Re-fetches the current user's row from Supabase and mirrors role/name/
     * group_id into DataStore. Lets server-side role changes (e.g. an admin
     * promoting a user) propagate without requiring a manual sign-out.
     * Best-effort: silently no-ops if signed out or offline.
     */
    suspend fun syncProfileFromServer(): Result<Unit>

    /** Fetches the current user's full profile (incl. bio / messenger fields). */
    suspend fun getMyProfile(): Result<User?>

    /** Updates the signed-in user's profile fields. RLS enforces self-only. */
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

    /**
     * Updates the signed-in user's password via Supabase auth.updateUser.
     * Only meaningful for users with email/password auth — Google OAuth
     * users should be guided to manage their password in Google itself.
     * The UI is expected to call [isEmailPasswordUser] first and hide
     * Change Password for users who only have a Google identity.
     */
    suspend fun changePassword(newPassword: String): Result<Unit>

    /**
     * Sends a password-reset email to [email] via Supabase Auth's built-in
     * SMTP. The recipient receives a one-time reset link that opens
     * Supabase's hosted "Set a new password" page in their browser.
     *
     * Soft-success even if the email isn't registered — we don't reveal
     * whether an account exists for that address (account-enumeration
     * defense). The user always sees "Check your email."
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    /**
     * True if the signed-in user has an email/password identity linked
     * to their Supabase account (vs. Google-only OAuth). Used to gate
     * the Change Password UI in Settings.
     */
    suspend fun isEmailPasswordUser(): Boolean

    /**
     * Phase P.5 (Leader Proxy Mode claim flow). Returns a proxy-only user
     * row whose email matches the currently-signed-in user, if one exists.
     * The UI then prompts the user with "We found your record in {cell} —
     * is this you?" before calling [claimProxyRecord]. Returns null when
     * there's no candidate (the common case for new signups).
     */
    suspend fun findClaimableProxy(): Result<User?>

    /**
     * Phase P.5 — merges the proxy record (attendance / meditation /
     * prayer history) into the signed-in user's account. Backed by the
     * `public.claim_proxy_record` SQL function which validates the email
     * match server-side and migrates all FKs in a transaction.
     */
    suspend fun claimProxyRecord(proxyUserId: String): Result<Unit>
}
