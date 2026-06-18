package com.grace.app.presentation.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.domain.model.UserNote
import com.grace.app.domain.usecase.notes.DeleteMyNoteUseCase
import com.grace.app.domain.usecase.notes.GetVisibleNotesUseCase
import com.grace.app.domain.usecase.notes.HideNoteUseCase
import com.grace.app.domain.usecase.notes.PostMyNoteUseCase
import com.grace.app.domain.usecase.notes.SubscribeToNoteChangesUseCase
import com.grace.app.domain.usecase.notes.ToggleNoteHeartUseCase
import com.grace.app.domain.usecase.notes.UnhideNoteUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesBarUiState(
    val myUserId: String? = null,
    val myRole: String = "member",
    val notes: List<UserNote> = emptyList(),
    val isLoading: Boolean = true,
    val composerOpen: Boolean = false,
    val viewing: UserNote? = null,
    val error: String? = null
) {
    val myNote: UserNote? get() = notes.firstOrNull { it.userId == myUserId }
    val isLeaderTier: Boolean
        get() = myRole == "cell_leader" || myRole == "council" ||
            myRole == "youth_president" || myRole == "pastor" || myRole == "admin"
}

sealed interface NotesBarEvent {
    data object Refresh : NotesBarEvent
    data object OpenComposer : NotesBarEvent
    data object CloseComposer : NotesBarEvent
    data class PostNote(val content: String) : NotesBarEvent
    data object DeleteMyNote : NotesBarEvent
    data class ViewNote(val note: UserNote) : NotesBarEvent
    data object DismissViewer : NotesBarEvent
    data class ToggleHeart(val noteUserId: String) : NotesBarEvent
    data class HideNote(val noteUserId: String) : NotesBarEvent
    data class UnhideNote(val noteUserId: String) : NotesBarEvent
}

sealed interface NotesBarEffect {
    data class Toast(val message: String, val isError: Boolean) : NotesBarEffect
}

@HiltViewModel
class NotesBarViewModel @Inject constructor(
    private val getVisible: GetVisibleNotesUseCase,
    private val postMyNote: PostMyNoteUseCase,
    private val deleteMyNote: DeleteMyNoteUseCase,
    private val toggleHeart: ToggleNoteHeartUseCase,
    private val hideNote: HideNoteUseCase,
    private val unhideNote: UnhideNoteUseCase,
    private val subscribeToChanges: SubscribeToNoteChangesUseCase,
    private val prefs: UserPreferencesRepo
) : ViewModel() {

    private val _ui = MutableStateFlow(NotesBarUiState())
    val uiState: StateFlow<NotesBarUiState> = _ui.asStateFlow()

    private val _effect = MutableSharedFlow<NotesBarEffect>()
    val effect: SharedFlow<NotesBarEffect> = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            _ui.update {
                it.copy(
                    myUserId = prefs.userId.first(),
                    myRole = prefs.userRole.first() ?: "member"
                )
            }
            load()
        }
        viewModelScope.launch {
            subscribeToChanges().collect { load() }
        }
    }

    fun onEvent(e: NotesBarEvent) {
        when (e) {
            NotesBarEvent.Refresh -> load()
            NotesBarEvent.OpenComposer -> _ui.update { it.copy(composerOpen = true) }
            NotesBarEvent.CloseComposer -> _ui.update { it.copy(composerOpen = false) }
            is NotesBarEvent.PostNote -> doPost(e.content)
            NotesBarEvent.DeleteMyNote -> doDelete()
            is NotesBarEvent.ViewNote -> _ui.update { it.copy(viewing = e.note) }
            NotesBarEvent.DismissViewer -> _ui.update { it.copy(viewing = null) }
            is NotesBarEvent.ToggleHeart -> doHeart(e.noteUserId)
            is NotesBarEvent.HideNote -> doHide(e.noteUserId)
            is NotesBarEvent.UnhideNote -> doUnhide(e.noteUserId)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = it.notes.isEmpty(), error = null) }
            when (val r = getVisible()) {
                is Result.Success ->
                    _ui.update { it.copy(isLoading = false, notes = r.data) }
                is Result.Error ->
                    _ui.update { it.copy(isLoading = false, error = r.message) }
                Result.Loading -> Unit
            }
        }
    }

    private fun doPost(content: String) {
        viewModelScope.launch {
            when (val r = postMyNote(content)) {
                is Result.Success -> {
                    _ui.update { it.copy(composerOpen = false) }
                    _effect.emit(NotesBarEffect.Toast("Posted.", false))
                    load()
                }
                is Result.Error ->
                    _effect.emit(NotesBarEffect.Toast(r.message, true))
                Result.Loading -> Unit
            }
        }
    }

    private fun doDelete() {
        viewModelScope.launch {
            when (val r = deleteMyNote()) {
                is Result.Success -> {
                    _ui.update { it.copy(composerOpen = false) }
                    _effect.emit(NotesBarEffect.Toast("Removed.", false))
                    load()
                }
                is Result.Error ->
                    _effect.emit(NotesBarEffect.Toast(r.message, true))
                Result.Loading -> Unit
            }
        }
    }

    private fun doHeart(noteUserId: String) {
        val before = _ui.value
        _ui.update { s ->
            s.copy(
                notes = s.notes.map {
                    if (it.userId == noteUserId) {
                        val delta = if (it.hasMyHeart) -1 else 1
                        it.copy(
                            hasMyHeart = !it.hasMyHeart,
                            heartCount = (it.heartCount + delta).coerceAtLeast(0)
                        )
                    } else it
                },
                viewing = s.viewing?.let {
                    if (it.userId == noteUserId) {
                        val delta = if (it.hasMyHeart) -1 else 1
                        it.copy(
                            hasMyHeart = !it.hasMyHeart,
                            heartCount = (it.heartCount + delta).coerceAtLeast(0)
                        )
                    } else it
                }
            )
        }
        viewModelScope.launch {
            when (val r = toggleHeart(noteUserId)) {
                is Result.Success -> Unit
                is Result.Error -> {
                    _ui.value = before
                    _effect.emit(NotesBarEffect.Toast(r.message, true))
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun doHide(noteUserId: String) {
        viewModelScope.launch {
            when (val r = hideNote(noteUserId)) {
                is Result.Success -> {
                    _ui.update { it.copy(viewing = null) }
                    _effect.emit(NotesBarEffect.Toast("Note hidden.", false))
                    load()
                }
                is Result.Error ->
                    _effect.emit(NotesBarEffect.Toast(r.message, true))
                Result.Loading -> Unit
            }
        }
    }

    private fun doUnhide(noteUserId: String) {
        viewModelScope.launch {
            when (val r = unhideNote(noteUserId)) {
                is Result.Success -> {
                    _effect.emit(NotesBarEffect.Toast("Note restored.", false))
                    load()
                }
                is Result.Error ->
                    _effect.emit(NotesBarEffect.Toast(r.message, true))
                Result.Loading -> Unit
            }
        }
    }
}
