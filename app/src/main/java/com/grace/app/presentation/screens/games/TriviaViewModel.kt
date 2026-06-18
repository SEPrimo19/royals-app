package com.grace.app.presentation.screens.games

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.BibleQuestion
import com.grace.app.domain.model.GameDifficulty
import com.grace.app.domain.model.GameMode
import com.grace.app.domain.model.LifelineKind
import com.grace.app.domain.model.LifelinesState
import com.grace.app.domain.usecase.games.CompleteDailyChallengeUseCase
import com.grace.app.domain.usecase.games.GetDailyChallengeUseCase
import com.grace.app.domain.usecase.games.GetLifelinesUseCase
import com.grace.app.domain.usecase.games.GetPracticeQuestionsUseCase
import com.grace.app.domain.usecase.games.RecordAttemptUseCase
import com.grace.app.domain.usecase.games.UseLifelineUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun parseModeArg(raw: String?): Pair<Boolean, GameDifficulty?> = when (raw) {
    "daily-easy" -> true to GameDifficulty.EASY
    "daily-medium" -> true to GameDifficulty.MEDIUM
    "daily-hard" -> true to GameDifficulty.HARD
    else -> false to null
}

data class TriviaUiState(
    val isDaily: Boolean = true,
    val difficulty: GameDifficulty? = null,
    val isLoading: Boolean = true,
    val questions: List<BibleQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedOption: Int? = null,
    val correctCount: Int = 0,
    val pointsEarned: Int = 0,
    val livesRemaining: Int = MAX_LIVES,
    val timerSeconds: Int = QUESTION_DURATION_S,
    val timedOut: Boolean = false,
    val timerFrozen: Boolean = false,
    val eliminatedIndices: Set<Int> = emptySet(),
    val lifelines: LifelinesState = LifelinesState(),
    val lifelineError: String? = null,
    val isFinished: Boolean = false,
    val finishedReason: FinishReason? = null,
    val streakAfter: Int? = null,
    val error: String? = null
) {
    val currentQuestion: BibleQuestion? get() = questions.getOrNull(currentIndex)
    val progressLabel: String
        get() = when {
            isDaily && questions.isNotEmpty() ->
                "${(currentIndex + 1).coerceAtMost(questions.size)} of ${questions.size}"
            !isDaily -> "Q${currentIndex + 1}"
            else -> ""
        }
    val hasAnswered: Boolean get() = selectedOption != null || timedOut
    val isLastQuestion: Boolean
        get() = isDaily && questions.isNotEmpty() && currentIndex == questions.lastIndex
}

enum class FinishReason { COMPLETED, OUT_OF_LIVES, NO_QUESTIONS }

private const val MAX_LIVES = 5
private const val QUESTION_DURATION_S = 15

