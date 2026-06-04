package com.grace.app.presentation.screens.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.usecase.events.CreateEventUseCase
import com.grace.app.domain.usecase.events.GetEventByIdUseCase
import com.grace.app.domain.usecase.events.UpdateEventUseCase
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
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class EventFormUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val title: String = "",
    val description: String = "",
    val date: LocalDate = LocalDate.now().plusDays(1),
    val time: LocalTime = LocalTime.of(19, 0), // 7pm default — common church evening
    // Optional end time. When null the legacy "+2h after start" window
    // applies. We default it to start+2h on the UI to give creators a sane
    // visible starting point they can adjust or clear.
    val endDate: LocalDate? = LocalDate.now().plusDays(1),
    val endTime: LocalTime? = LocalTime.of(21, 0),
    val location: String = "",
    val isRecurring: Boolean = false,
    // When false the event becomes an info-only reminder: no QR, no
    // check-in, no attendance tracking. Defaults to true (most events
    // are real events you want to track).
    val requiresAttendance: Boolean = true,
    val error: String? = null
) {
    val canSave: Boolean
        get() {
            if (isSaving) return false
            if (title.trim().length < 3) return false
            val end = composedEndDateTime
            if (end != null && !end.isAfter(composedDateTime)) return false
            return true
        }
    val composedDateTime: LocalDateTime get() = LocalDateTime.of(date, time)
    val composedEndDateTime: LocalDateTime?
        get() = if (endDate != null && endTime != null)
            LocalDateTime.of(endDate, endTime) else null
}

sealed interface EventFormEvent {
    data class TitleChanged(val value: String) : EventFormEvent
    data class DescriptionChanged(val value: String) : EventFormEvent
    data class DateChanged(val value: LocalDate) : EventFormEvent
    data class TimeChanged(val value: LocalTime) : EventFormEvent
    data class EndDateChanged(val value: LocalDate) : EventFormEvent
    data class EndTimeChanged(val value: LocalTime) : EventFormEvent
    data object ClearEnd : EventFormEvent
    data class LocationChanged(val value: String) : EventFormEvent
    data class RecurringChanged(val value: Boolean) : EventFormEvent
    data class RequiresAttendanceChanged(val value: Boolean) : EventFormEvent
    data object Save : EventFormEvent
}

sealed interface EventFormEffect {
    data object Saved : EventFormEffect
    data class ShowError(val message: String) : EventFormEffect
}

@HiltViewModel
class EventFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val createEventUseCase: CreateEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase
) : ViewModel() {

    // Nav arg is optional — "new" route passes the string "new"; edit passes uuid.
    private val eventId: String? =
        savedStateHandle.get<String>("eventId")?.takeIf { it != "new" }

    private val _uiState = MutableStateFlow(EventFormUiState(isEditMode = eventId != null))
    val uiState: StateFlow<EventFormUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<EventFormEffect>()
    val effect: SharedFlow<EventFormEffect> = _effect.asSharedFlow()

    init {
        if (eventId != null) loadForEdit(eventId)
    }

    private fun loadForEdit(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val r = getEventByIdUseCase(id)) {
                is Result.Success -> r.data?.let { ev ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            title = ev.title,
                            description = ev.description.orEmpty(),
                            date = ev.eventDate.toLocalDate(),
                            time = ev.eventDate.toLocalTime(),
                            endDate = ev.endDate?.toLocalDate(),
                            endTime = ev.endDate?.toLocalTime(),
                            location = ev.location.orEmpty(),
                            isRecurring = ev.isRecurring,
                            requiresAttendance = ev.requiresAttendance
                        )
                    }
                } ?: _uiState.update {
                    it.copy(isLoading = false, error = "Event not found.")
                }
                is Result.Error ->
                    _uiState.update { it.copy(isLoading = false, error = r.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun onEvent(event: EventFormEvent) {
        when (event) {
            is EventFormEvent.TitleChanged ->
                _uiState.update { it.copy(title = event.value) }
            is EventFormEvent.DescriptionChanged ->
                _uiState.update { it.copy(description = event.value) }
            is EventFormEvent.DateChanged ->
                _uiState.update {
                    // Keep end date in sync if the user hasn't customized it
                    // (still matches old start date). Cuts a step for the
                    // common "event spans the same day" case.
                    val keepEndInSync = it.endDate == it.date
                    it.copy(
                        date = event.value,
                        endDate = if (keepEndInSync) event.value else it.endDate
                    )
                }
            is EventFormEvent.TimeChanged ->
                _uiState.update {
                    // Bump default end time 2h forward when the user
                    // hasn't customized it (it still equals start+2h).
                    val defaultEnd = it.time.plusHours(2)
                    val keepEndInSync = it.endTime != null &&
                        it.endTime == defaultEnd
                    it.copy(
                        time = event.value,
                        endTime = if (keepEndInSync) event.value.plusHours(2)
                            else it.endTime
                    )
                }
            is EventFormEvent.EndDateChanged ->
                _uiState.update { it.copy(endDate = event.value) }
            is EventFormEvent.EndTimeChanged ->
                _uiState.update { it.copy(endTime = event.value) }
            EventFormEvent.ClearEnd ->
                _uiState.update { it.copy(endDate = null, endTime = null) }
            is EventFormEvent.LocationChanged ->
                _uiState.update { it.copy(location = event.value) }
            is EventFormEvent.RecurringChanged ->
                _uiState.update { it.copy(isRecurring = event.value) }
            is EventFormEvent.RequiresAttendanceChanged ->
                _uiState.update { it.copy(requiresAttendance = event.value) }
            EventFormEvent.Save -> save()
        }
    }

    private fun save() {
        val s = _uiState.value
        if (!s.canSave) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            // Handle create / update separately — avoids an unchecked cast
            // from Result<Event> to Result<Unit>. Both paths share the same
            // success/error tail below.
            val message: String? = if (eventId == null) {
                when (val r = createEventUseCase(
                    title = s.title,
                    description = s.description,
                    eventDate = s.composedDateTime,
                    endDate = s.composedEndDateTime,
                    location = s.location,
                    isRecurring = s.isRecurring,
                    requiresAttendance = s.requiresAttendance
                )) {
                    is Result.Success -> null
                    is Result.Error -> r.message
                    Result.Loading -> null
                }
            } else {
                when (val r = updateEventUseCase(
                    eventId = eventId,
                    title = s.title,
                    description = s.description,
                    eventDate = s.composedDateTime,
                    endDate = s.composedEndDateTime,
                    location = s.location,
                    isRecurring = s.isRecurring,
                    requiresAttendance = s.requiresAttendance
                )) {
                    is Result.Success -> null
                    is Result.Error -> r.message
                    Result.Loading -> null
                }
            }
            _uiState.update { it.copy(isSaving = false) }
            if (message == null) {
                _effect.emit(EventFormEffect.Saved)
            } else {
                _uiState.update { it.copy(error = message) }
                _effect.emit(EventFormEffect.ShowError(message))
            }
        }
    }
}
