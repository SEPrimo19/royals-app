package com.grace.app.presentation.screens.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.Attendee
import com.grace.app.domain.model.Event
import com.grace.app.domain.usecase.events.GetEventAttendeesUseCase
import com.grace.app.domain.usecase.events.GetEventByIdUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventQrUiState(
    val event: Event? = null,
    val attendees: List<Attendee> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingAttendees: Boolean = false,
    val error: String? = null
) {
    val qrPayload: String?
        get() = event?.id?.let { "grace://event-checkin/$it" }
}

@HiltViewModel
class EventQrViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val getEventAttendeesUseCase: GetEventAttendeesUseCase
) : ViewModel() {

    private val eventId: String = savedStateHandle.get<String>("eventId").orEmpty()

    private val _uiState = MutableStateFlow(EventQrUiState())
    val uiState: StateFlow<EventQrUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = getEventByIdUseCase(eventId)) {
                is Result.Success ->
                    _uiState.update {
                        it.copy(
                            event = r.data,
                            isLoading = false,
                            isLoadingAttendees = true
                        )
                    }
                is Result.Error ->
                    _uiState.update {
                        it.copy(isLoading = false, error = r.message)
                    }
                Result.Loading -> Unit
            }
            when (val r = getEventAttendeesUseCase(eventId)) {
                is Result.Success ->
                    _uiState.update {
                        it.copy(attendees = r.data, isLoadingAttendees = false)
                    }
                is Result.Error ->
                    _uiState.update { it.copy(isLoadingAttendees = false) }
                Result.Loading -> Unit
            }
        }
    }
}
