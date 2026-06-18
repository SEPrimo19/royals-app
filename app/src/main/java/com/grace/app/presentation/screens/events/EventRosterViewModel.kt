package com.grace.app.presentation.screens.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.AttendanceStatus
import com.grace.app.domain.model.Attendee
import com.grace.app.domain.model.Event
import com.grace.app.domain.repository.EventRepository
import com.grace.app.domain.repository.LeaderRepository
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

enum class RosterFilter(val label: String) {
    ALL("All"),
    PRESENT("Present"),
    LATE("Late"),
    EXCUSED("Excused"),
    ABSENT("Absent")
}

data class EventRosterUiState(
    val isLoading: Boolean = true,
    val event: Event? = null,
    val roster: List<Attendee> = emptyList(),
    val inFlightMemberIds: Set<String> = emptySet(),
    val filter: RosterFilter = RosterFilter.ALL,
    val error: String? = null,
    val toast: String? = null
) {
    val eventHasEnded: Boolean
        get() {
            val ev = event ?: return false
            val end = ev.endDate ?: ev.eventDate.plusHours(2)
            return LocalDateTime.now().isAfter(end)
        }

    val filteredRoster: List<Attendee>
        get() = when (filter) {
            RosterFilter.ALL -> roster
            RosterFilter.PRESENT ->
                roster.filter { it.status == AttendanceStatus.PRESENT }
            RosterFilter.LATE ->
                roster.filter { it.status == AttendanceStatus.LATE }
            RosterFilter.EXCUSED ->
                roster.filter { it.status == AttendanceStatus.EXCUSED }
            RosterFilter.ABSENT ->
                roster.filter { it.status == AttendanceStatus.ABSENT }
        }

    val counts: Map<AttendanceStatus, Int>
        get() = roster.groupingBy { it.status }.eachCount()
}

sealed interface EventRosterEvent {
    data class MarkAttendance(val memberId: String, val status: AttendanceStatus) : EventRosterEvent
    data class UndoAttendance(val memberId: String) : EventRosterEvent
    data class FilterChanged(val filter: RosterFilter) : EventRosterEvent
    data object DismissToast : EventRosterEvent
    data object Refresh : EventRosterEvent
}

@HiltViewModel
class EventRosterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val leaderRepository: LeaderRepository
) : ViewModel() {

    private val eventId: String = savedStateHandle.get<String>("eventId").orEmpty()

    private val _uiState = MutableStateFlow(EventRosterUiState())
    val uiState: StateFlow<EventRosterUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val eventResult = eventRepository.getEvent(eventId)
            val rosterResult = leaderRepository.getEventRosterForLeader(eventId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    event = (eventResult as? Result.Success)?.data,
                    roster = (rosterResult as? Result.Success)?.data.orEmpty(),
                    error = (rosterResult as? Result.Error)?.message
                )
            }
        }
    }

    fun onEvent(event: EventRosterEvent) {
        when (event) {
            is EventRosterEvent.MarkAttendance -> mark(event.memberId, event.status)
            is EventRosterEvent.UndoAttendance -> undo(event.memberId)
            is EventRosterEvent.FilterChanged ->
                _uiState.update { it.copy(filter = event.filter) }
            EventRosterEvent.Refresh -> load()
            EventRosterEvent.DismissToast -> _uiState.update { it.copy(toast = null) }
        }
    }

    private fun mark(memberId: String, status: AttendanceStatus) {
        _uiState.update { it.copy(inFlightMemberIds = it.inFlightMemberIds + memberId) }
        viewModelScope.launch {
            val r = leaderRepository.markProxyAttendance(eventId, memberId, status)
            when (r) {
                is Result.Success -> {
                    _uiState.update { s ->
                        s.copy(
                            roster = s.roster.map { a ->
                                if (a.user.id == memberId) a.copy(status = status) else a
                            },
                            inFlightMemberIds = s.inFlightMemberIds - memberId,
                            toast = "✓ Marked ${status.name.lowercase()}"
                        )
                    }
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        inFlightMemberIds = it.inFlightMemberIds - memberId,
                        toast = "⚠ ${r.message}"
                    )
                }
                else -> _uiState.update {
                    it.copy(inFlightMemberIds = it.inFlightMemberIds - memberId)
                }
            }
        }
    }

    private fun undo(memberId: String) {
        _uiState.update { it.copy(inFlightMemberIds = it.inFlightMemberIds + memberId) }
        viewModelScope.launch {
            val r = leaderRepository.removeProxyAttendance(eventId, memberId)
            when (r) {
                is Result.Success -> _uiState.update { s ->
                    s.copy(
                        roster = s.roster.map { a ->
                            if (a.user.id == memberId) a.copy(status = AttendanceStatus.ABSENT)
                            else a
                        },
                        inFlightMemberIds = s.inFlightMemberIds - memberId,
                        toast = "Undone"
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        inFlightMemberIds = it.inFlightMemberIds - memberId,
                        toast = "⚠ ${r.message}"
                    )
                }
                else -> _uiState.update {
                    it.copy(inFlightMemberIds = it.inFlightMemberIds - memberId)
                }
            }
        }
    }
}
