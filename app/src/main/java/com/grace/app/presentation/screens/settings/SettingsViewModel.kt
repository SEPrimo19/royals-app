package com.grace.app.presentation.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.data.datastore.ThemeMode
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.domain.repository.AuthRepository
import com.grace.app.domain.usecase.auth.SignOutUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Slimmed Settings now that the kitchen-sink links (My Content / Reminders /
 * Admin / Community shortcuts) have moved into the app-level burger drawer.
 * This screen keeps ONLY account-management concerns: profile, password,
 * notification preferences, privacy + delete account, sign out, about.
 */
data class SettingsUiState(
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val notifPrayer: Boolean = true,
    val notifDevo: Boolean = true,
    val notifMessages: Boolean = true,
    val notifCommunity: Boolean = true,
    val fontScale: Float = 1.0f,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val canChangePassword: Boolean = false,
    val isWorking: Boolean = false,
    val passwordChangeError: String? = null,
    val passwordChangeSuccess: Boolean = false,
    // Surfaced when the privileged delete-account Edge Function call fails.
    // Settings screen should render this as a small rose toast/banner.
    val deleteAccountError: String? = null
)

sealed interface SettingsEvent {
    data class ToggleNotif(val channel: String, val enabled: Boolean) : SettingsEvent
    data class SetFontScale(val scale: Float) : SettingsEvent
    data class SetThemeMode(val mode: ThemeMode) : SettingsEvent
    data class ChangePassword(val newPassword: String) : SettingsEvent
    data object DismissPasswordResult : SettingsEvent
    data object DismissDeleteAccountError : SettingsEvent
    data object SignOut : SettingsEvent
    data object DeleteAccount : SettingsEvent
}

sealed interface SettingsEffect {
    data object NavigateToLogin : SettingsEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val prefs: UserPreferencesRepo,
    private val signOutUseCase: SignOutUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<SettingsEffect>()
    val effect: SharedFlow<SettingsEffect> = _effect.asSharedFlow()

    val appVersion: String = com.grace.app.BuildConfig.VERSION_NAME

    init {
        viewModelScope.launch {
            combine(
                prefs.userName, prefs.userEmail, prefs.userRole
            ) { name, email, role -> Triple(name, email, role) }
                .collect { (name, email, role) ->
                    _uiState.update {
                        it.copy(
                            name = name ?: "",
                            email = email ?: "",
                            role = role ?: "member"
                        )
                    }
                }
        }
        viewModelScope.launch {
            combine(
                prefs.notifPrayerEnabled, prefs.notifDevoEnabled,
                prefs.notifMessagesEnabled, prefs.notifCommunityEnabled
            ) { p, d, m, c -> listOf(p, d, m, c) }.collect { v ->
                _uiState.update {
                    it.copy(
                        notifPrayer = v[0], notifDevo = v[1],
                        notifMessages = v[2], notifCommunity = v[3]
                    )
                }
            }
        }
        viewModelScope.launch {
            // Check once on screen entry — auth identity rarely changes
            // mid-session, and a stale "false" would just hide the link
            // until next app launch which is acceptable.
            val canChange = authRepository.isEmailPasswordUser()
            _uiState.update { it.copy(canChangePassword = canChange) }
        }
        viewModelScope.launch {
            prefs.fontScale.collect { v ->
                _uiState.update { it.copy(fontScale = v) }
            }
        }
        viewModelScope.launch {
            prefs.themeMode.collect { v ->
                _uiState.update { it.copy(themeMode = v) }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleNotif -> viewModelScope.launch {
                when (event.channel) {
                    "prayer" -> prefs.setNotifPrayerEnabled(event.enabled)
                    "devo" -> prefs.setNotifDevoEnabled(event.enabled)
                    "messages" -> prefs.setNotifMessagesEnabled(event.enabled)
                    "community" -> prefs.setNotifCommunityEnabled(event.enabled)
                }
            }
            is SettingsEvent.SetFontScale -> viewModelScope.launch {
                prefs.setFontScale(event.scale)
            }
            is SettingsEvent.SetThemeMode -> viewModelScope.launch {
                prefs.setThemeMode(event.mode)
            }
            is SettingsEvent.ChangePassword -> viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isWorking = true,
                        passwordChangeError = null,
                        passwordChangeSuccess = false
                    )
                }
                when (val r = authRepository.changePassword(event.newPassword)) {
                    is Result.Success -> _uiState.update {
                        it.copy(isWorking = false, passwordChangeSuccess = true)
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(isWorking = false, passwordChangeError = r.message)
                    }
                    else -> _uiState.update { it.copy(isWorking = false) }
                }
            }
            SettingsEvent.DismissPasswordResult -> _uiState.update {
                it.copy(passwordChangeError = null, passwordChangeSuccess = false)
            }
            SettingsEvent.DismissDeleteAccountError -> _uiState.update {
                it.copy(deleteAccountError = null)
            }
            SettingsEvent.SignOut -> viewModelScope.launch {
                _uiState.update { it.copy(isWorking = true) }
                signOutUseCase()
                _effect.emit(SettingsEffect.NavigateToLogin)
            }
            SettingsEvent.DeleteAccount -> viewModelScope.launch {
                _uiState.update { it.copy(isWorking = true) }
                // Phase B: deleteAccount now returns Result.Error on failure
                // (e.g. Edge Function not deployed, network down) instead of
                // silently pretending it worked. Surface the error so the
                // user can retry rather than incorrectly believing their
                // data is gone.
                when (val r = authRepository.deleteAccount()) {
                    is Result.Success -> {
                        _effect.emit(SettingsEffect.NavigateToLogin)
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(isWorking = false, deleteAccountError = r.message)
                        }
                    }
                    Result.Loading -> Unit
                }
            }
        }
    }
}
