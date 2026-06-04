package com.grace.app.presentation.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.GameStats
import com.grace.app.domain.model.LeaderboardEntry
import com.grace.app.domain.usecase.games.GetMyGameStatsUseCase
import com.grace.app.domain.usecase.games.GetWeeklyLeaderboardUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GamesHomeUiState(
    val isLoading: Boolean = true,
    val stats: GameStats = GameStats(userId = ""),
    val leaderboardPreview: List<LeaderboardEntry> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class GamesHomeViewModel @Inject constructor(
    private val getMyGameStatsUseCase: GetMyGameStatsUseCase,
    private val getWeeklyLeaderboardUseCase: GetWeeklyLeaderboardUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamesHomeUiState())
    val uiState: StateFlow<GamesHomeUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val stats = (getMyGameStatsUseCase() as? Result.Success)?.data
                ?: GameStats(userId = "")
            // Pull Top 3 for the preview tile; full Top 5 lives on the
            // dedicated leaderboard screen (Phase 4).
            val board = (getWeeklyLeaderboardUseCase(limit = 3) as? Result.Success)
                ?.data.orEmpty()
            _uiState.update {
                it.copy(isLoading = false, stats = stats, leaderboardPreview = board)
            }
        }
    }
}
