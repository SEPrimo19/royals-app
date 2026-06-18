package com.grace.app.presentation.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.DiscipleshipActivity
import com.grace.app.domain.usecase.discipleship.GetCellCompletionCountUseCase
import com.grace.app.domain.usecase.discipleship.GetMyDiscipleshipStreakUseCase
import com.grace.app.domain.usecase.discipleship.GetTodaysActivityUseCase
import com.grace.app.domain.usecase.discipleship.IsActivityCompletedTodayUseCase
import com.grace.app.domain.usecase.discipleship.MarkActivityCompletedUseCase
import com.grace.app.domain.usecase.discipleship.SwapTodaysActivityUseCase
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

data class DailyChallengeUiState(
    val isLoading: Boolean = true,
    val activity: DiscipleshipActivity? = null,
    val isCompletedToday: Boolean = false,
    val cellCount: Int = 0,
    val streak: Int = 0,
    val composerOpen: Boolean = false,
    val isWorking: Boolean = false,
    val error: String? = null
)

sealed interface DailyChallengeEvent {
    data object Refresh : DailyChallengeEvent
    data object Swap : DailyChallengeEvent
    data object OpenComposer : DailyChallengeEvent
    data object CloseComposer : DailyChallengeEvent
    data class MarkDone(val reflection: String) : DailyChallengeEvent
}

sealed interface DailyChallengeEffect {
    data class Toast(val message: String, val isError: Boolean) : DailyChallengeEffect
}

@HiltViewModel
class DailyChallengeViewModel @Inject constructor(
    private val getTodays: GetTodaysActivityUseCase,
    private val swap: SwapTodaysActivityUseCase,
    private val markDone: MarkActivityCompletedUseCase,
    private val isCompleted: IsActivityCompletedTodayUseCase,
    private val cellCount: GetCellCompletionCountUseCase,
    private val getStreak: GetMyDiscipleshipStreakUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(DailyChallengeUiState())
    val uiState: StateFlow<DailyChallengeUiState> = _ui.asStateFlow()

    private val _effect = MutableSharedFlow<DailyChallengeEffect>()
    val effect: SharedFlow<DailyChallengeEffect> = _effect.asSharedFlow()


    fun onEvent(e: DailyChallengeEvent) {
        when (e) {
            DailyChallengeEvent.Refresh -> load()
            DailyChallengeEvent.Swap -> doSwap()
            DailyChallengeEvent.OpenComposer -> _ui.update { it.copy(composerOpen = true) }
            DailyChallengeEvent.CloseComposer -> _ui.update { it.copy(composerOpen = false) }
            is DailyChallengeEvent.MarkDone -> doMarkDone(e.reflection)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = it.activity == null, error = null) }
            val pick = getTodays()
            when (pick) {
                is Result.Success -> {
                    val activity = pick.data
                    val done = activity?.let {
                        (isCompleted(it.id) as? Result.Success)?.data ?: false
                    } ?: false
                    val count = (cellCount() as? Result.Success)?.data ?: 0
                    val streak = (getStreak() as? Result.Success)?.data ?: 0
                    _ui.update {
                        it.copy(
                            isLoading = false,
                            activity = activity,
                            isCompletedToday = done,
                            cellCount = count,
                            streak = streak
                        )
                    }
                }
                is Result.Error -> _ui.update {
                    it.copy(isLoading = false, error = pick.message)
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun doSwap() {
        val current = _ui.value.activity ?: return
        viewModelScope.launch {
            _ui.update { it.copy(isWorking = true) }
            swap(current.id)
            val pick = getTodays()
            _ui.update { it.copy(isWorking = false) }
            when (pick) {
                is Result.Success -> {
                    val activity = pick.data
                    val done = activity?.let {
                        (isCompleted(it.id) as? Result.Success)?.data ?: false
                    } ?: false
                    _ui.update {
                        it.copy(activity = activity, isCompletedToday = done)
                    }
                    if (activity == null) {
                        _effect.emit(
                            DailyChallengeEffect.Toast(
                                "No other activities to suggest right now.", true
                            )
                        )
                    }
                }
                is Result.Error -> _effect.emit(
                    DailyChallengeEffect.Toast(pick.message, true)
                )
                Result.Loading -> Unit
            }
        }
    }

    private fun doMarkDone(reflection: String) {
        val activity = _ui.value.activity ?: return
        viewModelScope.launch {
            _ui.update { it.copy(isWorking = true, composerOpen = false) }
            when (val r = markDone(activity.id, reflection.takeIf { it.isNotBlank() })) {
                is Result.Success -> {
                    _ui.update { it.copy(isWorking = false, isCompletedToday = true) }
                    _effect.emit(DailyChallengeEffect.Toast("Marked done ✓", false))
                    val streak = (getStreak() as? Result.Success)?.data ?: _ui.value.streak
                    val count = (cellCount() as? Result.Success)?.data ?: _ui.value.cellCount
                    _ui.update { it.copy(streak = streak, cellCount = count) }
                }
                is Result.Error -> {
                    _ui.update { it.copy(isWorking = false) }
                    _effect.emit(DailyChallengeEffect.Toast(r.message, true))
                }
                Result.Loading -> Unit
            }
        }
    }
}
