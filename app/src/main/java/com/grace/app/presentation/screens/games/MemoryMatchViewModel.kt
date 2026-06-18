package com.grace.app.presentation.screens.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.GameMode
import com.grace.app.domain.model.MemoryCardPair
import com.grace.app.domain.usecase.games.GetMemoryCardPairsUseCase
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

const val MEMORY_PAIRS_PER_BOARD = 6
const val MEMORY_POINTS_PER_PAIR = 10
const val MEMORY_PERFECT_BONUS = 30
private const val MISMATCH_FLIP_BACK_DELAY_MS = 900L

data class MemoryCard(
    val pairId: String,
    val isReference: Boolean,
    val text: String
)

data class MemoryMatchUiState(
    val isLoading: Boolean = true,
    val cards: List<MemoryCard> = emptyList(),
    val faceUpIndices: Set<Int> = emptySet(),
    val matchedIndices: Set<Int> = emptySet(),
    val mismatchCount: Int = 0,
    val matchedPairCount: Int = 0,
    val isEvaluating: Boolean = false,
    val boardComplete: Boolean = false,
    val pointsEarned: Int = 0,
    val pairs: List<MemoryCardPair> = emptyList(),
    val error: String? = null
) {
    val isPerfectSoFar: Boolean get() = mismatchCount == 0
}

@HiltViewModel
class MemoryMatchViewModel @Inject constructor(
    private val getMemoryCardPairsUseCase: GetMemoryCardPairsUseCase,
    private val recordAttemptUseCase: RecordAttemptUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemoryMatchUiState())
    val uiState: StateFlow<MemoryMatchUiState> = _uiState.asStateFlow()

    init { newBoard() }

    fun newBoard() {
        viewModelScope.launch {
            _uiState.update { MemoryMatchUiState(isLoading = true) }
            when (val r = getMemoryCardPairsUseCase(count = 30)) {
                is Result.Success -> {
                    val pool = r.data
                    if (pool.size < MEMORY_PAIRS_PER_BOARD) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Need at least $MEMORY_PAIRS_PER_BOARD pairs in the pool."
                            )
                        }
                        return@launch
                    }
                    val chosen = pool.shuffled().take(MEMORY_PAIRS_PER_BOARD)
                    val deck = buildDeck(chosen)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            cards = deck,
                            pairs = chosen
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

    fun onCardTapped(index: Int) {
        val s = _uiState.value
        if (s.isEvaluating) return
        if (s.boardComplete) return
        if (index in s.matchedIndices) return
        if (index in s.faceUpIndices) return

        val newFaceUp = s.faceUpIndices + index
        _uiState.update { it.copy(faceUpIndices = newFaceUp) }

        if (newFaceUp.size == 2) evaluatePair(newFaceUp.toList())
    }

    private fun evaluatePair(flipped: List<Int>) {
        val s = _uiState.value
        val a = s.cards[flipped[0]]
        val b = s.cards[flipped[1]]
        val isMatch = a.pairId == b.pairId && a.isReference != b.isReference

        if (isMatch) {
            val newMatched = s.matchedIndices + flipped[0] + flipped[1]
            val pairsMatchedNow = s.matchedPairCount + 1
            val complete = pairsMatchedNow >= MEMORY_PAIRS_PER_BOARD
            val pointsNow = pairsMatchedNow * MEMORY_POINTS_PER_PAIR +
                if (complete && s.isPerfectSoFar) MEMORY_PERFECT_BONUS else 0
            _uiState.update {
                it.copy(
                    faceUpIndices = emptySet(),
                    matchedIndices = newMatched,
                    matchedPairCount = pairsMatchedNow,
                    pointsEarned = pointsNow,
                    boardComplete = complete
                )
            }
            recordAttempt(a.pairId, correct = true, points = MEMORY_POINTS_PER_PAIR)
            if (complete && s.isPerfectSoFar) {
                recordAttempt(
                    pairId = null,
                    correct = true,
                    points = MEMORY_PERFECT_BONUS
                )
            }
        } else {
            _uiState.update { it.copy(isEvaluating = true) }
            viewModelScope.launch {
                delay(MISMATCH_FLIP_BACK_DELAY_MS)
                _uiState.update {
                    it.copy(
                        faceUpIndices = emptySet(),
                        mismatchCount = it.mismatchCount + 1,
                        isEvaluating = false
                    )
                }
            }
        }
    }

    private fun recordAttempt(pairId: String?, correct: Boolean, points: Int) {
        viewModelScope.launch {
            recordAttemptUseCase(
                mode = GameMode.MEMORY_MATCH,
                pairId = pairId,
                correct = correct,
                pointsEarned = points,
                isDaily = false
            )
        }
    }

    private fun buildDeck(pairs: List<MemoryCardPair>): List<MemoryCard> {
        val cards = mutableListOf<MemoryCard>()
        pairs.forEach { p ->
            cards += MemoryCard(p.id, isReference = true, text = p.reference)
            cards += MemoryCard(p.id, isReference = false, text = p.verseSnippet)
        }
        return cards.shuffled()
    }
}
