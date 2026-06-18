package com.grace.app.presentation.screens.bible

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.usecase.bible.CreateSessionNoteUseCase
import com.grace.app.domain.usecase.bible.DeleteNoteUseCase
import com.grace.app.domain.usecase.bible.GetBibleBooksUseCase
import com.grace.app.domain.usecase.bible.ListBibleNotesUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyNoteListItem(
    val id: String,
    val title: String,
    val preview: String,
    val isSession: Boolean
)

data class MyStudyNotesUiState(
    val isLoading: Boolean = true,
    val notes: List<StudyNoteListItem> = emptyList(),
    val error: String? = null
)

sealed interface MyStudyNotesEffect {
    data class OpenNote(val id: String) : MyStudyNotesEffect
    data class ShowError(val message: String) : MyStudyNotesEffect
}

@HiltViewModel
class MyStudyNotesViewModel @Inject constructor(
    private val listNotes: ListBibleNotesUseCase,
    private val createSessionNote: CreateSessionNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val getBooks: GetBibleBooksUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyStudyNotesUiState())
    val uiState: StateFlow<MyStudyNotesUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<MyStudyNotesEffect>()
    val effect: SharedFlow<MyStudyNotesEffect> = _effect.asSharedFlow()

    private var bookNames: Map<Int, String> = emptyMap()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            if (bookNames.isEmpty()) {
                bookNames = runCatching { getBooks().associate { it.order to it.name } }
                    .getOrDefault(emptyMap())
            }
            when (val r = listNotes()) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoading = false, notes = r.data.map { n ->
                        StudyNoteListItem(
                            id = n.id,
                            title = if (n.isSession) {
                                n.title?.takeIf { t -> t.isNotBlank() } ?: "Untitled note"
                            } else {
                                "${bookNames[n.bookOrder] ?: "Chapter"} ${n.chapter}"
                            },
                            preview = n.content.replace("\n", " ").trim().take(90),
                            isSession = n.isSession
                        )
                    })
                }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, error = r.message)
                }
                Result.Loading -> Unit
            }
        }
    }

    fun createNote(title: String) {
        viewModelScope.launch {
            when (val r = createSessionNote(title.trim().ifBlank { "Untitled note" })) {
                is Result.Success -> {
                    _effect.emit(MyStudyNotesEffect.OpenNote(r.data.id))
                    load()
                }
                is Result.Error -> _effect.emit(MyStudyNotesEffect.ShowError(r.message))
                Result.Loading -> Unit
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            when (val r = deleteNote(id)) {
                is Result.Success -> load()
                is Result.Error -> _effect.emit(MyStudyNotesEffect.ShowError(r.message))
                Result.Loading -> Unit
            }
        }
    }
}
