package com.grace.app.presentation.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.domain.model.Event
import com.grace.app.domain.model.RsvpStatus
import com.grace.app.domain.model.UserRole
import com.grace.app.domain.usecase.events.DeleteEventUseCase
import com.grace.app.domain.usecase.events.GetEventsUseCase
import com.grace.app.domain.usecase.events.RsvpToEventUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EventsUiState(
    val events: List<Event> = emptyList(),
    val isLoading: Boolean = true,
    val currentUserId: String? = null,
    val currentRole: UserRole = UserRole.MEMBER,
    val error: String? = null
) {
    val canCreateEvents: Boolean
        get() = currentRole == UserRole.CELL_LEADER ||
            currentRole == UserRole.YOUTH_PRESIDENT ||
            currentRole == UserRole.PASTOR ||
            currentRole == UserRole.ADMIN

    fun canManage(event: Event): Boolean {
        if (event.createdBy != null && event.createdBy == currentUserId) return true
        return currentRole == UserRole.YOUTH_PRESIDENT ||
            currentRole == UserRole.PASTOR ||
            currentRole == UserRole.ADMIN
    }
}

sealed interface EventsEvent {
    data class Rsvp(val eventId: String, val status: RsvpStatus) : EventsEvent
    data class DeleteEvent(val eventId: String) : EventsEvent
    data object DismissError : EventsEvent
}

sealed interface EventsEffect {
    data class ShowError(val message: String) : EventsEffect
}

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val getEventsUseCase: GetEventsUseCase,
    private val rsvpToEventUseCase: RsvpToEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val prefs: UserPreferencesRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(EventsUiState())
    val uiState: StateFlow<EventsUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<EventsEffect>()
    val effect: SharedFlow<EventsEffect> = _effect.asSharedFlow()

    private var eventsJob: Job? = null

    init {
        viewModelScope.launch {
            val role = parseRole(prefs.userRole.first() ?: "member")
            _uiState.update {
                it.copy(currentUserId = prefs.userId.first(), currentRole = role)
            }
        }
        observeEvents()
    }

    private fun parseRole(raw: String): UserRole = when (raw.trim().lowercase()) {
        "cell_leader" -> UserRole.CELL_LEADER
        "youth_president" -> UserRole.YOUTH_PRESIDENT
        "pastor" -> UserRole.PASTOR
        "admin" -> UserRole.ADMIN
        else -> UserRole.MEMBER
    }

    private fun observeEvents() {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            getEventsUseCase().collect { result ->
                when (result) {
                    is Result.Success ->
                        _uiState.update {
                            it.copy(events = result.data, isLoading = false, error = null)
                        }
                    is Result.Error ->
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    Result.Loading ->
                        _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun refresh() = observeEvents()

    fun surfaceError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun onEvent(event: EventsEvent) {
        when (event) {
            is EventsEvent.Rsvp -> rsvp(event.eventId, event.status)
            is EventsEvent.DeleteEvent -> deleteEvent(event.eventId)
            EventsEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            when (val r = deleteEventUseCase(eventId)) {
                is Result.Success -> observeEvents()
                is Result.Error -> _effect.emit(EventsEffect.ShowError(r.message))
                Result.Loading -> Unit
            }
        }
    }

    private fun rsvp(eventId: String, status: RsvpStatus) {
        viewModelScope.launch {
            when (val r = rsvpToEventUseCase(eventId, status)) {
                is Result.Success -> observeEvents()
                is Result.Error -> _effect.emit(EventsEffect.ShowError(r.message))
                Result.Loading -> Unit
            }
        }
    }
}
