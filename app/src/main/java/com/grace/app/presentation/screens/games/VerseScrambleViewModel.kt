package com.grace.app.presentation.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.BibleVerseScramble
import com.grace.app.domain.model.GameMode
import com.grace.app.domain.usecase.games.GetVerseScramblesUseCase
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

const val VERSES_PER_ROUND = 5
const val VERSE_POINTS = 30
const val VERSE_PERFECT_BONUS = 10
private const val WRONG_FLASH_MS = 500L

/**
 * One word chip. [originalIndex] is the chip's identity (so we can tell
 * "the" from "the" if a verse repeats a word). [text] is the displayed
 * word.
 */
data class WordChip(
    val originalIndex: Int,   // position in the correct word list
    val text: String
)

data class VerseScrambleUiState(
    val isLoading: Boolean = true,
    val round: List<BibleVerseScramble> = emptyList(),
    val currentIndex: Int = 0,
    /** Chips currently in the bottom pool (tap to place). */
    val pool: List<WordChip> = emptyList(),
    /** Chips placed in slots, in placement order. */
    val placed: List<WordChip> = emptyList(),
    /** The chip that just got flashed red — UI uses this for the flash animation. */
    val wrongFlashChipIndex: Int? = null,
    val wrongTapsThisVerse: Int = 0,
    val verseComplete: Boolean = false,
    val pointsThisVerse: Int = 0,
    val totalPoints: Int = 0,
    val versesCompleted: Int = 0,
    val isRoundFinished: Boolean = false,
    val error: String? = null
) {
    val currentVerse: BibleVerseScramble? get() = round.getOrNull(currentIndex)
    val correctWords: List<String> get() = currentVerse?.correctWords.orEmpty()
    val progressLabel: String
        get() = "Verse ${(currentIndex + 1).coerceAtMost(round.size)} of ${round.size}"
    /** True while no wrong taps have happened on this verse — eligible for bonus. */
    val isPerfectSoFar: Boolean get() = wrongTapsThisVerse == 0
}

@HiltViewModel
class VerseScrambleViewModel @Inject constructor(
    private val getVerseScramblesUseCase: GetVerseScramblesUseCase,
    private val recordAttemptUseCase: RecordAttemptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(VerseScrambleUiState())
    val uiState: StateFlow<VerseScrambleUiState> = _uiState.asStateFlow()

    init { newRound() }

    fun newRound() {
        viewModelScope.launch {
            _uiState.update { VerseScrambleUiState(isLoading = true) }
            when (val r = getVerseScramblesUseCase(count = 25, maxWordCount = 12)) {
                is Result.Success -> {
                    val verses = r.data
                    if (verses.size < VERSES_PER_ROUND) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Need at least $VERSES_PER_ROUND short verses."
                            )
                        }
                        return@launch
                    }
                    val round = verses.shuffled().take(VERSES_PER_ROUND)
                    _uiState.update {
                        it.copy(isLoading = false, round = round)
                    }
                    setupVerseAt(0)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = r.message)
                }
                Result.Loading -> Unit
            }
        }
    }

    /** Build chip pool for the verse at [index] (shuffled words). */
    private fun setupVerseAt(index: Int) {
        val verse = _uiState.value.round.getOrNull(index) ?: return
        val chips = verse.correctWords
            .mapIndexed { i, w -> WordChip(originalIndex = i, text = w) }
            .shuffled()
        _uiState.update {
            it.copy(
                currentIndex = index,
                pool = chips,
                placed = emptyList(),
                wrongFlashChipIndex = null,
                wrongTapsThisVerse = 0,
                verseComplete = false,
                pointsThisVerse = 0
            )
        }
    }

    /**
     * Player tapped a chip in the pool. Strict left-to-right: if the chip
     * is the next expected word it slots in; otherwise it flashes red,
     * counts as a wrong tap, and stays in the pool.
     */
    fun onChipTapped(chip: WordChip) {
        val s = _uiState.value
        if (s.verseComplete) return
        if (s.wrongFlashChipIndex != null) return  // flash in flight
        if (chip !in s.pool) return                // ignore stale taps

        val nextExpectedIndex = s.placed.size  // next slot to fill
        if (chip.originalIndex == nextExpectedIndex) {
            // Correct. Move chip from pool to placed.
            val newPool = s.pool - chip
            val newPlaced = s.placed + chip
            val complete = newPlaced.size == s.correctWords.size
            val pts = if (complete) {
                VERSE_POINTS + if (s.isPerfectSoFar) VERSE_PERFECT_BONUS else 0
            } else 0
            _uiState.update {
                it.copy(
                    pool = newPool,
                    placed = newPlaced,
                    verseComplete = complete,
                    pointsThisVerse = pts
                )
            }
            if (complete) onVerseCompleted(pts)
        } else {
            // Wrong. Flash the chip briefly, bump counter, leave in pool.
            _uiState.update {
                it.copy(
                    wrongFlashChipIndex = chip.originalIndex,
                    wrongTapsThisVerse = it.wrongTapsThisVerse + 1
                )
            }
            viewModelScope.launch {
                delay(WRONG_FLASH_MS)
                _uiState.update { it.copy(wrongFlashChipIndex = null) }
            }
        }
    }

    /** Tap a placed chip to take it back to the pool (in case they tap the
     *  wrong correct word and want to course-correct). Free action, no
     *  penalty. */
    fun onPlacedChipTapped(chip: WordChip) {
        val s = _uiState.value
        if (s.verseComplete) return
        if (chip !in s.placed) return
        // Pop everything after this chip's position too, because the slots
        // are filled in order. Simpler model than re-indexing.
        val idx = s.placed.indexOf(chip)
        val keptPlaced = s.placed.take(idx)
        val poppedChips = s.placed.drop(idx)
        _uiState.update {
            it.copy(
                placed = keptPlaced,
                pool = it.pool + poppedChips
            )
        }
    }

    private fun onVerseCompleted(points: Int) {
        val s = _uiState.value
        val verse = s.currentVerse ?: return
        // Record an attempt row for the leaderboard / monthly sum.
        viewModelScope.launch {
            recordAttemptUseCase(
                mode = GameMode.VERSE_SCRAMBLE,
                scrambleId = verse.id,
                correct = true,
                pointsEarned = points,
                isDaily = false
            )
        }
        _uiState.update {
            it.copy(
                totalPoints = it.totalPoints + points,
                versesCompleted = it.versesCompleted + 1
            )
        }
    }

    /** Advance after the user taps "Next verse →". Ends round at last verse. */
    fun next() {
        val s = _uiState.value
        if (!s.verseComplete) return
        val nextIdx = s.currentIndex + 1
        if (nextIdx >= s.round.size) {
            _uiState.update { it.copy(isRoundFinished = true) }
        } else {
            setupVerseAt(nextIdx)
        }
    }
}
