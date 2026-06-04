package com.grace.app.presentation.screens.games.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.BiblePassage
import com.grace.app.domain.model.BibleQuestion
import com.grace.app.domain.usecase.games.GetAllPassagesUseCase
import com.grace.app.domain.usecase.games.GetAllQuestionsUseCase
import com.grace.app.domain.usecase.games.SetPassageActiveUseCase
import com.grace.app.domain.usecase.games.SetQuestionActiveUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ManageTab { TRIVIA, VERSES }

data class ManageContentUiState(
    val tab: ManageTab = ManageTab.TRIVIA,
    val isLoading: Boolean = false,
    val questions: List<BibleQuestion> = emptyList(),
    val passages: List<BiblePassage> = emptyList(),
    val error: String? = null
)

sealed class ManageContentEvent {
    data class TabChanged(val tab: ManageTab) : ManageContentEvent()
    data object Refresh : ManageContentEvent()
    data class ToggleQuestionActive(val id: String, val newValue: Boolean) : ManageContentEvent()
    data class TogglePassageActive(val id: String, val newValue: Boolean) : ManageContentEvent()
    data object DismissError : ManageContentEvent()
}

sealed class ManageContentEffect {
    data class ShowError(val message: String) : ManageContentEffect()
    data class ShowToast(val message: String) : ManageContentEffect()
}

@HiltViewModel
class ManageContentViewModel @Inject constructor(
    private val getAllQuestions: GetAllQuestionsUseCase,
    private val getAllPassages: GetAllPassagesUseCase,
    private val setQuestionActive: SetQuestionActiveUseCase,
    private val setPassageActive: SetPassageActiveUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageContentUiState())
    val uiState: StateFlow<ManageContentUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ManageContentEffect>()
    val effect = _effect.asSharedFlow()

    init { refresh() }

    fun onEvent(event: ManageContentEvent) {
        when (event) {
            is ManageContentEvent.TabChanged -> {
                _uiState.update { it.copy(tab = event.tab) }
            }
            ManageContentEvent.Refresh -> refresh()
            is ManageContentEvent.ToggleQuestionActive -> toggleQuestion(event.id, event.newValue)
            is ManageContentEvent.TogglePassageActive -> togglePassage(event.id, event.newValue)
            ManageContentEvent.DismissError -> {
                _uiState.update { it.copy(error = null) }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val qRes = getAllQuestions()
            val pRes = getAllPassages()
            val questions = (qRes as? Result.Success)?.data ?: emptyList()
            val passages = (pRes as? Result.Success)?.data ?: emptyList()
            val err = (qRes as? Result.Error)?.message ?: (pRes as? Result.Error)?.message
            _uiState.update {
                it.copy(
                    isLoading = false,
                    questions = questions,
                    passages = passages,
                    error = err
                )
            }
            if (err != null) _effect.emit(ManageContentEffect.ShowError(err))
        }
    }

    private fun toggleQuestion(id: String, newValue: Boolean) {
        viewModelScope.launch {
            when (val r = setQuestionActive(id, newValue)) {
                is Result.Success -> {
                    _uiState.update { s ->
                        s.copy(questions = s.questions.map {
                            if (it.id == id) it.copy(isActive = newValue) else it
                        })
                    }
                    _effect.emit(ManageContentEffect.ShowToast(
                        if (newValue) "Question activated" else "Question hidden"
                    ))
                }
                is Result.Error -> _effect.emit(ManageContentEffect.ShowError(r.message))
                Result.Loading -> Unit
            }
        }
    }

    private fun togglePassage(id: String, newValue: Boolean) {
        viewModelScope.launch {
            when (val r = setPassageActive(id, newValue)) {
                is Result.Success -> {
                    _uiState.update { s ->
                        s.copy(passages = s.passages.map {
                            if (it.id == id) it.copy(isActive = newValue) else it
                        })
                    }
                    _effect.emit(ManageContentEffect.ShowToast(
                        if (newValue) "Verse activated" else "Verse hidden"
                    ))
                }
                is Result.Error -> _effect.emit(ManageContentEffect.ShowError(r.message))
                Result.Loading -> Unit
            }
        }
    }

    private fun MutableStateFlow<ManageContentUiState>.update(
        block: (ManageContentUiState) -> ManageContentUiState
    ) { value = block(value) }
}
