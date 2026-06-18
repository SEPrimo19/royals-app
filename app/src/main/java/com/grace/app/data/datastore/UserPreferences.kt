package com.grace.app.data.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

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
    val BIBLE_GAMES_REMINDER_HOUR = intPreferencesKey("bible_games_reminder_hour")
    val NOTIF_PRAYER_ENABLED = booleanPreferencesKey("notif_prayer_enabled")
    val NOTIF_DEVO_ENABLED = booleanPreferencesKey("notif_devo_enabled")
    val NOTIF_MESSAGES_ENABLED = booleanPreferencesKey("notif_messages_enabled")
    val NOTIF_COMMUNITY_ENABLED = booleanPreferencesKey("notif_community_enabled")
    val NOTIF_BIBLE_GAMES_ENABLED = booleanPreferencesKey("notif_bible_games_enabled")

    val FONT_SCALE = floatPreferencesKey("font_scale")

    val THEME_MODE = stringPreferencesKey("theme_mode")

    val DISMISSED_UPDATE_FOR_VERSION = intPreferencesKey("dismissed_update_for_version")

    val LAST_BIBLE_GAMES_PLAYED_DATE = stringPreferencesKey("last_bible_games_played_date")

    val BIBLE_LAST_BOOK = intPreferencesKey("bible_last_book")
    val BIBLE_LAST_CHAPTER = intPreferencesKey("bible_last_chapter")

    const val DEFAULT_REMINDER_HOUR = 7
    const val DEFAULT_PRAYER_REMINDER_HOUR = 21
    const val DEFAULT_BIBLE_GAMES_REMINDER_HOUR = 19
    const val DEFAULT_FONT_SCALE = 1.0f
    const val DEFAULT_THEME_MODE = "LIGHT"
}

enum class ThemeMode { DARK, LIGHT, SYSTEM;
    companion object {
        fun fromStored(value: String?): ThemeMode = when (value?.uppercase()) {
            "DARK" -> DARK
            "SYSTEM" -> SYSTEM
            else -> LIGHT
        }
    }
}
