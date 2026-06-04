package com.grace.app.presentation.screens.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.AttendanceStatus
import com.grace.app.domain.model.Event
import com.grace.app.domain.usecase.events.CheckInToEventUseCase
import com.grace.app.domain.usecase.events.GetEventByIdUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventCheckInUiState(
    val event: Event? = null,
    val isLoading: Boolean = true,
    val isCheckingIn: Boolean = false,
    val isDone: Boolean = false,
    // Filled when isDone == true — server-computed.
    val resultStatus: AttendanceStatus? = null,
    val resultLateMinutes: Int = 0,
    val error: String? = null
)

@HiltViewModel
class EventCheckInViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val checkInToEventUseCase: CheckInToEventUseCase
) : ViewModel() {

    private val eventId: String = savedStateHandle.get<String>("eventId").orEmpty()

    private val _uiState = MutableStateFlow(EventCheckInUiState())
    val uiState: StateFlow<EventCheckInUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val r = getEventByIdUseCase(eventId)) {
                is Result.Success -> _uiState.update {
                    it.copy(event = r.data, isLoading = false)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = r.message)
                }
                Result.Loading -> Unit
            }
        }
    }

    fun confirm() {
        if (_uiState.value.isCheckingIn) return
        _uiState.update { it.copy(isCheckingIn = true, error = null) }
        viewModelScope.launch {
            when (val r = checkInToEventUseCase(eventId)) {
                is Result.Success ->
                    _uiState.update {
                        it.copy(
                            isCheckingIn = false,
                            isDone = true,
                            resultStatus = r.data.status,
                            resultLateMinutes = r.data.lateByMinutes
                        )
                    }
                is Result.Error ->
                    _uiState.update {
                        it.copy(isCheckingIn = false, error = r.message)
                    }
                Result.Loading -> Unit
            }
        }
    }
}
