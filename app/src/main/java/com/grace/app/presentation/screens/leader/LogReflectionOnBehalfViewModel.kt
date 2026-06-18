package com.grace.app.presentation.screens.leader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.WeeklyMeditation
import com.grace.app.domain.repository.LeaderRepository
import com.grace.app.domain.usecase.meditation.GetAllMeditationsUseCase
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
import java.time.LocalDate
import javax.inject.Inject

data class LogReflectionOnBehalfUiState(
    val isLoading: Boolean = true,
    val meditations: List<WeeklyMeditation> = emptyList(),
    val selectedMeditationId: String? = null,
    val reflectionText: String = "",
    val isSubmitting: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean
        get() = selectedMeditationId != null &&
            reflectionText.trim().length >= 3 &&
            !isSubmitting
}

sealed interface LogReflectionOnBehalfEvent {
    data class MeditationSelected(val id: String) : LogReflectionOnBehalfEvent
    data class ReflectionChanged(val v: String) : LogReflectionOnBehalfEvent
    data object Submit : LogReflectionOnBehalfEvent
    data object DismissError : LogReflectionOnBehalfEvent
}

sealed interface LogReflectionOnBehalfEffect {
    data object Logged : LogReflectionOnBehalfEffect
}

@HiltViewModel
class LogReflectionOnBehalfViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val leaderRepository: LeaderRepository,
    private val getAllMeditationsUseCase: GetAllMeditationsUseCase
) : ViewModel() {

    private val memberId: String = savedStateHandle.get<String>("memberId").orEmpty()

    private val _uiState = MutableStateFlow(LogReflectionOnBehalfUiState())
    val uiState: StateFlow<LogReflectionOnBehalfUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<LogReflectionOnBehalfEffect>()
    val effect: SharedFlow<LogReflectionOnBehalfEffect> = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            val meds = runCatching { getAllMeditationsUseCase().first() }
                .getOrDefault(emptyList())
                .sortedByDescending { it.startDate }
            val today = LocalDate.now()
            val defaultId = meds.firstOrNull { it.isCurrent(today) }?.id
                ?: meds.firstOrNull()?.id
            _uiState.update {
                it.copy(
                    isLoading = false,
                    meditations = meds,
                    selectedMeditationId = defaultId
                )
            }
        }
    }

    fun onEvent(event: LogReflectionOnBehalfEvent) {
        when (event) {
            is LogReflectionOnBehalfEvent.MeditationSelected ->
                _uiState.update { it.copy(selectedMeditationId = event.id) }
            is LogReflectionOnBehalfEvent.ReflectionChanged ->
                _uiState.update { it.copy(reflectionText = event.v) }
            LogReflectionOnBehalfEvent.DismissError ->
                _uiState.update { it.copy(error = null) }
            LogReflectionOnBehalfEvent.Submit -> submit()
        }
    }

    private fun submit() {
        val s = _uiState.value
        if (!s.canSubmit) return
        val mid = s.selectedMeditationId ?: return
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val r = leaderRepository.logReflectionOnBehalf(
                memberId = memberId,
                meditationId = mid,
                reflectionText = s.reflectionText
            )
            when (r) {
                is Result.Success -> {
                    _effect.emit(LogReflectionOnBehalfEffect.Logged)
                    _uiState.update { it.copy(isSubmitting = false) }
                }
                is Result.Error -> _uiState.update {
                    it.copy(isSubmitting = false, error = r.message)
                }
                else -> _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
