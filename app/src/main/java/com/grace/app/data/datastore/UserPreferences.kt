package com.grace.app.data.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

// All DataStore preference keys for GRACE in one place.
object UserPreferences {
    val USER_ID = stringPreferencesKey("user_id")
    val USER_NAME = stringPreferencesKey("user_name")
    val USER_EMAIL = stringPreferencesKey("user_email")
    val USER_ROLE = stringPreferencesKey("user_role")
    val GROUP_ID = stringPreferencesKey("group_id")
    val FCM_TOKEN = stringPreferencesKey("fcm_token")
    val DEVO_STREAK = intPreferencesKey("devo_streak")
    val LAST_DEVO_DATE = stringPreferencesKey("last_devo_date")
    val DEVO_REMINDER_HOUR = intPreferencesKey("devo_reminder_hour")
    val PRAYER_REMINDER_HOUR = intPreferencesKey("prayer_reminder_hour")
    val NOTIF_PRAYER_ENABLED = booleanPreferencesKey("notif_prayer_enabled")
    val NOTIF_DEVO_ENABLED = booleanPreferencesKey("notif_devo_enabled")
    val NOTIF_MESSAGES_ENABLED = booleanPreferencesKey("notif_messages_enabled")
    val NOTIF_COMMUNITY_ENABLED = booleanPreferencesKey("notif_community_enabled")

    /** Text-size override applied via CompositionLocalProvider(LocalDensity).
     *  1.0 = system default, 1.15 = Large, 1.30 = Largest. Stored separately
     *  from the OS-wide fontScale so the app can offer its own picker that
     *  works even for users who don't want to change device-wide settings. */
    val FONT_SCALE = floatPreferencesKey("font_scale")

    /** Theme selection. Stored as a string so adding future modes (Auto-Night,
     *  high-contrast, etc.) doesn't require a DataStore migration. Values:
     *  "DARK" | "LIGHT" | "SYSTEM" — see ThemeMode enum. */
    val THEME_MODE = stringPreferencesKey("theme_mode")

    const val DEFAULT_REMINDER_HOUR = 7
    const val DEFAULT_PRAYER_REMINDER_HOUR = 21 // 9pm — natural evening prayer slot
    const val DEFAULT_FONT_SCALE = 1.0f
    // Default on fresh install is LIGHT — Royals' Light palette is the
    // friendlier first-impression for new youth members; users who prefer
    // Dark can switch in Settings.
    const val DEFAULT_THEME_MODE = "LIGHT"
}

/**
 * User-selectable appearance mode. LIGHT is the default for new installs;
 * DARK is the historic Royals look (still available via Settings); SYSTEM
 * follows the device-wide setting (Android Settings → Display → Dark theme).
 */
enum class ThemeMode { DARK, LIGHT, SYSTEM;
    companion object {
        // Fallback when DataStore has no value: NEW users land on LIGHT.
        // Existing users who never explicitly chose a theme also get
        // moved to LIGHT on next launch — acceptable for the pre-launch
        // tester pool. The "DARK" branch handles round-tripping for
        // anyone who did pick Dark explicitly before this change.
        fun fromStored(value: String?): ThemeMode = when (value?.uppercase()) {
            "DARK" -> DARK
            "SYSTEM" -> SYSTEM
            else -> LIGHT
        }
    }
}
