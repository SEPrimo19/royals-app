package com.grace.app.presentation.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.BibleCharacter
import com.grace.app.domain.model.GameMode
import com.grace.app.domain.model.LifelineKind
import com.grace.app.domain.model.LifelinesState
import com.grace.app.domain.model.pointsForCluesUsed
import com.grace.app.domain.usecase.games.GetLifelinesUseCase
import com.grace.app.domain.usecase.games.GetWhoAmICharactersUseCase
import com.grace.app.domain.usecase.games.RecordAttemptUseCase
import com.grace.app.domain.usecase.games.UseLifelineUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Per-round options shape — locked once the character loads so the
 * order of MCQ buttons stays stable across recompositions.
 */
data class WhoAmIOptions(
    val options: List<String>,
    val correctIndex: Int
)

data class WhoAmIUiState(
    val isLoading: Boolean = true,
    val characters: List<BibleCharacter> = emptyList(),
    val currentIndex: Int = 0,
    val options: WhoAmIOptions? = null,
    /** Clues revealed so far: 1..4. Player always sees at least the first. */
    val cluesRevealed: Int = 1,
    /** Wrong attempts so far (out of 4 maximum). */
    val wrongAttempts: Int = 0,
    val selectedOption: Int? = null,
    val hasAnswered: Boolean = false,
    val isCorrect: Boolean = false,
    val pointsEarned: Int = 0,
    val totalPoints: Int = 0,
    val correctCount: Int = 0,
    /** Total characters played in this session (correct + bombed). */
    val charactersPlayed: Int = 0,
    /** Like Trivia Practice — 5 lives, each character bombed (0 pts) costs 1. */
    val livesRemaining: Int = MAX_LIVES_WAI,
    /** Daniel Effect — option indices greyed out for this character. */
    val eliminatedIndices: Set<Int> = emptySet(),
    val lifelines: LifelinesState = LifelinesState(),
    val lifelineError: String? = null,
    val isFinished: Boolean = false,
    val error: String? = null
) {
    val currentCharacter: BibleCharacter? get() = characters.getOrNull(currentIndex)
    val progressLabel: String get() = "Round ${currentIndex + 1}"
}

const val MAX_LIVES_WAI = 5

