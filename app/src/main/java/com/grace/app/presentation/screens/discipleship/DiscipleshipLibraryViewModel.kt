package com.grace.app.presentation.screens.discipleship

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.domain.model.ActivityCategory
import com.grace.app.domain.model.DiscipleshipActivity
import com.grace.app.domain.usecase.discipleship.DeleteActivityUseCase
import com.grace.app.domain.usecase.discipleship.ListAllActivitiesUseCase
import com.grace.app.domain.usecase.discipleship.ListTodaysCompletedIdsUseCase
import com.grace.app.domain.usecase.discipleship.MarkActivityCompletedUseCase
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

data class LibraryUiState(
    val isLoading: Boolean = true,
    val activities: List<DiscipleshipActivity> = emptyList(),
    val completedTodayIds: Set<String> = emptySet(),
    val filter: ActivityCategory? = null,
    val myRole: String = "member",
    val error: String? = null,
    val pendingDelete: DiscipleshipActivity? = null
) {
    val visible: List<DiscipleshipActivity>
        get() = filter?.let { f -> activities.filter { it.category == f } } ?: activities
    val canManage: Boolean
        get() = myRole == "cell_leader" || myRole == "council" ||
            myRole == "youth_president" || myRole == "pastor" || myRole == "admin"
}

sealed interface LibraryEvent {
    data object Refresh : LibraryEvent
    data class FilterChanged(val v: ActivityCategory?) : LibraryEvent
    data class MarkDone(val activityId: String) : LibraryEvent
    data class PromptDelete(val activity: DiscipleshipActivity) : LibraryEvent
    data object CancelDelete : LibraryEvent
    data object ConfirmDelete : LibraryEvent
}

sealed interface LibraryEffect {
    data class Toast(val message: String, val isError: Boolean) : LibraryEffect
}

@HiltViewModel
class DiscipleshipLibraryViewModel @Inject constructor(
    private val listAll: ListAllActivitiesUseCase,
    private val listCompletedToday: ListTodaysCompletedIdsUseCase,
    private val markDone: MarkActivityCompletedUseCase,
    private val deleteAct: DeleteActivityUseCase,
    private val prefs: UserPreferencesRepo
) : ViewModel() {

    private val _ui = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _ui.asStateFlow()

    private val _effect = MutableSharedFlow<LibraryEffect>()
    val effect: SharedFlow<LibraryEffect> = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            _ui.update { it.copy(myRole = prefs.userRole.first() ?: "member") }
            load()
        }
    }

    fun onEvent(e: LibraryEvent) {
        when (e) {
            LibraryEvent.Refresh -> load()
            is LibraryEvent.FilterChanged ->
                _ui.update { it.copy(filter = e.v) }
            is LibraryEvent.MarkDone -> doMarkDone(e.activityId)
            is LibraryEvent.PromptDelete ->
                _ui.update { it.copy(pendingDelete = e.activity) }
            LibraryEvent.CancelDelete ->
                _ui.update { it.copy(pendingDelete = null) }
            LibraryEvent.ConfirmDelete -> doDelete()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            val done = (listCompletedToday() as? Result.Success)?.data ?: emptySet()
            when (val r = listAll()) {
                is Result.Success ->
                    _ui.update {
                        it.copy(
                            isLoading = false,
                            activities = r.data,
                            completedTodayIds = done
                        )
                    }
                is Result.Error ->
                    _ui.update { it.copy(isLoading = false, error = r.message) }
                Result.Loading -> Unit
            }
        }
    }

    private fun doMarkDone(activityId: String) {
        viewModelScope.launch {
            when (val r = markDone(activityId, null)) {
                is Result.Success -> {
                    _ui.update {
                        it.copy(completedTodayIds = it.completedTodayIds + activityId)
                    }
                    _effect.emit(LibraryEffect.Toast("Marked done ✓", false))
                }
                is Result.Error ->
                    _effect.emit(LibraryEffect.Toast(r.message, true))
                Result.Loading -> Unit
            }
        }
    }

    private fun doDelete() {
        val target = _ui.value.pendingDelete ?: return
        _ui.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            when (val r = deleteAct(target.id)) {
                is Result.Success -> {
                    _effect.emit(LibraryEffect.Toast("Activity removed.", false))
                    load()
                }
                is Result.Error ->
                    _effect.emit(LibraryEffect.Toast(r.message, true))
                Result.Loading -> Unit
            }
        }
    }
}
