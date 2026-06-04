package com.grace.app.data.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.grace.app.data.util.CrashReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Supabase JWT refresh token is sensitive and must never sit in plain
 * DataStore. It is stored here in EncryptedSharedPreferences, whose master key
 * is held in the Android Keystore (AES-256-GCM) and never leaves the device.
 *
 * **Self-healing decrypt:** If the encrypted prefs file and the Keystore
 * master-key alias get out of sync (e.g. uninstall+reinstall, device backup/
 * restore, OEM ROM behavior), the next read throws AEADBadTagException
 * inside [EncryptedSharedPreferences.create] and the whole app crashes on
 * launch. [buildPrefs] catches that, wipes both halves, and rebuilds — the
 * worst case is the user has to sign in once more. Without this, the only
 * recovery would be clearing app data in system Settings.
 */
@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy { buildPrefs(context) }

    fun saveRefreshToken(token: String?) {
        prefs.edit().apply {
            if (token.isNullOrBlank()) remove(KEY_REFRESH) else putString(KEY_REFRESH, token)
        }.apply()
    }

    fun refreshToken(): String? = prefs.getString(KEY_REFRESH, null)

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun buildPrefs(ctx: Context): SharedPreferences = try {
        createEncrypted(ctx)
    } catch (e: Throwable) {
        // The Keystore <-> encrypted-prefs handshake is in a bad state.
        // Surface it in Crashlytics so we can spot trends, then reset.
        CrashReporter.log("SecureTokenStore: decrypt failed; resetting Keystore alias + prefs")
        CrashReporter.recordNonFatal(e)
        resetCryptoState(ctx)
        // Single retry — if this still throws, the Throwable propagates
        // and Crashlytics captures the real underlying problem. We don't
        // want to mask repeated failures with an infinite loop.
        createEncrypted(ctx)
    }

    private fun createEncrypted(ctx: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            ctx,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Wipe both halves of the broken state — the prefs file (encrypted
     * keyset is in there) and the Keystore master-key alias. After this
     * the next createEncrypted() call gets a fresh key + fresh keyset.
     * Each step is independently wrapped so one failure doesn't block
     * the other.
     */
    private fun resetCryptoState(ctx: Context) {
        runCatching { ctx.deleteSharedPreferences(PREFS_NAME) }
        runCatching {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            if (ks.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                ks.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            }
        }
    }

    private companion object {
        const val KEY_REFRESH = "supabase_refresh_token"
        const val PREFS_NAME = "grace_secure_prefs"
    }
}
