package com.grace.app.presentation.screens.games.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.BibleQuestion
import com.grace.app.domain.model.GameDifficulty
import com.grace.app.domain.model.QuestionCategory
import com.grace.app.domain.usecase.games.GetAllQuestionsUseCase
import com.grace.app.domain.usecase.games.SaveQuestionUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditQuestionUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isNew: Boolean = true,
    val id: String = "",
    val category: QuestionCategory = QuestionCategory.NEW_TESTAMENT,
    val difficulty: GameDifficulty = GameDifficulty.EASY,
    val question: String = "",
    val options: List<String> = listOf("", "", "", ""),
    val correctIndex: Int = 0,
    val explanation: String = "",
    val sourceRef: String = "",
    val isActive: Boolean = true,
    val error: String? = null
)

sealed class EditQuestionEvent {
    data class CategoryChanged(val v: QuestionCategory) : EditQuestionEvent()
    data class DifficultyChanged(val v: GameDifficulty) : EditQuestionEvent()
    data class QuestionChanged(val v: String) : EditQuestionEvent()
    data class OptionChanged(val index: Int, val v: String) : EditQuestionEvent()
    data class CorrectChanged(val index: Int) : EditQuestionEvent()
    data class ExplanationChanged(val v: String) : EditQuestionEvent()
    data class SourceRefChanged(val v: String) : EditQuestionEvent()
    data class ActiveChanged(val v: Boolean) : EditQuestionEvent()
    data object Save : EditQuestionEvent()
    data object DismissError : EditQuestionEvent()
}

sealed class EditQuestionEffect {
    data object Saved : EditQuestionEffect()
    data class ShowError(val message: String) : EditQuestionEffect()
}

@HiltViewModel
class EditQuestionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllQuestions: GetAllQuestionsUseCase,
    private val saveQuestion: SaveQuestionUseCase
) : ViewModel() {

    private val arg: String = savedStateHandle["questionId"] ?: "new"

    private val _uiState = MutableStateFlow(EditQuestionUiState(isNew = arg == "new"))
    val uiState: StateFlow<EditQuestionUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<EditQuestionEffect>()
    val effect = _effect.asSharedFlow()

    init { if (arg != "new") loadExisting(arg) }

    fun onEvent(event: EditQuestionEvent) {
        when (event) {
            is EditQuestionEvent.CategoryChanged ->
                _uiState.update { it.copy(category = event.v) }
            is EditQuestionEvent.DifficultyChanged ->
                _uiState.update { it.copy(difficulty = event.v) }
            is EditQuestionEvent.QuestionChanged ->
                _uiState.update { it.copy(question = event.v) }
            is EditQuestionEvent.OptionChanged ->
                _uiState.update { s ->
                    s.copy(options = s.options.toMutableList().also {
                        it[event.index] = event.v
                    })
                }
            is EditQuestionEvent.CorrectChanged ->
                _uiState.update { it.copy(correctIndex = event.index) }
            is EditQuestionEvent.ExplanationChanged ->
                _uiState.update { it.copy(explanation = event.v) }
            is EditQuestionEvent.SourceRefChanged ->
                _uiState.update { it.copy(sourceRef = event.v) }
            is EditQuestionEvent.ActiveChanged ->
                _uiState.update { it.copy(isActive = event.v) }
            EditQuestionEvent.Save -> save()
            EditQuestionEvent.DismissError ->
                _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = getAllQuestions()
            val q = (res as? Result.Success)?.data?.firstOrNull { it.id == id }
            if (q != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isNew = false,
                        id = q.id,
                        category = q.category,
                        difficulty = q.difficulty,
                        question = q.question,
                        options = q.options.toList().let { o ->
                            if (o.size == 4) o else (o + List(4 - o.size) { "" }).take(4)
                        },
                        correctIndex = q.correctIndex.coerceIn(0, 3),
                        explanation = q.explanation.orEmpty(),
                        sourceRef = q.sourceRef.orEmpty(),
                        isActive = q.isActive
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false,
                        error = "Could not load question.")
                }
            }
        }
    }

    private fun save() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val toSave = BibleQuestion(
                id = s.id,
                category = s.category,
                difficulty = s.difficulty,
                question = s.question.trim(),
                options = s.options.map { it.trim() },
                correctIndex = s.correctIndex,
                explanation = s.explanation.trim().ifBlank { null },
                sourceRef = s.sourceRef.trim().ifBlank { null },
                isActive = s.isActive
            )
            when (val r = saveQuestion(toSave)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _effect.emit(EditQuestionEffect.Saved)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = r.message) }
                    _effect.emit(EditQuestionEffect.ShowError(r.message))
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun MutableStateFlow<EditQuestionUiState>.update(
        block: (EditQuestionUiState) -> EditQuestionUiState
    ) { value = block(value) }
}
