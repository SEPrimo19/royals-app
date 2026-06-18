package com.grace.app.presentation.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.domain.model.LeaderboardEntry
import com.grace.app.domain.usecase.games.GetMonthlyGlobalLeaderboardUseCase
import com.grace.app.domain.usecase.games.GetTeamLeaderboardUseCase
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

enum class LeaderboardPeriod { WEEKLY_CELL, MONTHLY_GLOBAL, TEAMS }

data class LeaderboardUiState(
    val period: LeaderboardPeriod = LeaderboardPeriod.WEEKLY_CELL,
    val isLoading: Boolean = true,
    val weeklyRows: List<LeaderboardEntry> = emptyList(),
    val monthlyRows: List<LeaderboardEntry> = emptyList(),
    val teamRows: List<LeaderboardEntry> = emptyList(),
    val weeklyLoaded: Boolean = false,
    val monthlyLoaded: Boolean = false,
    val teamLoaded: Boolean = false,
    val hasGroup: Boolean = true,
    val error: String? = null
) {
    val rows: List<LeaderboardEntry>
        get() = when (period) {
            LeaderboardPeriod.WEEKLY_CELL -> weeklyRows
            LeaderboardPeriod.MONTHLY_GLOBAL -> monthlyRows
            LeaderboardPeriod.TEAMS -> teamRows
        }

    val podium: List<LeaderboardEntry> get() = rows.take(3)
    val rest: List<LeaderboardEntry> get() = rows.drop(3)
    val isUserInList: Boolean get() = rows.any { it.isMe }
    val myRow: LeaderboardEntry? get() = rows.firstOrNull { it.isMe }

    val showNoGroupCard: Boolean
        get() = period == LeaderboardPeriod.WEEKLY_CELL && !hasGroup
}

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val getWeeklyLeaderboardUseCase: GetWeeklyLeaderboardUseCase,
    private val getMonthlyGlobalLeaderboardUseCase: GetMonthlyGlobalLeaderboardUseCase,
    private val getTeamLeaderboardUseCase: GetTeamLeaderboardUseCase,
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

    fun refresh() {
        when (_uiState.value.period) {
            LeaderboardPeriod.WEEKLY_CELL ->
                _uiState.update { it.copy(weeklyLoaded = false) }
            LeaderboardPeriod.MONTHLY_GLOBAL ->
                _uiState.update { it.copy(monthlyLoaded = false) }
            LeaderboardPeriod.TEAMS ->
                _uiState.update { it.copy(teamLoaded = false) }
        }
        loadCurrent()
    }

    private fun loadCurrent() {
        viewModelScope.launch {
            val s = _uiState.value
            val alreadyLoaded = when (s.period) {
                LeaderboardPeriod.WEEKLY_CELL -> s.weeklyLoaded
                LeaderboardPeriod.MONTHLY_GLOBAL -> s.monthlyLoaded
                LeaderboardPeriod.TEAMS -> s.teamLoaded
            }
            if (alreadyLoaded) {
                _uiState.update { it.copy(isLoading = false, error = null) }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, error = null) }
            when (_uiState.value.period) {
                LeaderboardPeriod.WEEKLY_CELL -> loadWeekly()
                LeaderboardPeriod.MONTHLY_GLOBAL -> loadMonthly()
                LeaderboardPeriod.TEAMS -> loadTeam()
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

    private suspend fun loadTeam() {
        when (val r = getTeamLeaderboardUseCase(limit = 25)) {
            is Result.Success -> _uiState.update {
                it.copy(
                    isLoading = false,
                    teamRows = r.data,
                    teamLoaded = true
                )
            }
            is Result.Error -> _uiState.update {
                it.copy(isLoading = false, error = r.message)
            }
            Result.Loading -> Unit
        }
    }
}
