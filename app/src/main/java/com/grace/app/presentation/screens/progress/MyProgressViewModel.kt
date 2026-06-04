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

/**
 * Pairs a meditation submission with the meditation entry it responded to —
 * the PDF needs both to render meaningful context (title + scripture + the
 * user's reflection text). meditation is nullable for the rare case where
 * a meditation was deleted server-side after the user submitted.
 */
data class ProgressReflectionItem(
    val submission: MeditationSubmission,
    val meditation: WeeklyMeditation?
)

/**
 * Date-range filter applied to the export. Sealed so the report builder
 * can `when` on it without dealing with sentinel year=0 / month=0 values.
 */
sealed interface ExportPeriod {
    fun label(): String

    /** No filtering — include every record. */
    data object AllTime : ExportPeriod {
        override fun label() = "All time"
    }

    /** Just the events / reflections that fall inside this calendar month. */
    data class Month(val yearMonth: YearMonth) : ExportPeriod {
        override fun label(): String =
            yearMonth.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    /** All of a specific year. */
    data class WholeYear(val year: Int) : ExportPeriod {
        override fun label() = "All of $year"
    }
}

/**
 * Pre-canned period options for the dropdown. Builds the list dynamically
 * from "today" so the menu always opens to the current month plus a
 * meaningful slice of history — better than hardcoding a year range.
 */
fun buildPeriodOptions(today: LocalDate = LocalDate.now()): List<ExportPeriod> =
    buildList {
        add(ExportPeriod.AllTime)
        // Current month + last 11. Covers the typical "monthly compliance
        // report" use case without flooding the dropdown.
        val currentMonth = YearMonth.from(today)
        for (i in 0..11) {
            add(ExportPeriod.Month(currentMonth.minusMonths(i.toLong())))
        }
        // The current year + previous year as whole-year options, for
        // year-end summaries.
        add(ExportPeriod.WholeYear(today.year))
        add(ExportPeriod.WholeYear(today.year - 1))
    }

data class MyProgressUiState(
    val isLoading: Boolean = true,
    val snapshot: ProgressSnapshot = ProgressSnapshot(),
    val attendedEvents: List<AttendedEvent> = emptyList(),
    val reflections: List<ProgressReflectionItem> = emptyList(),
    /** Self-profile so the PDF cover page can show name/role/Compassion #. */
    val me: User? = null,
    /**
     * Export filters per the Compassion spec:
     *   - All           = include the personal-stats sections (devo/prayer/community)
     *   - Attendance    = include the per-event attendance list
     *   - Meditation    = include the user's reflection text per week
     * Defaults match user direction: the two Compassion-compliance sections
     * are checked, the broader personal-stats section is off — so the
     * default export is a tight compliance report. User can opt into the
     * fuller picture.
     */
    val includeAll: Boolean = false,
    val includeAttendance: Boolean = true,
    val includeMeditation: Boolean = true,
    /**
     * Which period the export covers. Defaults to AllTime so users who
     * don't care still get a complete report on first tap.
     */
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
            // GetMyProgressUseCase never throws (wraps each fetch) so the
            // snapshot lands here whole, possibly with zeros for any failed
            // sub-fetch. Better to show partial truth than to show nothing.
            val snapshot = getMyProgressUseCase()
            // Attendance + meditation data load in parallel — both are
            // wrapped in runCatching so a single failure doesn't blow up
            // the screen.
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
