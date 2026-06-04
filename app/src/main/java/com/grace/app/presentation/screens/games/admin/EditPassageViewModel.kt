package com.grace.app.presentation.screens.games.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.BiblePassage
import com.grace.app.domain.usecase.games.GetAllPassagesUseCase
import com.grace.app.domain.usecase.games.SavePassageUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditPassageUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isNew: Boolean = true,
    val id: String = "",
    val reference: String = "",
    val text: String = "",
    val blankWord: String = "",
    val distractor1: String = "",
    val distractor2: String = "",
    val distractor3: String = "",
    val isActive: Boolean = true,
    val error: String? = null
)

sealed class EditPassageEvent {
    data class ReferenceChanged(val v: String) : EditPassageEvent()
    data class TextChanged(val v: String) : EditPassageEvent()
    data class BlankWordChanged(val v: String) : EditPassageEvent()
    data class Distractor1Changed(val v: String) : EditPassageEvent()
    data class Distractor2Changed(val v: String) : EditPassageEvent()
    data class Distractor3Changed(val v: String) : EditPassageEvent()
    data class ActiveChanged(val v: Boolean) : EditPassageEvent()
    data object Save : EditPassageEvent()
    data object DismissError : EditPassageEvent()
}

sealed class EditPassageEffect {
    data object Saved : EditPassageEffect()
    data class ShowError(val message: String) : EditPassageEffect()
}

@HiltViewModel
class EditPassageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllPassages: GetAllPassagesUseCase,
    private val savePassage: SavePassageUseCase
) : ViewModel() {

    private val arg: String = savedStateHandle["passageId"] ?: "new"

    private val _uiState = MutableStateFlow(EditPassageUiState(isNew = arg == "new"))
    val uiState: StateFlow<EditPassageUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<EditPassageEffect>()
    val effect = _effect.asSharedFlow()

    init { if (arg != "new") loadExisting(arg) }

    fun onEvent(event: EditPassageEvent) {
        when (event) {
            is EditPassageEvent.ReferenceChanged ->
                _uiState.update { it.copy(reference = event.v) }
            is EditPassageEvent.TextChanged ->
                _uiState.update { it.copy(text = event.v) }
            is EditPassageEvent.BlankWordChanged ->
                _uiState.update { it.copy(blankWord = event.v) }
            is EditPassageEvent.Distractor1Changed ->
                _uiState.update { it.copy(distractor1 = event.v) }
            is EditPassageEvent.Distractor2Changed ->
                _uiState.update { it.copy(distractor2 = event.v) }
            is EditPassageEvent.Distractor3Changed ->
                _uiState.update { it.copy(distractor3 = event.v) }
            is EditPassageEvent.ActiveChanged ->
                _uiState.update { it.copy(isActive = event.v) }
            EditPassageEvent.Save -> save()
            EditPassageEvent.DismissError ->
                _uiState.update { it.copy(error = null) }
        }
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val res = getAllPassages()
            val p = (res as? Result.Success)?.data?.firstOrNull { it.id == id }
            if (p != null) {
                val d = p.distractors.toList().let {
                    if (it.size == 3) it else (it + List(3 - it.size) { "" }).take(3)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isNew = false,
                        id = p.id,
                        reference = p.reference,
                        text = p.text,
                        blankWord = p.blankWord,
                        distractor1 = d[0],
                        distractor2 = d[1],
                        distractor3 = d[2],
                        isActive = p.isActive
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false,
                        error = "Could not load verse.")
                }
            }
        }
    }

    private fun save() {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val toSave = BiblePassage(
                id = s.id,
                reference = s.reference.trim(),
                text = s.text.trim(),
                blankWord = s.blankWord.trim(),
                distractors = listOf(s.distractor1, s.distractor2, s.distractor3)
                    .map { it.trim() },
                isActive = s.isActive
            )
            when (val r = savePassage(toSave)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _effect.emit(EditPassageEffect.Saved)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isSaving = false, error = r.message) }
                    _effect.emit(EditPassageEffect.ShowError(r.message))
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun MutableStateFlow<EditPassageUiState>.update(
        block: (EditPassageUiState) -> EditPassageUiState
    ) { value = block(value) }
}
