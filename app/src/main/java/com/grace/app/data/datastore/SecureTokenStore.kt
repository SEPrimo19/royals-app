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
        CrashReporter.log("SecureTokenStore: decrypt failed; resetting Keystore alias + prefs")
        CrashReporter.recordNonFatal(e)
        resetCryptoState(ctx)
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