@HiltViewModel
class TriviaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDailyChallengeUseCase: GetDailyChallengeUseCase,
    private val getPracticeQuestionsUseCase: GetPracticeQuestionsUseCase,
    private val recordAttemptUseCase: RecordAttemptUseCase,
    private val completeDailyChallengeUseCase: CompleteDailyChallengeUseCase,
    private val getLifelinesUseCase: GetLifelinesUseCase,
    private val useLifelineUseCase: UseLifelineUseCase
) : ViewModel() {

    private val isDaily: Boolean
    private val difficulty: GameDifficulty?

    init {
        val parsed = parseModeArg(savedStateHandle.get<String>("mode"))
        isDaily = parsed.first
        difficulty = parsed.second
    }

    private val _uiState = MutableStateFlow(
        TriviaUiState(isDaily = isDaily, difficulty = difficulty)
    )
    val uiState: StateFlow<TriviaUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    init {
        loadQuestions()
        loadLifelines()
    }

    private fun loadLifelines() {
        viewModelScope.launch {
            val r = getLifelinesUseCase()
            if (r is Result.Success) {
                _uiState.update { it.copy(lifelines = r.data) }
            }
        }
    }

    fun useJoshua() {
        val s = _uiState.value
        if (s.isDaily) return
        if (s.hasAnswered) return
        if (s.timerFrozen) return
        if (s.lifelines.joshuaRemaining <= 0) {
            _uiState.update { it.copy(lifelineError = "No Joshua Effects left today.") }
            return
        }
        timerJob?.cancel()
        _uiState.update { it.copy(timerFrozen = true) }
        viewModelScope.launch {
            when (val r = useLifelineUseCase(LifelineKind.JOSHUA)) {
                is Result.Success -> _uiState.update { it.copy(lifelines = r.data) }
                is Result.Error -> _uiState.update {
                    it.copy(timerFrozen = false, lifelineError = r.message)
                }.also { startTimer() }
                Result.Loading -> Unit
            }
        }
    }

    fun useDaniel() {
        val s = _uiState.value
        val q = s.currentQuestion ?: return
        if (s.hasAnswered) return
        if (s.eliminatedIndices.isNotEmpty()) return
        if (s.lifelines.danielRemaining <= 0) {
            _uiState.update { it.copy(lifelineError = "No Daniel Effects left today.") }
            return
        }
        val wrongs = q.options.indices.filter { it != q.correctIndex }
        val toEliminate = wrongs.shuffled().take(2).toSet()
        _uiState.update { it.copy(eliminatedIndices = toEliminate) }
        viewModelScope.launch {
            when (val r = useLifelineUseCase(LifelineKind.DANIEL)) {
                is Result.Success -> _uiState.update { it.copy(lifelines = r.data) }
                is Result.Error -> _uiState.update {
                    it.copy(eliminatedIndices = emptySet(), lifelineError = r.message)
                }
                Result.Loading -> Unit
            }
        }
    }

    fun dismissLifelineError() {
        _uiState.update { it.copy(lifelineError = null) }
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = if (isDaily) {
                getDailyChallengeUseCase(difficulty!!)
            } else {
                getPracticeQuestionsUseCase(count = 500)
            }
            when (result) {
                is Result.Success -> {
                    if (result.data.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isFinished = true,
                                finishedReason = FinishReason.NO_QUESTIONS,
                                error = "No questions available yet."
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, questions = result.data)
                        }
                        if (!isDaily) startTimer()
                    }
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = result.message)
                }
                Result.Loading -> Unit
            }
        }
    }


    private fun startTimer() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(timerSeconds = QUESTION_DURATION_S, timedOut = false)
        }
        timerJob = viewModelScope.launch {
            while (_uiState.value.timerSeconds > 0) {
                delay(1000L)
                val s = _uiState.value
                if (s.hasAnswered) return@launch
                if (s.timerFrozen) continue
                _uiState.update { it.copy(timerSeconds = it.timerSeconds - 1) }
            }
            timeOutCurrent()
        }
    }

    private fun timeOutCurrent() {
        val state = _uiState.value
        val q = state.currentQuestion ?: return
        if (state.hasAnswered) return
        _uiState.update {
            it.copy(
                timedOut = true,
                livesRemaining = (it.livesRemaining - 1).coerceAtLeast(0)
            )
        }
        viewModelScope.launch {
            recordAttemptUseCase(
                mode = GameMode.TRIVIA,
                questionId = q.id,
                correct = false,
                pointsEarned = 0,
                isDaily = false
            )
        }
    }


    fun selectOption(index: Int) {
        val state = _uiState.value
        if (state.hasAnswered) return
        if (index in state.eliminatedIndices) return
        val q = state.currentQuestion ?: return
        timerJob?.cancel()
        val correct = index == q.correctIndex
        val points = if (correct) q.difficulty.points else 0
        _uiState.update {
            it.copy(
                selectedOption = index,
                correctCount = it.correctCount + (if (correct) 1 else 0),
                pointsEarned = it.pointsEarned + points,
                livesRemaining = if (!it.isDaily && !correct)
                    (it.livesRemaining - 1).coerceAtLeast(0)
                else it.livesRemaining
            )
        }
        viewModelScope.launch {
            recordAttemptUseCase(
                mode = GameMode.TRIVIA,
                questionId = q.id,
                correct = correct,
                pointsEarned = points,
                isDaily = isDaily
            )
        }
    }

    fun next() {
        val state = _uiState.value
        if (!state.hasAnswered) return
        if (!isDaily && state.livesRemaining <= 0) {
            finishRound(FinishReason.OUT_OF_LIVES)
            return
        }
        if (state.isLastQuestion) {
            finishRound(FinishReason.COMPLETED)
            return
        }
        if (isDaily) {
            _uiState.update {
                it.copy(
                    currentIndex = it.currentIndex + 1,
                    selectedOption = null,
                    timedOut = false,
                    timerSeconds = QUESTION_DURATION_S,
                    timerFrozen = false,
                    eliminatedIndices = emptySet()
                )
            }
        } else {
            val nextIdx = state.currentIndex + 1
            if (nextIdx >= state.questions.size) {
                val lastSeen = state.currentQuestion
                val reshuffled = state.questions.shuffled().let { list ->
                    if (list.firstOrNull()?.id == lastSeen?.id && list.size > 1)
                        list.drop(1) + list.first()
                    else list
                }
                _uiState.update {
                    it.copy(
                        questions = reshuffled,
                        currentIndex = 0,
                        selectedOption = null,
                        timedOut = false,
                        timerSeconds = QUESTION_DURATION_S,
                        timerFrozen = false,
                        eliminatedIndices = emptySet()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        currentIndex = nextIdx,
                        selectedOption = null,
                        timedOut = false,
                        timerSeconds = QUESTION_DURATION_S,
                        timerFrozen = false,
                        eliminatedIndices = emptySet()
                    )
                }
            }
            startTimer()
        }
    }

    private fun finishRound(reason: FinishReason) {
        timerJob?.cancel()
        _uiState.update { it.copy(isFinished = true, finishedReason = reason) }
        if (isDaily && difficulty != null) {
            viewModelScope.launch {
                val r = completeDailyChallengeUseCase(difficulty)
                if (r is Result.Success) {
                    _uiState.update { it.copy(streakAfter = r.data.currentStreak) }
                }
            }
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
