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
    // The event lookup — drives the initial full-screen spinner.
    val isLoading: Boolean = true,
    // The attendees lookup — drives an inline hint under the QR so the
    // user knows the screen is still working without losing the QR.
    val isLoadingAttendees: Boolean = false,
    val error: String? = null
) {
    /** QR content — what gets scanned. Custom scheme so MainActivity catches it. */
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
                        // Event done loading — hand off the spinner to
                        // isLoadingAttendees so the QR can render now
                        // while the (slower) roster query continues.
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
            // Attendee fetch is best-effort — if RLS denies (unlikely since
            // only creators reach this screen) we just show "0 checked in".
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
