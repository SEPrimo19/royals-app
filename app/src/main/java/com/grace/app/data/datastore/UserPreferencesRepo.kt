package com.grace.app.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepo @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val userId: Flow<String?> = dataStore.data.map { it[UserPreferences.USER_ID] }
    val userName: Flow<String?> = dataStore.data.map { it[UserPreferences.USER_NAME] }
    val userEmail: Flow<String?> = dataStore.data.map { it[UserPreferences.USER_EMAIL] }
    val userRole: Flow<String?> = dataStore.data.map { it[UserPreferences.USER_ROLE] }
    val groupId: Flow<String?> = dataStore.data.map { it[UserPreferences.GROUP_ID] }
    val fcmToken: Flow<String?> = dataStore.data.map { it[UserPreferences.FCM_TOKEN] }

    val devoStreak: Flow<Int> = dataStore.data.map { it[UserPreferences.DEVO_STREAK] ?: 0 }
    val lastDevoDate: Flow<String?> =
        dataStore.data.map { it[UserPreferences.LAST_DEVO_DATE] }
    val devoReminderHour: Flow<Int> = dataStore.data.map {
        it[UserPreferences.DEVO_REMINDER_HOUR] ?: UserPreferences.DEFAULT_REMINDER_HOUR
    }
    val prayerReminderHour: Flow<Int> = dataStore.data.map {
        it[UserPreferences.PRAYER_REMINDER_HOUR]
            ?: UserPreferences.DEFAULT_PRAYER_REMINDER_HOUR
    }

    val notifPrayerEnabled: Flow<Boolean> =
        dataStore.data.map { it[UserPreferences.NOTIF_PRAYER_ENABLED] ?: true }
    val notifDevoEnabled: Flow<Boolean> =
        dataStore.data.map { it[UserPreferences.NOTIF_DEVO_ENABLED] ?: true }
    val notifMessagesEnabled: Flow<Boolean> =
        dataStore.data.map { it[UserPreferences.NOTIF_MESSAGES_ENABLED] ?: true }
    val notifCommunityEnabled: Flow<Boolean> =
        dataStore.data.map { it[UserPreferences.NOTIF_COMMUNITY_ENABLED] ?: true }

    val fontScale: Flow<Float> = dataStore.data.map {
        it[UserPreferences.FONT_SCALE] ?: UserPreferences.DEFAULT_FONT_SCALE
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map {
        ThemeMode.fromStored(it[UserPreferences.THEME_MODE])
    }

    suspend fun saveSession(
        userId: String,
        name: String,
        email: String,
        role: String,
        groupId: String?
    ) {
        dataStore.edit { p ->
            p[UserPreferences.USER_ID] = userId
            p[UserPreferences.USER_NAME] = name
            p[UserPreferences.USER_EMAIL] = email
            p[UserPreferences.USER_ROLE] = role
            if (groupId.isNullOrBlank()) {
                p.remove(UserPreferences.GROUP_ID)
            } else {
                p[UserPreferences.GROUP_ID] = groupId
            }
        }
    }

    suspend fun setRoleAndGroup(role: String, groupId: String) {
        dataStore.edit { p ->
            p[UserPreferences.USER_ROLE] = role
            p[UserPreferences.GROUP_ID] = groupId
        }
    }

    suspend fun setFcmToken(token: String) {
        dataStore.edit { it[UserPreferences.FCM_TOKEN] = token }
    }

    suspend fun setStreak(streak: Int) {
        dataStore.edit { it[UserPreferences.DEVO_STREAK] = streak }
    }

    suspend fun setLastDevoDate(date: String) {
        dataStore.edit { it[UserPreferences.LAST_DEVO_DATE] = date }
    }

    /**
     * Reconciles streak + lastDevoDate with the values stored on the server.
     *
     * Server wins when it's ahead or tied (covers fresh install + reinstall,
     * where DataStore is empty and the server has the real value). Local
     * wins when it's ahead (covers the case where the user completed a
     * devotional offline, bumping local streak, and the next online sync
     * hasn't yet pushed it back to the server — don't clobber that). This
     * is the rule that keeps your streak alive across uninstalls.
     */
    suspend fun reconcileStreakFromServer(serverStreak: Int, serverLastDevoIso: String?) {
        val local = devoStreak.first()
        if (serverStreak <= local && serverStreak > 0) return
        // Normalize to plain ISO date (yyyy-MM-dd). The server's TIMESTAMPTZ
        // comes back as "2026-05-28T00:00:00+00:00"; the streak comparison
        // logic in DevotionalRepositoryImpl expects the plain LocalDate
        // form — taking the first 10 chars handles both shapes.
        val normalized = serverLastDevoIso
            ?.takeIf { it.isNotBlank() }
            ?.let { if (it.length >= 10) it.substring(0, 10) else it }
        dataStore.edit { p ->
            p[UserPreferences.DEVO_STREAK] = serverStreak
            if (normalized != null) {
                p[UserPreferences.LAST_DEVO_DATE] = normalized
            }
        }
    }

    suspend fun setDevoReminderHour(hour: Int) {
        dataStore.edit { it[UserPreferences.DEVO_REMINDER_HOUR] = hour }
    }

    suspend fun setPrayerReminderHour(hour: Int) {
        dataStore.edit { it[UserPreferences.PRAYER_REMINDER_HOUR] = hour }
    }

    suspend fun setNotifPrayerEnabled(enabled: Boolean) {
        dataStore.edit { it[UserPreferences.NOTIF_PRAYER_ENABLED] = enabled }
    }

    suspend fun setNotifDevoEnabled(enabled: Boolean) {
        dataStore.edit { it[UserPreferences.NOTIF_DEVO_ENABLED] = enabled }
    }

    suspend fun setNotifMessagesEnabled(enabled: Boolean) {
        dataStore.edit { it[UserPreferences.NOTIF_MESSAGES_ENABLED] = enabled }
    }

    suspend fun setNotifCommunityEnabled(enabled: Boolean) {
        dataStore.edit { it[UserPreferences.NOTIF_COMMUNITY_ENABLED] = enabled }
    }

    suspend fun setFontScale(scale: Float) {
        // Clamp defensively — applying scale > 2x or < 0.5x produces
        // overlapping text and broken layouts.
        val safe = scale.coerceIn(0.85f, 1.5f)
        dataStore.edit { it[UserPreferences.FONT_SCALE] = safe }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[UserPreferences.THEME_MODE] = mode.name }
    }

    // Wipes the entire DataStore — used on sign-out / account deletion.
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }
}
