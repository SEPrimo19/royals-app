package com.grace.app.presentation.screens.discipleship

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.ActivityCategory
import com.grace.app.domain.model.DurationTag
import com.grace.app.domain.usecase.discipleship.CreateActivityUseCase
import com.grace.app.domain.usecase.discipleship.ListAllActivitiesUseCase
import com.grace.app.domain.usecase.discipleship.UpdateActivityUseCase
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

data class AuthorUiState(
    val activityId: String? = null,
    val title: String = "",
    val description: String = "",
    val category: ActivityCategory = ActivityCategory.CHARACTER,
    val durationTag: DurationTag = DurationTag.FIFTEEN,
    val isActive: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val isEdit: Boolean get() = activityId != null
    val canSubmit: Boolean
        get() = title.trim().length in 1..80 &&
            description.trim().length in 1..600 &&
            !isSaving
}

sealed interface AuthorEvent {
    data class TitleChanged(val v: String) : AuthorEvent
    data class DescChanged(val v: String) : AuthorEvent
    data class CategoryChanged(val v: ActivityCategory) : AuthorEvent
    data class DurationChanged(val v: DurationTag) : AuthorEvent
    data class ActiveChanged(val v: Boolean) : AuthorEvent
    data object Save : AuthorEvent
}

sealed interface AuthorEffect {
    data object Saved : AuthorEffect
    data class Error(val message: String) : AuthorEffect
}

@HiltViewModel
class DiscipleshipAuthorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val listAll: ListAllActivitiesUseCase,
    private val create: CreateActivityUseCase,
    private val update: UpdateActivityUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(
        AuthorUiState(
            activityId = savedStateHandle.get<String>("activityId")
                ?.takeIf { it != "new" }
        )
    )
    val uiState: StateFlow<AuthorUiState> = _ui.asStateFlow()

    private val _effect = MutableSharedFlow<AuthorEffect>()
    val effect: SharedFlow<AuthorEffect> = _effect.asSharedFlow()

    init {
        _ui.value.activityId?.let { id ->
            viewModelScope.launch {
                _ui.update { it.copy(isLoading = true) }
                when (val r = listAll()) {
                    is Result.Success -> {
                        val a = r.data.firstOrNull { it.id == id }
                        if (a != null) {
                            _ui.update {
                                it.copy(
                                    title = a.title,
                                    description = a.description,
                                    category = a.category,
                                    durationTag = a.durationTag,
                                    isActive = a.isActive,
                                    isLoading = false
                                )
                            }
                        } else {
                            _ui.update {
                                it.copy(isLoading = false, error = "Activity not found.")
                            }
                        }
                    }
                    is Result.Error ->
                        _ui.update { it.copy(isLoading = false, error = r.message) }
                    Result.Loading -> Unit
                }
            }
        }
    }

    fun onEvent(e: AuthorEvent) {
        when (e) {
            is AuthorEvent.TitleChanged -> _ui.update { it.copy(title = e.v.take(80)) }
            is AuthorEvent.DescChanged -> _ui.update { it.copy(description = e.v.take(600)) }
            is AuthorEvent.CategoryChanged -> _ui.update { it.copy(category = e.v) }
            is AuthorEvent.DurationChanged -> _ui.update { it.copy(durationTag = e.v) }
            is AuthorEvent.ActiveChanged -> _ui.update { it.copy(isActive = e.v) }
            AuthorEvent.Save -> save()
        }
    }

    private fun save() {
        val s = _ui.value
        if (!s.canSubmit) return
        viewModelScope.launch {
            _ui.update { it.copy(isSaving = true, error = null) }
            val r = if (s.isEdit) {
                update(s.activityId!!, s.title, s.description, s.category, s.durationTag, s.isActive)
            } else {
                create(s.title, s.description, s.category, s.durationTag)
            }
            _ui.update { it.copy(isSaving = false) }
            when (r) {
                is Result.Success -> _effect.emit(AuthorEffect.Saved)
                is Result.Error -> {
                    _ui.update { it.copy(error = r.message) }
                    _effect.emit(AuthorEffect.Error(r.message))
                }
                Result.Loading -> Unit
            }
        }
    }
}
