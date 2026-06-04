package com.grace.app.presentation.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.BiblePassage
import com.grace.app.domain.model.GameMode
import com.grace.app.domain.usecase.games.CompleteDailyFitbUseCase
import com.grace.app.domain.usecase.games.GetRandomFillInBlankUseCase
import com.grace.app.domain.usecase.games.RecordAttemptUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FillInBlankUiState(
    val isLoading: Boolean = true,
    val passage: BiblePassage? = null,
    /** Options pinned at load time so the order is stable across recomposition. */
    val options: List<String> = emptyList(),
    val selectedOption: String? = null,
    val correct: Boolean = false,
    val pointsEarned: Int = 0,
    val isFinished: Boolean = false,
    val streakAfter: Int? = null,
    val error: String? = null
) {
    val hasAnswered: Boolean get() = selectedOption != null
    /** Verse text with the blank word replaced for the play view. */
    val verseWithBlank: String?
        get() = passage?.let { p ->
            val token = p.blankWord
            val replaced = p.text.replace(token, BLANK_TOKEN, ignoreCase = false)
            // Fallback: case-insensitive replace if exact-case missed.
            if (replaced == p.text)
                p.text.replace(Regex("(?i)" + Regex.escape(token)), BLANK_TOKEN)
            else replaced
        }
}

private const val BLANK_TOKEN = "______"
private const val FITB_POINTS = 25

@HiltViewModel
class FillInBlankViewModel @Inject constructor(
    private val getRandomFillInBlankUseCase: GetRandomFillInBlankUseCase,
    private val recordAttemptUseCase: RecordAttemptUseCase,
    private val completeDailyFitbUseCase: CompleteDailyFitbUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FillInBlankUiState())
    val uiState: StateFlow<FillInBlankUiState> = _uiState.asStateFlow()

    init { loadPassage() }

    private fun loadPassage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = getRandomFillInBlankUseCase()) {
                is Result.Success -> {
                    val passage = r.data
                    if (passage == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "No verses available yet."
                            )
                        }
                    } else {
                        // Pin the option order once so it stays stable while
                        // the user thinks — re-shuffling on recomposition
                        // would feel chaotic.
                        val opts = (passage.distractors + passage.blankWord)
                            .distinct()
                            .shuffled()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                passage = passage,
                                options = opts
                            )
                        }
                    }
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = r.message)
                }
                Result.Loading -> Unit
            }
        }
    }

    fun selectOption(option: String) {
        val state = _uiState.value
        if (state.hasAnswered) return
        val passage = state.passage ?: return
        val correct = option.equals(passage.blankWord, ignoreCase = true)
        val points = if (correct) FITB_POINTS else 0
        _uiState.update {
            it.copy(
                selectedOption = option,
                correct = correct,
                pointsEarned = points
            )
        }
        viewModelScope.launch {
            recordAttemptUseCase(
                mode = GameMode.FILL_IN_THE_BLANK,
                passageId = passage.id,
                correct = correct,
                pointsEarned = points,
                isDaily = true
            )
        }
    }

    fun finish() {
        val state = _uiState.value
        if (!state.hasAnswered || state.isFinished) return
        _uiState.update { it.copy(isFinished = true) }
        viewModelScope.launch {
            val r = completeDailyFitbUseCase()
            if (r is Result.Success) {
                _uiState.update { it.copy(streakAfter = r.data.currentStreak) }
            }
        }
    }
}
