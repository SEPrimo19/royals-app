package com.grace.app.presentation.screens.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.AttendedEvent
import com.grace.app.domain.usecase.progress.GetMyAttendanceUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

data class MyAttendanceUiState(
    val isLoading: Boolean = true,
    val attended: List<AttendedEvent> = emptyList(),
    val totalAllTime: Int = 0,
    val thisMonth: Int = 0,
    val thisYear: Int = 0,
    val mostRecent: LocalDateTime? = null,
    val error: String? = null
)

@HiltViewModel
class MyAttendanceViewModel @Inject constructor(
    private val getMyAttendanceUseCase: GetMyAttendanceUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyAttendanceUiState())
    val uiState: StateFlow<MyAttendanceUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = getMyAttendanceUseCase()) {
                is Result.Success -> {
                    val now = LocalDateTime.now()
                    val thisMonth = YearMonth.from(now)
                    val list = r.data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            attended = list,
                            totalAllTime = list.size,
                            thisMonth = list.count {
                                YearMonth.from(it.attendedAt) == thisMonth
                            },
                            thisYear = list.count {
                                it.attendedAt.year == now.year
                            },
                            mostRecent = list.firstOrNull()?.attendedAt
                        )
                    }
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = r.message)
                }
                Result.Loading -> Unit
            }
        }
    }
}
