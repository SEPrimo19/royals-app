package com.grace.app.presentation.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.usecase.admin.BuildComplianceReportUseCase
import com.grace.app.domain.usecase.admin.ComplianceAudience
import com.grace.app.domain.usecase.admin.ComplianceReportData
import com.grace.app.domain.util.Result
import com.grace.app.presentation.screens.progress.ExportPeriod
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

data class AdminComplianceUiState(
    val audience: ComplianceAudience = ComplianceAudience.ALL,
    val period: ExportPeriod = ExportPeriod.AllTime,
    val includeAttendance: Boolean = true,
    val includeMeditation: Boolean = true,
    val isLoading: Boolean = false,
    val report: ComplianceReportData? = null,
    val error: String? = null
)

sealed interface AdminComplianceEvent {
    data class AudienceChanged(val audience: ComplianceAudience) : AdminComplianceEvent
    data class PeriodChanged(val period: ExportPeriod) : AdminComplianceEvent
    data class IncludeAttendanceChanged(val on: Boolean) : AdminComplianceEvent
    data class IncludeMeditationChanged(val on: Boolean) : AdminComplianceEvent
    data object Refresh : AdminComplianceEvent
}

sealed interface AdminComplianceEffect {
    data class ShowError(val message: String) : AdminComplianceEffect
}

@HiltViewModel
class AdminComplianceReportViewModel @Inject constructor(
    private val buildComplianceReportUseCase: BuildComplianceReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminComplianceUiState())
    val uiState: StateFlow<AdminComplianceUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<AdminComplianceEffect>()
    val effect: SharedFlow<AdminComplianceEffect> = _effect.asSharedFlow()

    init { refresh() }

    fun onEvent(event: AdminComplianceEvent) {
        when (event) {
            is AdminComplianceEvent.AudienceChanged -> {
                _uiState.update { it.copy(audience = event.audience) }
                refresh()
            }
            is AdminComplianceEvent.PeriodChanged -> {
                _uiState.update { it.copy(period = event.period) }
                refresh()
            }
            is AdminComplianceEvent.IncludeAttendanceChanged ->
                _uiState.update { it.copy(includeAttendance = event.on) }
            is AdminComplianceEvent.IncludeMeditationChanged ->
                _uiState.update { it.copy(includeMeditation = event.on) }
            AdminComplianceEvent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = buildComplianceReportUseCase(
                audience = s.audience,
                periodIncludes = { dt -> s.period.includes(dt) }
            )
            when (result) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, report = result.data)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                    _effect.emit(AdminComplianceEffect.ShowError(result.message))
                }
                Result.Loading -> Unit
            }
        }
    }
}

private fun ExportPeriod.includes(dt: java.time.LocalDateTime): Boolean = when (this) {
    is ExportPeriod.AllTime -> true
    is ExportPeriod.Month ->
        java.time.YearMonth.from(dt.toLocalDate()) == this.yearMonth
    is ExportPeriod.WholeYear -> dt.year == this.year
}
