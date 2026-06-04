package com.grace.app.presentation.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.BibleEvent
import com.grace.app.domain.model.GameMode
import com.grace.app.domain.usecase.games.GetBibleEventsUseCase
import com.grace.app.domain.usecase.games.RecordAttemptUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val TIMELINE_PUZZLES_PER_ROUND = 3
const val TIMELINE_EVENTS_PER_PUZZLE = 5
const val TIMELINE_PUZZLE_POINTS = 40
const val TIMELINE_PERFECT_BONUS = 20
private const val TIMELINE_WRONG_FLASH_MS = 500L

data class TimelineSortUiState(
    val isLoading: Boolean = true,
    val pool: List<BibleEvent> = emptyList(),
    val puzzles: List<List<BibleEvent>> = emptyList(),  // shuffled puzzle sets
    val currentIndex: Int = 0,
    /** Events currently in the bottom pool (tap to place). */
    val poolForPuzzle: List<BibleEvent> = emptyList(),
    /** Events placed in slots, in placement order (= chronological order). */
    val placed: List<BibleEvent> = emptyList(),
    /** Event chip id currently flashing red — for the wrong-tap animation. */
    val wrongFlashEventId: String? = null,
    val wrongTapsThisPuzzle: Int = 0,
    val puzzleComplete: Boolean = false,
    val pointsThisPuzzle: Int = 0,
    val totalPoints: Int = 0,
    val puzzlesCompleted: Int = 0,
    val isRoundFinished: Boolean = false,
    val error: String? = null
) {
    val currentPuzzle: List<BibleEvent>? get() = puzzles.getOrNull(currentIndex)
    val correctOrder: List<BibleEvent>
        get() = currentPuzzle.orEmpty().sortedBy { it.chronologicalOrder }
    val progressLabel: String
        get() = "Puzzle ${(currentIndex + 1).coerceAtMost(puzzles.size)} of ${puzzles.size}"
    /** Eligible for perfect-no-undo bonus while wrongTaps == 0. */
    val isPerfectSoFar: Boolean get() = wrongTapsThisPuzzle == 0
}

@HiltViewModel
class TimelineSortViewModel @Inject constructor(
    private val getBibleEventsUseCase: GetBibleEventsUseCase,
    private val recordAttemptUseCase: RecordAttemptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineSortUiState())
    val uiState: StateFlow<TimelineSortUiState> = _uiState.asStateFlow()

    init { newRound() }

    fun newRound() {
        viewModelScope.launch {
            _uiState.update { TimelineSortUiState(isLoading = true) }
            when (val r = getBibleEventsUseCase(count = 50)) {
                is Result.Success -> {
                    val pool = r.data
                    val needed = TIMELINE_PUZZLES_PER_ROUND * TIMELINE_EVENTS_PER_PUZZLE
                    if (pool.size < needed) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Need at least $needed events in the pool."
                            )
                        }
                        return@launch
                    }
                    // Build N distinct puzzles by sampling without replacement
                    // from the pool — keeps puzzles within a single round
                    // from showing the same event twice.
                    val shuffled = pool.shuffled().toMutableList()
                    val puzzles = mutableListOf<List<BibleEvent>>()
                    repeat(TIMELINE_PUZZLES_PER_ROUND) {
                        val slice = shuffled
                            .take(TIMELINE_EVENTS_PER_PUZZLE)
                            .sortedBy { it.chronologicalOrder }   // store canonical
                        puzzles += slice
                        repeat(TIMELINE_EVENTS_PER_PUZZLE) { shuffled.removeAt(0) }
                    }
                    _uiState.update {
                        it.copy(isLoading = false, pool = pool, puzzles = puzzles)
                    }
                    setupPuzzleAt(0)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = r.message)
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun setupPuzzleAt(index: Int) {
        val puzzle = _uiState.value.puzzles.getOrNull(index) ?: return
        // Shuffle the canonical-order puzzle into a random order for the pool.
        val scrambled = puzzle.shuffled()
        _uiState.update {
            it.copy(
                currentIndex = index,
                poolForPuzzle = scrambled,
                placed = emptyList(),
                wrongFlashEventId = null,
                wrongTapsThisPuzzle = 0,
                puzzleComplete = false,
                pointsThisPuzzle = 0
            )
        }
    }

    /** Tap an event chip in the pool. Strict top-to-bottom placement. */
    fun onEventTapped(event: BibleEvent) {
        val s = _uiState.value
        if (s.puzzleComplete) return
        if (s.wrongFlashEventId != null) return       // flash in flight
        if (event !in s.poolForPuzzle) return         // stale tap

        val expectedNext = s.correctOrder.getOrNull(s.placed.size) ?: return
        if (event.id == expectedNext.id) {
            val newPool = s.poolForPuzzle - event
            val newPlaced = s.placed + event
            val complete = newPlaced.size == s.correctOrder.size
            val pts = if (complete) {
                TIMELINE_PUZZLE_POINTS +
                    if (s.isPerfectSoFar) TIMELINE_PERFECT_BONUS else 0
            } else 0
            _uiState.update {
                it.copy(
                    poolForPuzzle = newPool,
                    placed = newPlaced,
                    puzzleComplete = complete,
                    pointsThisPuzzle = pts
                )
            }
            if (complete) onPuzzleCompleted(pts)
        } else {
            _uiState.update {
                it.copy(
                    wrongFlashEventId = event.id,
                    wrongTapsThisPuzzle = it.wrongTapsThisPuzzle + 1
                )
            }
            viewModelScope.launch {
                delay(TIMELINE_WRONG_FLASH_MS)
                _uiState.update { it.copy(wrongFlashEventId = null) }
            }
        }
    }

    /** Tap a placed event to pop it (and everything after) back. */
    fun onPlacedTapped(event: BibleEvent) {
        val s = _uiState.value
        if (s.puzzleComplete) return
        if (event !in s.placed) return
        val idx = s.placed.indexOf(event)
        val keptPlaced = s.placed.take(idx)
        val poppedChips = s.placed.drop(idx)
        _uiState.update {
            it.copy(
                placed = keptPlaced,
                poolForPuzzle = it.poolForPuzzle + poppedChips
            )
        }
    }

    private fun onPuzzleCompleted(points: Int) {
        // Timeline puzzles don't have a single content-FK target — each
        // puzzle is a generated set of events — so we leave all content
        // ids null on the game_attempts row. Per the [[who-am-i-shipped]]
        // memory, this is acceptable for v1 since the leaderboard only
        // aggregates by user.
        viewModelScope.launch {
            recordAttemptUseCase(
                mode = GameMode.TIMELINE_SORT,
                correct = true,
                pointsEarned = points,
                isDaily = false
            )
        }
        _uiState.update {
            it.copy(
                totalPoints = it.totalPoints + points,
                puzzlesCompleted = it.puzzlesCompleted + 1
            )
        }
    }

    fun next() {
        val s = _uiState.value
        if (!s.puzzleComplete) return
        val nextIdx = s.currentIndex + 1
        if (nextIdx >= s.puzzles.size) {
            _uiState.update { it.copy(isRoundFinished = true) }
        } else {
            setupPuzzleAt(nextIdx)
        }
    }
}
