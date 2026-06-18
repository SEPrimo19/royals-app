package com.grace.app.presentation.screens.leader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.AttendanceStatus
import com.grace.app.domain.repository.LeaderRepository
import com.grace.app.domain.usecase.leader.AddProxyMemberUseCase
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
import java.time.LocalDate
import javax.inject.Inject

data class AddProxyMemberUiState(
    val name: String = "",
    val birthdate: LocalDate? = null,
    val sex: String = "",
    val isCompassion: Boolean = false,
    val compassionDigits: String = "",
    val emergencyContact: String = "",
    val email: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null,
    val attendForEventId: String? = null
) {
    val canSubmit: Boolean
        get() = name.trim().length >= 2 &&
            birthdate != null &&
            (sex == "M" || sex == "F") &&
            !isSubmitting &&
            (!isCompassion || compassionDigits.length == 4)
}

sealed interface AddProxyMemberEvent {
    data class NameChanged(val v: String) : AddProxyMemberEvent
    data class BirthdateChanged(val v: LocalDate) : AddProxyMemberEvent
    data class SexChanged(val v: String) : AddProxyMemberEvent
    data class CompassionToggled(val v: Boolean) : AddProxyMemberEvent
    data class CompassionDigitsChanged(val v: String) : AddProxyMemberEvent
    data class EmergencyChanged(val v: String) : AddProxyMemberEvent
    data class EmailChanged(val v: String) : AddProxyMemberEvent
    data object Submit : AddProxyMemberEvent
    data object SubmitAndMarkAttended : AddProxyMemberEvent
    data object DismissError : AddProxyMemberEvent
}

sealed interface AddProxyMemberEffect {
    data class MemberAdded(val newMemberId: String) : AddProxyMemberEffect
}

@HiltViewModel
class AddProxyMemberViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val addProxyMemberUseCase: AddProxyMemberUseCase,
    private val leaderRepository: LeaderRepository
) : ViewModel() {

    private val attendForEventId: String? =
        savedStateHandle.get<String>("eventId")
            ?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow(
        AddProxyMemberUiState(attendForEventId = attendForEventId)
    )
    val uiState: StateFlow<AddProxyMemberUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<AddProxyMemberEffect>()
    val effect: SharedFlow<AddProxyMemberEffect> = _effect.asSharedFlow()

    fun onEvent(event: AddProxyMemberEvent) {
        when (event) {
            is AddProxyMemberEvent.NameChanged ->
                _uiState.update { it.copy(name = event.v) }
            is AddProxyMemberEvent.BirthdateChanged ->
                _uiState.update { it.copy(birthdate = event.v) }
            is AddProxyMemberEvent.SexChanged ->
                _uiState.update { it.copy(sex = event.v) }
            is AddProxyMemberEvent.CompassionToggled ->
                _uiState.update {
                    if (event.v) it.copy(isCompassion = true)
                    else it.copy(isCompassion = false, compassionDigits = "")
                }
            is AddProxyMemberEvent.CompassionDigitsChanged ->
                _uiState.update {
                    it.copy(
                        compassionDigits = event.v
                            .filter { c -> c.isDigit() }
                            .take(4)
                    )
                }
            is AddProxyMemberEvent.EmergencyChanged ->
                _uiState.update { it.copy(emergencyContact = event.v) }
            is AddProxyMemberEvent.EmailChanged ->
                _uiState.update { it.copy(email = event.v) }
            AddProxyMemberEvent.DismissError ->
                _uiState.update { it.copy(error = null) }
            AddProxyMemberEvent.Submit -> submit(markAttended = false)
            AddProxyMemberEvent.SubmitAndMarkAttended ->
                submit(markAttended = true)
        }
    }

    private fun submit(markAttended: Boolean) {
        val s = _uiState.value
        if (!s.canSubmit) return
        val birthdate = s.birthdate ?: return
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val result = addProxyMemberUseCase(
                name = s.name,
                birthdate = birthdate,
                sex = s.sex,
                isCompassion = s.isCompassion,
                compassionNumber = if (s.isCompassion) "PH867-${s.compassionDigits}" else null,
                emergencyContact = s.emergencyContact,
                email = s.email
            )
            when (result) {
                is Result.Success -> {
                    val newId = result.data
                    val eventId = s.attendForEventId
                    if (markAttended && eventId != null) {
                        val r = leaderRepository.markProxyAttendance(
                            eventId = eventId,
                            memberId = newId,
                            status = AttendanceStatus.PRESENT
                        )
                        if (r is Result.Error) {
                            _uiState.update {
                                it.copy(
                                    isSubmitting = false,
                                    error = "Member added, but couldn't mark them " +
                                        "attended (${r.message}). Mark from the roster."
                                )
                            }
                            _effect.emit(AddProxyMemberEffect.MemberAdded(newId))
                            return@launch
                        }
                    }
                    _effect.emit(AddProxyMemberEffect.MemberAdded(newId))
                    _uiState.update { it.copy(isSubmitting = false) }
                }
                is Result.Error -> _uiState.update {
                    it.copy(isSubmitting = false, error = result.message)
                }
                else -> _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
