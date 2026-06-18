package com.grace.app.presentation.screens.bible

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.usecase.bible.DeleteNoteUseCase
import com.grace.app.domain.usecase.bible.GetBibleBooksUseCase
import com.grace.app.domain.usecase.bible.GetBibleNoteUseCase
import com.grace.app.domain.usecase.bible.SaveNoteUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BibleNoteEditorUiState(
    val isLoading: Boolean = true,
    val isSession: Boolean = false,
    val title: String = "",
    val content: String = "",
    val notFound: Boolean = false,
    val error: String? = null
)

sealed interface BibleNoteEditorEffect {
    data object Closed : BibleNoteEditorEffect
    data class ShowError(val message: String) : BibleNoteEditorEffect
}

@HiltViewModel
class BibleNoteEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getNote: GetBibleNoteUseCase,
    private val saveNote: SaveNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val getBooks: GetBibleBooksUseCase
) : ViewModel() {

    private val noteId: String = savedStateHandle.get<String>("noteId").orEmpty()

    private val _uiState = MutableStateFlow(BibleNoteEditorUiState())
    val uiState: StateFlow<BibleNoteEditorUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<BibleNoteEditorEffect>()
    val effect: SharedFlow<BibleNoteEditorEffect> = _effect.asSharedFlow()

    private var saveJob: Job? = null

    init { load() }

    private fun load() {
        viewModelScope.launch {
            when (val r = getNote(noteId)) {
                is Result.Success -> {
                    val note = r.data
                    if (note == null) {
                        _uiState.update { it.copy(isLoading = false, notFound = true) }
                        return@launch
                    }
                    val title = if (note.isSession) {
                        note.title.orEmpty()
                    } else {
                        val name = runCatching {
                            getBooks().firstOrNull { it.order == note.bookOrder }?.name
                        }.getOrNull() ?: "Chapter"
                        "$name ${note.chapter}"
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSession = note.isSession,
                            title = title,
                            content = note.content
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

    fun onTitleChanged(text: String) {
        if (!_uiState.value.isSession) return
        _uiState.update { it.copy(title = text) }
        scheduleSave()
    }

    fun onContentChanged(text: String) {
        _uiState.update { it.copy(content = text) }
        scheduleSave()
    }

    fun addVerse(reference: String, text: String) {
        _uiState.update {
            val addition = "$reference — $text"
            val merged =
                if (it.content.isBlank()) addition
                else it.content.trimEnd() + "\n\n" + addition
            it.copy(content = merged)
        }
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(800)
            val s = _uiState.value
            saveNote(noteId, if (s.isSession) s.title else null, s.content)
        }
    }

    fun delete() {
        viewModelScope.launch {
            when (val r = deleteNote(noteId)) {
                is Result.Success -> _effect.emit(BibleNoteEditorEffect.Closed)
                is Result.Error -> _effect.emit(BibleNoteEditorEffect.ShowError(r.message))
                Result.Loading -> Unit
            }
        }
    }
}
