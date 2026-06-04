package com.grace.app.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.usecase.profile.GetMyProfileUseCase
import com.grace.app.domain.usecase.profile.UpdateMyProfileUseCase
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

data class EditProfileUiState(
    val name: String = "",
    val email: String = "",            // read-only display
    val bio: String = "",
    val messengerUrl: String = "",
    val messengerPublic: Boolean = false,
    // Compassion participant fields. compassionDigits is the 4-digit suffix
    // only; the PH867- prefix is composed in the use case.
    val isCompassion: Boolean = false,
    val compassionDigits: String = "",
    val emergencyContact: String = "",
    // Birthdate + sex — same fields collected on AddProxyMember/ProfileSetup
    // so app users and proxy-only members share the same data model.
    val birthdate: java.time.LocalDate? = null,
    val sex: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null
) {
    /**
     * Save enabled when: name is valid AND (not Compassion OR full set is
     * provided). Compassion users must have 4 digits + birthdate + sex —
     * matches the AddProxyMember + ProfileSetup contract.
     */
    val canSave: Boolean
        get() = name.trim().length >= 2 &&
            (!isCompassion || (
                compassionDigits.length == 4 &&
                    birthdate != null &&
                    (sex == "M" || sex == "F")
            )) &&
            !isSaving
}

sealed interface EditProfileEvent {
    data class NameChanged(val value: String) : EditProfileEvent
    data class BioChanged(val value: String) : EditProfileEvent
    data class MessengerChanged(val value: String) : EditProfileEvent
    data class MessengerPublicChanged(val value: Boolean) : EditProfileEvent
    data class CompassionToggled(val on: Boolean) : EditProfileEvent
    data class CompassionDigitsChanged(val digits: String) : EditProfileEvent
    data class EmergencyContactChanged(val value: String) : EditProfileEvent
    data class BirthdateChanged(val value: java.time.LocalDate) : EditProfileEvent
    data class SexChanged(val value: String) : EditProfileEvent
    data object Save : EditProfileEvent
    data object DismissError : EditProfileEvent
}

sealed interface EditProfileEffect {
    data object Saved : EditProfileEffect
    data class ShowError(val message: String) : EditProfileEffect
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val updateMyProfileUseCase: UpdateMyProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<EditProfileEffect>()
    val effect: SharedFlow<EditProfileEffect> = _effect.asSharedFlow()

    init { load() }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = getMyProfileUseCase()) {
                is Result.Success -> {
                    val u = r.data
                    // Split the server's full PH867-XXXX back into the 4-digit
                    // suffix the UI edits. removePrefix is safe even if the
                    // value is missing or doesn't match — returns the whole
                    // string, which is then trimmed to 4 digits.
                    val savedDigits = (u?.compassionNumber ?: "")
                        .removePrefix("PH867-")
                        .filter { it.isDigit() }
                        .take(4)
                    _uiState.update {
                        it.copy(
                            name = u?.name.orEmpty(),
                            email = u?.email.orEmpty(),
                            bio = u?.bio.orEmpty(),
                            messengerUrl = u?.messengerUrl.orEmpty(),
                            messengerPublic = u?.messengerPublic ?: false,
                            isCompassion = u?.isCompassion ?: false,
                            compassionDigits = savedDigits,
                            emergencyContact = u?.emergencyContact.orEmpty(),
                            birthdate = u?.birthdate,
                            sex = u?.sex.orEmpty(),
                            isLoading = false
                        )
                    }
                }
                is Result.Error ->
                    _uiState.update { it.copy(isLoading = false, error = r.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.NameChanged ->
                _uiState.update { it.copy(name = event.value) }
            is EditProfileEvent.BioChanged ->
                // Cap at 280 chars in UI so the validator doesn't have to.
                _uiState.update { it.copy(bio = event.value.take(280)) }
            is EditProfileEvent.MessengerChanged ->
                _uiState.update { it.copy(messengerUrl = event.value) }
            is EditProfileEvent.MessengerPublicChanged ->
                _uiState.update { it.copy(messengerPublic = event.value) }
            is EditProfileEvent.CompassionToggled ->
                _uiState.update {
                    // Same toggle-off discipline as signup — clear digits so
                    // a re-toggle starts clean and we don't accidentally save
                    // a stale partial ID.
                    it.copy(
                        isCompassion = event.on,
                        compassionDigits = if (event.on) it.compassionDigits else ""
                    )
                }
            is EditProfileEvent.CompassionDigitsChanged ->
                _uiState.update {
                    it.copy(
                        compassionDigits = event.digits
                            .filter { c -> c.isDigit() }
                            .take(4)
                    )
                }
            is EditProfileEvent.EmergencyContactChanged ->
                _uiState.update { it.copy(emergencyContact = event.value) }
            is EditProfileEvent.BirthdateChanged ->
                _uiState.update { it.copy(birthdate = event.value) }
            is EditProfileEvent.SexChanged ->
                _uiState.update { it.copy(sex = event.value) }
            EditProfileEvent.Save -> save()
            EditProfileEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun save() {
        val snap = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val r = updateMyProfileUseCase(
                name = snap.name,
                bio = snap.bio.ifBlank { null },
                messengerUrl = snap.messengerUrl.ifBlank { null },
                messengerPublic = snap.messengerPublic,
                isCompassion = snap.isCompassion,
                compassionDigits = snap.compassionDigits,
                emergencyContact = snap.emergencyContact.ifBlank { null },
                birthdate = snap.birthdate,
                sex = snap.sex.takeIf { it == "M" || it == "F" }
            )
            _uiState.update { it.copy(isSaving = false) }
            when (r) {
                is Result.Success -> _effect.emit(EditProfileEffect.Saved)
                is Result.Error -> _effect.emit(EditProfileEffect.ShowError(r.message))
                Result.Loading -> Unit
            }
        }
    }
}
