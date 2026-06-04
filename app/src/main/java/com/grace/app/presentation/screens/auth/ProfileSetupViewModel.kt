package com.grace.app.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.Group
import com.grace.app.domain.model.UserRole
import com.grace.app.domain.repository.AuthRepository
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileSetupUiState(
    val selectedRole: UserRole? = null,
    val availableGroups: List<Group> = emptyList(),
    val selectedGroupId: String? = null,
    // Compassion participant fields. compassionDigits is just the 4-digit
    // suffix the user types in — the PH867- prefix is fixed and added when
    // we save to the server.
    val isCompassion: Boolean = false,
    val compassionDigits: String = "",
    val emergencyContact: String = "",
    // Birthdate + sex required for accurate Compassion compliance reports.
    // Optional for non-Compassion users (still helpful for demographics).
    val birthdate: java.time.LocalDate? = null,
    val sex: String = "",   // "" until picked; "M" or "F" thereafter
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val error: String? = null
) {
    /**
     * Compassion participants must have exactly 4 digits + birthdate + sex.
     * Non-Compassion members skip those requirements. Emergency contact is
     * always optional.
     */
    val canSubmit: Boolean
        get() = selectedRole != null &&
            !selectedGroupId.isNullOrBlank() &&
            (!isCompassion || (
                compassionDigits.length == 4 &&
                    birthdate != null &&
                    (sex == "M" || sex == "F")
            ))

    /** Full Compassion ID the server stores, when applicable. */
    val composedCompassionNumber: String?
        get() = if (isCompassion && compassionDigits.length == 4)
            "PH867-$compassionDigits" else null
}

sealed interface ProfileSetupEvent {
    data class RoleSelected(val role: UserRole) : ProfileSetupEvent
    data class GroupSelected(val groupId: String) : ProfileSetupEvent
    data class CompassionToggled(val on: Boolean) : ProfileSetupEvent
    data class CompassionDigitsChanged(val digits: String) : ProfileSetupEvent
    data class EmergencyContactChanged(val value: String) : ProfileSetupEvent
    data class BirthdateChanged(val value: java.time.LocalDate) : ProfileSetupEvent
    data class SexChanged(val value: String) : ProfileSetupEvent
    data object CompleteSetup : ProfileSetupEvent
}

sealed interface ProfileSetupEffect {
    data object NavigateToHome : ProfileSetupEffect
    data class ShowError(val message: String) : ProfileSetupEffect
}

// AuthRepository is injected directly: getGroups/completeProfile are thin
// passthroughs with no business rule, so a dedicated use case would add no value.
@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileSetupEffect>()
    val effect: SharedFlow<ProfileSetupEffect> = _effect.asSharedFlow()

    init {
        loadGroups()
    }

    private fun loadGroups() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.getGroups()) {
                is Result.Success ->
                    _uiState.update { it.copy(isLoading = false, availableGroups = result.data) }
                is Result.Error ->
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun onEvent(event: ProfileSetupEvent) {
        when (event) {
            is ProfileSetupEvent.RoleSelected ->
                _uiState.update { it.copy(selectedRole = event.role) }
            is ProfileSetupEvent.GroupSelected ->
                _uiState.update { it.copy(selectedGroupId = event.groupId) }
            is ProfileSetupEvent.CompassionToggled ->
                _uiState.update {
                    // Clearing digits on toggle-off prevents stale data
                    // surviving a "yes → no → yes" flip.
                    it.copy(
                        isCompassion = event.on,
                        compassionDigits = if (event.on) it.compassionDigits else ""
                    )
                }
            is ProfileSetupEvent.CompassionDigitsChanged ->
                _uiState.update {
                    // Strip non-digits and cap at 4. The IME's number keyboard
                    // helps, but paste / autofill can still slip non-digits in.
                    it.copy(
                        compassionDigits = event.digits
                            .filter { c -> c.isDigit() }
                            .take(4)
                    )
                }
            is ProfileSetupEvent.EmergencyContactChanged ->
                _uiState.update { it.copy(emergencyContact = event.value) }
            is ProfileSetupEvent.BirthdateChanged ->
                _uiState.update { it.copy(birthdate = event.value) }
            is ProfileSetupEvent.SexChanged ->
                _uiState.update { it.copy(sex = event.value) }
            ProfileSetupEvent.CompleteSetup -> complete()
        }
    }

    private fun complete() {
        val s = _uiState.value
        if (!s.canSubmit || s.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = authRepository.completeProfile(
                role = s.selectedRole!!,
                groupId = s.selectedGroupId!!,
                isCompassion = s.isCompassion,
                compassionNumber = s.composedCompassionNumber,
                emergencyContact = s.emergencyContact.trim().takeIf { it.isNotEmpty() },
                birthdate = s.birthdate,
                sex = s.sex.takeIf { it == "M" || it == "F" }
            )
            when (result) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _effect.emit(ProfileSetupEffect.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSubmitting = false, error = result.message) }
                    _effect.emit(ProfileSetupEffect.ShowError(result.message))
                }
                Result.Loading -> Unit
            }
        }
    }
}
