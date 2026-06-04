package com.grace.app.presentation.components

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.worker.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State backing AppDrawer. Mirrors the subset of SettingsViewModel that the
 * drawer needs (profile fields + reminder times) so the drawer is fully
 * self-contained and doesn't depend on SettingsScreen being on-screen.
 */
data class AppDrawerUiState(
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val reminderHour: Int = 7,
    val prayerReminderHour: Int = 21
) {
    val isLeader: Boolean
        get() = role == "cell_leader" || role == "youth_president" ||
            role == "pastor" || role == "admin"
    val isAdmin: Boolean
        get() = role == "youth_president" || role == "admin"
}

@HiltViewModel
class AppDrawerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val prefs: UserPreferencesRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppDrawerUiState())
    val uiState: StateFlow<AppDrawerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.userName, prefs.userEmail, prefs.userRole,
                prefs.devoReminderHour, prefs.prayerReminderHour
            ) { values -> values.toList() }.collect { vals ->
                _uiState.update {
                    it.copy(
                        name = (vals[0] as? String) ?: "",
                        email = (vals[1] as? String) ?: "",
                        role = (vals[2] as? String) ?: "member",
                        reminderHour = (vals[3] as? Int) ?: 7,
                        prayerReminderHour = (vals[4] as? Int) ?: 21
                    )
                }
            }
        }
    }

    fun setDevoReminderHour(hour: Int) {
        viewModelScope.launch {
            val normalized = ((hour % 24) + 24) % 24
            prefs.setDevoReminderHour(normalized)
            ReminderScheduler.scheduleDevoReminder(
                appContext, normalized, ExistingPeriodicWorkPolicy.REPLACE
            )
        }
    }

    fun setPrayerReminderHour(hour: Int) {
        viewModelScope.launch {
            val normalized = ((hour % 24) + 24) % 24
            prefs.setPrayerReminderHour(normalized)
            ReminderScheduler.schedulePrayerReminder(
                appContext, normalized, ExistingPeriodicWorkPolicy.REPLACE
            )
        }
    }
}
