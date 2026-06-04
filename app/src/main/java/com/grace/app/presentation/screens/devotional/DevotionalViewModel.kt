package com.grace.app.presentation.screens.devotional

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.local.dao.UserDevoProgressDao
import com.grace.app.domain.model.Devotional
import com.grace.app.domain.repository.DevotionalRepository
import com.grace.app.domain.usecase.devotional.GetTodayDevotionalUseCase
import com.grace.app.domain.usecase.devotional.MarkDevotionalCompleteUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevotionalUiState(
    val devotional: Devotional? = null,
    val currentStep: Int = 0, // 0=Scripture 1=Reflection 2=Prayer 3=Journal
    val isLoading: Boolean = true,
    val isDone: Boolean = false,
    val streakCount: Int = 0,
    val journalText: String = "",
    val isMarkingComplete: Boolean = false,
    // View-only: re-reading an already-completed devotional. Does NOT touch the
    // user_devo_progress row, so completion / streak stay intact.
    val isReReading: Boolean = false,
    val error: String? = null,
    val isOfflineCached: Boolean = false
) {
    // Once complete, progress stays 100% — re-reading never drops it to 0%.
    val progress: Float get() = if (isDone) 1f else (currentStep / 3f).coerceIn(0f, 1f)
}

sealed interface DevotionalEvent {
    data object NextStep : DevotionalEvent
    data object PreviousStep : DevotionalEvent
    data class GoToStep(val step: Int) : DevotionalEvent
    data class JournalTextChanged(val text: String) : DevotionalEvent
    data object MarkComplete : DevotionalEvent
    data object ReadAgain : DevotionalEvent
    data object BackToSummary : DevotionalEvent
    data object DismissError : DevotionalEvent
    data object RetryLoad : DevotionalEvent
}

sealed interface DevotionalEffect {
    data class ShowCompletionCelebration(val newStreak: Int) : DevotionalEffect
    data class ShowError(val message: String) : DevotionalEffect
}

@HiltViewModel
class DevotionalViewModel @Inject constructor(
    private val getTodayDevotionalUseCase: GetTodayDevotionalUseCase,
    private val markDevotionalCompleteUseCase: MarkDevotionalCompleteUseCase,
    private val devotionalRepository: DevotionalRepository,
    private val progressDao: UserDevoProgressDao,
    private val prefs: UserPreferencesRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(DevotionalUiState())
    val uiState: StateFlow<DevotionalUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<DevotionalEffect>()
    val effect: SharedFlow<DevotionalEffect> = _effect.asSharedFlow()

    init {
        observeDevotional()
        observeStreak()
        observeDone()
    }

    /**
     * Mirror today's progress row into [DevotionalUiState.isDone]. Without this
     * the Devo screen would always open at step 0 even when Home shows 100% —
     * because in-session completion was the ONLY thing that flipped isDone.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeDone() {
        viewModelScope.launch {
            combine(
                prefs.userId,
                _uiState.asStateFlow()
            ) { uid, state -> uid to state.devotional?.id }
                .distinctUntilChanged()
                .flatMapLatest { (uid, devoId) ->
                    if (uid.isNullOrBlank() || devoId.isNullOrBlank()) flowOf(null)
                    else progressDao.observeProgress(uid, devoId)
                }
                .collect { progress ->
                    _uiState.update { it.copy(isDone = progress != null) }
                }
        }
    }

    private fun observeDevotional() {
        viewModelScope.launch {
            getTodayDevotionalUseCase().collect { result ->
                when (result) {
                    is Result.Success -> _uiState.update {
                        it.copy(
                            devotional = result.data,
                            isLoading = false,
                            error = null
                        )
                    }
                    is Result.Error -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            // Cached copy still on screen → soft offline state.
                            isOfflineCached = it.devotional != null,
                            error = if (it.devotional == null) result.message else null
                        )
                    }
                    Result.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun observeStreak() {
        viewModelScope.launch {
            devotionalRepository.getStreak().collect { streak ->
                _uiState.update { it.copy(streakCount = streak) }
            }
        }
    }

    fun onEvent(event: DevotionalEvent) {
        when (event) {
            DevotionalEvent.NextStep -> _uiState.update {
                it.copy(currentStep = (it.currentStep + 1).coerceAtMost(3))
            }
            DevotionalEvent.PreviousStep -> _uiState.update {
                it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0))
            }
            is DevotionalEvent.GoToStep -> _uiState.update {
                it.copy(currentStep = event.step.coerceIn(0, 3))
            }
            is DevotionalEvent.JournalTextChanged -> _uiState.update {
                it.copy(journalText = event.text)
            }
            DevotionalEvent.DismissError -> _uiState.update { it.copy(error = null) }
            DevotionalEvent.RetryLoad -> {
                _uiState.update { it.copy(isLoading = true, error = null) }
                observeDevotional()
            }
            DevotionalEvent.MarkComplete -> markComplete()
            // Re-read: leave isDone (and the progress row) alone, just show the
            // step content again starting at Scripture.
            DevotionalEvent.ReadAgain -> _uiState.update {
                it.copy(isReReading = true, currentStep = 0)
            }
            DevotionalEvent.BackToSummary -> _uiState.update {
                it.copy(isReReading = false)
            }
        }
    }

    private fun markComplete() {
        val state = _uiState.value
        val devo = state.devotional ?: return
        if (state.isMarkingComplete || state.isDone) return
        _uiState.update { it.copy(isMarkingComplete = true, error = null) }
        viewModelScope.launch {
            when (val result =
                markDevotionalCompleteUseCase(devo.id, state.journalText)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isMarkingComplete = false, isDone = true) }
                    _effect.emit(
                        DevotionalEffect.ShowCompletionCelebration(
                            _uiState.value.streakCount
                        )
                    )
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isMarkingComplete = false, error = result.message)
                    }
                    _effect.emit(DevotionalEffect.ShowError(result.message))
                }
                Result.Loading -> Unit
            }
        }
    }
}