@HiltViewModel
class WhoAmIViewModel @Inject constructor(
    private val getWhoAmICharactersUseCase: GetWhoAmICharactersUseCase,
    private val recordAttemptUseCase: RecordAttemptUseCase,
    private val getLifelinesUseCase: GetLifelinesUseCase,
    private val useLifelineUseCase: UseLifelineUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhoAmIUiState())
    val uiState: StateFlow<WhoAmIUiState> = _uiState.asStateFlow()

    init {
        loadCharacters()
        loadLifelines()
    }

    private fun loadLifelines() {
        viewModelScope.launch {
            val r = getLifelinesUseCase()
            if (r is com.grace.app.domain.util.Result.Success) {
                _uiState.update { it.copy(lifelines = r.data) }
            }
        }
    }

    /**
     * 🕯️ Daniel Effect — 50/50 over the 4 MCQ options. Joshua doesn't
     * apply here (no timer). Per-character single-use.
     */
    fun useDaniel() {
        val s = _uiState.value
        val opts = s.options ?: return
        if (s.hasAnswered) return
        if (s.eliminatedIndices.isNotEmpty()) return
        if (s.lifelines.danielRemaining <= 0) {
            _uiState.update { it.copy(lifelineError = "No Daniel Effects left today.") }
            return
        }
        val wrongs = opts.options.indices.filter { it != opts.correctIndex }
        val toEliminate = wrongs.shuffled().take(2).toSet()
        _uiState.update { it.copy(eliminatedIndices = toEliminate) }
        viewModelScope.launch {
            when (val r = useLifelineUseCase(LifelineKind.DANIEL)) {
                is com.grace.app.domain.util.Result.Success ->
                    _uiState.update { it.copy(lifelines = r.data) }
                is com.grace.app.domain.util.Result.Error ->
                    _uiState.update {
                        it.copy(eliminatedIndices = emptySet(), lifelineError = r.message)
                    }
                com.grace.app.domain.util.Result.Loading -> Unit
            }
        }
    }

    fun dismissLifelineError() {
        _uiState.update { it.copy(lifelineError = null) }
    }

    private fun loadCharacters() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = getWhoAmICharactersUseCase(count = 100)) {
                is Result.Success -> {
                    if (r.data.isEmpty()) {
                        _uiState.update {
                            it.copy(isLoading = false, error = "No characters available yet.")
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                characters = r.data,
                                currentIndex = 0,
                                options = buildOptions(r.data.first()),
                                cluesRevealed = 1,
                                wrongAttempts = 0,
                                selectedOption = null,
                                hasAnswered = false
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

    /** Tap "Next Clue 🔍" without guessing — peels one more clue. */
    fun revealNextClue() {
        val s = _uiState.value
        if (s.hasAnswered) return
        if (s.cluesRevealed >= 4) return
        _uiState.update { it.copy(cluesRevealed = it.cluesRevealed + 1) }
    }

    /**
     * User picks an MCQ option. Right → end round with score by clues used.
     * Wrong → bump wrongAttempts, peel next clue (if any), let them try
     * again. Out of attempts (4) → reveal answer with 0 pts.
     */
    fun selectOption(index: Int) {
        val s = _uiState.value
        val q = s.currentCharacter ?: return
        val opts = s.options ?: return
        if (s.hasAnswered) return
        if (index in s.eliminatedIndices) return    // Daniel: dead option

        val correct = index == opts.correctIndex
        if (correct) {
            val cluesUsed = s.cluesRevealed.coerceAtLeast(1)
            val points = pointsForCluesUsed(cluesUsed)
            _uiState.update {
                it.copy(
                    selectedOption = index,
                    hasAnswered = true,
                    isCorrect = true,
                    pointsEarned = points,
                    totalPoints = it.totalPoints + points,
                    correctCount = it.correctCount + 1,
                    charactersPlayed = it.charactersPlayed + 1
                )
            }
            recordAttempt(q.id, correct = true, points = points)
        } else {
            val nextAttempts = s.wrongAttempts + 1
            val maxedOut = nextAttempts >= 4
            if (maxedOut) {
                // Bombed this character — costs 1 life. Don't decrement
                // here; do it in [next] so the user sees the reveal first
                // and lives drop atomically with the round transition.
                _uiState.update {
                    it.copy(
                        selectedOption = index,
                        wrongAttempts = nextAttempts,
                        cluesRevealed = 4,
                        hasAnswered = true,
                        isCorrect = false,
                        pointsEarned = 0,
                        charactersPlayed = it.charactersPlayed + 1
                    )
                }
                recordAttempt(q.id, correct = false, points = 0)
            } else {
                // Reveal another clue if any remain, allow retry.
                _uiState.update {
                    it.copy(
                        wrongAttempts = nextAttempts,
                        cluesRevealed = (it.cluesRevealed + 1).coerceAtMost(4),
                        selectedOption = null  // clear so the user can pick again
                    )
                }
            }
        }
    }

    /** Move to the next character; reshuffle the pool when exhausted. */
    fun next() {
        val s = _uiState.value
        if (!s.hasAnswered) return
        // If they bombed this character, spend a life. Then check whether
        // the run is over BEFORE setting up the next round.
        val livesAfter = if (!s.isCorrect)
            (s.livesRemaining - 1).coerceAtLeast(0)
        else s.livesRemaining
        if (livesAfter <= 0) {
            _uiState.update {
                it.copy(livesRemaining = 0, isFinished = true)
            }
            return
        }
        val nextIdx = s.currentIndex + 1
        if (nextIdx >= s.characters.size) {
            val lastSeen = s.currentCharacter
            val reshuffled = s.characters.shuffled().let { list ->
                if (list.firstOrNull()?.id == lastSeen?.id && list.size > 1)
                    list.drop(1) + list.first()
                else list
            }
            _uiState.update {
                it.copy(
                    characters = reshuffled,
                    currentIndex = 0,
                    options = buildOptions(reshuffled.first()),
                    cluesRevealed = 1,
                    wrongAttempts = 0,
                    selectedOption = null,
                    hasAnswered = false,
                    isCorrect = false,
                    pointsEarned = 0,
                    livesRemaining = livesAfter,
                    eliminatedIndices = emptySet()
                )
            }
        } else {
            _uiState.update {
                val nextChar = it.characters[nextIdx]
                it.copy(
                    currentIndex = nextIdx,
                    options = buildOptions(nextChar),
                    cluesRevealed = 1,
                    wrongAttempts = 0,
                    selectedOption = null,
                    hasAnswered = false,
                    isCorrect = false,
                    pointsEarned = 0,
                    livesRemaining = livesAfter,
                    eliminatedIndices = emptySet()
                )
            }
        }
    }

    /** Reset everything and re-fetch the pool — used by "Play Again". */
    fun restart() {
        _uiState.update {
            WhoAmIUiState(isLoading = true)
        }
        loadCharacters()
    }

    private fun recordAttempt(characterId: String, correct: Boolean, points: Int) {
        viewModelScope.launch {
            // The repo writes a game_attempts row with mode = "who_am_i".
            // isDaily = false because v1 ships Practice-only.
            // NOTE: pass via characterId (FK to bible_characters), NOT
            // questionId — that column FKs to bible_questions and would
            // silently fail every Who-Am-I insert (the bug fixed in v10).
            recordAttemptUseCase(
                mode = GameMode.WHO_AM_I,
                characterId = characterId,
                correct = correct,
                pointsEarned = points,
                isDaily = false
            )
        }
    }

    /** Stable per-character MCQ ordering: shuffled once when the round opens. */
    private fun buildOptions(character: BibleCharacter): WhoAmIOptions {
        val opts = character.shuffledOptions()
        val correctIndex = opts.indexOf(character.name).coerceAtLeast(0)
        return WhoAmIOptions(options = opts, correctIndex = correctIndex)
    }
}
