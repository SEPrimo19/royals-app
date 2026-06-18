package com.grace.app.presentation.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.AttendedEvent
import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.ProgressSnapshot
import com.grace.app.domain.model.User
import com.grace.app.domain.model.WeeklyMeditation
import com.grace.app.domain.repository.AuthRepository
import com.grace.app.domain.usecase.meditation.GetAllMeditationsUseCase
import com.grace.app.domain.usecase.meditation.GetMyMeditationSubmissionsUseCase
import com.grace.app.domain.usecase.progress.GetMyAttendanceUseCase
import com.grace.app.domain.usecase.progress.GetMyProgressUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class ProgressReflectionItem(
    val submission: MeditationSubmission,
    val meditation: WeeklyMeditation?
)

sealed interface ExportPeriod {
    fun label(): String

    data object AllTime : ExportPeriod {
        override fun label() = "All time"
    }

    data class Month(val yearMonth: YearMonth) : ExportPeriod {
        override fun label(): String =
            yearMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    data class WholeYear(val year: Int) : ExportPeriod {
        override fun label() = "All of $year"
    }
}

fun buildPeriodOptions(today: LocalDate = LocalDate.now()): List<ExportPeriod> =
    buildList {
        add(ExportPeriod.AllTime)
        val currentMonth = YearMonth.from(today)
        for (i in 0..11) {
            add(ExportPeriod.Month(currentMonth.minusMonths(i.toLong())))
        }
        add(ExportPeriod.WholeYear(today.year))
        add(ExportPeriod.WholeYear(today.year - 1))
    }

data class MyProgressUiState(
    val isLoading: Boolean = true,
    val snapshot: ProgressSnapshot = ProgressSnapshot(),
    val attendedEvents: List<AttendedEvent> = emptyList(),
    val reflections: List<ProgressReflectionItem> = emptyList(),
    val me: User? = null,
    val includeAll: Boolean = false,
    val includeAttendance: Boolean = true,
    val includeMeditation: Boolean = true,
    val period: ExportPeriod = ExportPeriod.AllTime,
    val error: String? = null
)

sealed interface MyProgressEvent {
    data class IncludeAllChanged(val on: Boolean) : MyProgressEvent
    data class IncludeAttendanceChanged(val on: Boolean) : MyProgressEvent
    data class IncludeMeditationChanged(val on: Boolean) : MyProgressEvent
    data class PeriodChanged(val period: ExportPeriod) : MyProgressEvent
}

@HiltViewModel
class MyProgressViewModel @Inject constructor(
    private val getMyProgressUseCase: GetMyProgressUseCase,
    private val getMyAttendanceUseCase: GetMyAttendanceUseCase,
    private val getMyMeditationSubmissionsUseCase: GetMyMeditationSubmissionsUseCase,
    private val getAllMeditationsUseCase: GetAllMeditationsUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyProgressUiState())
    val uiState: StateFlow<MyProgressUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val snapshot = getMyProgressUseCase()
            val attended = (runCatching { getMyAttendanceUseCase() }.getOrNull()
                as? Result.Success)?.data.orEmpty()
            val subs = runCatching {
                getMyMeditationSubmissionsUseCase().first()
            }.getOrDefault(emptyList())
            val meds = runCatching {
                getAllMeditationsUseCase().first()
            }.getOrDefault(emptyList())
            val medsById = meds.associateBy { it.id }
            val reflections = subs.map {
                ProgressReflectionItem(it, medsById[it.meditationId])
            }
            val me = (runCatching { authRepository.getMyProfile() }.getOrNull()
                as? Result.Success)?.data
            _uiState.update {
                it.copy(
                    isLoading = false,
                    snapshot = snapshot,
                    attendedEvents = attended,
                    reflections = reflections,
                    me = me
                )
            }
        }
    }

    fun onEvent(event: MyProgressEvent) {
        when (event) {
            is MyProgressEvent.IncludeAllChanged ->
                _uiState.update { it.copy(includeAll = event.on) }
            is MyProgressEvent.IncludeAttendanceChanged ->
                _uiState.update { it.copy(includeAttendance = event.on) }
            is MyProgressEvent.IncludeMeditationChanged ->
                _uiState.update { it.copy(includeMeditation = event.on) }
            is MyProgressEvent.PeriodChanged ->
                _uiState.update { it.copy(period = event.period) }
        }
    }
}
