package com.grace.app.presentation.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.domain.model.LeaderboardEntry
import com.grace.app.domain.usecase.games.GetMonthlyGlobalLeaderboardUseCase
import com.grace.app.domain.usecase.games.GetWeeklyLeaderboardUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Two boards, one screen. Period switches between:
 *   - WEEKLY_CELL  — caller's cell group, Daily Challenge only, resets Monday.
 *   - MONTHLY_GLOBAL — entire church, Daily + Practice, resets 1st of month.
 *
 * The user picks via a tab toggle in the screen header. We lazy-load each
 * tab: the first tap on a tab fetches it, subsequent taps reuse the cache.
 */
enum class LeaderboardPeriod { WEEKLY_CELL, MONTHLY_GLOBAL }

data class LeaderboardUiState(
    val period: LeaderboardPeriod = LeaderboardPeriod.WEEKLY_CELL,
    val isLoading: Boolean = true,
    // Cached rows per period so swapping tabs is instant after first load.
    val weeklyRows: List<LeaderboardEntry> = emptyList(),
    val monthlyRows: List<LeaderboardEntry> = emptyList(),
    val weeklyLoaded: Boolean = false,
    val monthlyLoaded: Boolean = false,
    val hasGroup: Boolean = true,
    val error: String? = null
) {
    /** Rows for the currently-selected period. */
    val rows: List<LeaderboardEntry>
        get() = if (period == LeaderboardPeriod.WEEKLY_CELL) weeklyRows else monthlyRows

    /** Top 3 for the podium row. */
    val podium: List<LeaderboardEntry> get() = rows.take(3)
    val rest: List<LeaderboardEntry> get() = rows.drop(3)
    val isUserInList: Boolean get() = rows.any { it.isMe }
    val myRow: LeaderboardEntry? get() = rows.firstOrNull { it.isMe }

    /** Weekly board hides when the user has no group; monthly is always open. */
    val showNoGroupCard: Boolean
        get() = period == LeaderboardPeriod.WEEKLY_CELL && !hasGroup
}

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val getWeeklyLeaderboardUseCase: GetWeeklyLeaderboardUseCase,
    private val getMonthlyGlobalLeaderboardUseCase: GetMonthlyGlobalLeaderboardUseCase,
    private val prefs: UserPreferencesRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init { loadCurrent() }

    fun selectPeriod(period: LeaderboardPeriod) {
        if (_uiState.value.period == period) return
        _uiState.update { it.copy(period = period, error = null) }
        loadCurrent()
    }

    /** Re-fetch from network for the currently selected period. */
    fun refresh() {
        when (_uiState.value.period) {
            LeaderboardPeriod.WEEKLY_CELL ->
                _uiState.update { it.copy(weeklyLoaded = false) }
            LeaderboardPeriod.MONTHLY_GLOBAL ->
                _uiState.update { it.copy(monthlyLoaded = false) }
        }
        loadCurrent()
    }

    private fun loadCurrent() {
        viewModelScope.launch {
            val s = _uiState.value
            // Reuse cache if the active tab already has data — avoids
            // a flash of loading every time the user toggles tabs.
            val alreadyLoaded =
                (s.period == LeaderboardPeriod.WEEKLY_CELL && s.weeklyLoaded) ||
                    (s.period == LeaderboardPeriod.MONTHLY_GLOBAL && s.monthlyLoaded)
            if (alreadyLoaded) {
                _uiState.update { it.copy(isLoading = false, error = null) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            when (_uiState.value.period) {
                LeaderboardPeriod.WEEKLY_CELL -> loadWeekly()
                LeaderboardPeriod.MONTHLY_GLOBAL -> loadMonthly()
            }
        }
    }

    private suspend fun loadWeekly() {
        val hasGroup = !prefs.groupId.first().isNullOrBlank()
        if (!hasGroup) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    hasGroup = false,
                    weeklyRows = emptyList(),
                    weeklyLoaded = true
                )
            }
            return
        }
        when (val r = getWeeklyLeaderboardUseCase(limit = 10)) {
            is Result.Success -> _uiState.update {
                it.copy(
                    isLoading = false,
                    hasGroup = true,
                    weeklyRows = r.data,
                    weeklyLoaded = true
                )
            }
            is Result.Error -> _uiState.update {
                it.copy(isLoading = false, hasGroup = true, error = r.message)
            }
            Result.Loading -> Unit
        }
    }

    private suspend fun loadMonthly() {
        when (val r = getMonthlyGlobalLeaderboardUseCase(limit = 25)) {
            is Result.Success -> _uiState.update {
                it.copy(
                    isLoading = false,
                    monthlyRows = r.data,
                    monthlyLoaded = true
                )
            }
            is Result.Error -> _uiState.update {
                it.copy(isLoading = false, error = r.message)
            }
            Result.Loading -> Unit
        }
    }
}
