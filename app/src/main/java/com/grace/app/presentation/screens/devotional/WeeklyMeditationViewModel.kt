package com.grace.app.presentation.screens.devotional

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.WeeklyMeditation
import com.grace.app.domain.usecase.meditation.FindMyMeditationSubmissionUseCase
import com.grace.app.domain.usecase.meditation.GetCurrentWeekMeditationUseCase
import com.grace.app.domain.usecase.meditation.SubmitMeditationReflectionUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeeklyMeditationUiState(
    val isLoading: Boolean = true,
    val meditation: WeeklyMeditation? = null,
    val mySubmission: MeditationSubmission? = null,
    val reflectionText: String = "",
    val isDirty: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean
        get() = reflectionText.trim().isNotEmpty() && !isSaving
}

sealed interface WeeklyMeditationEvent {
    data class ReflectionChanged(val text: String) : WeeklyMeditationEvent
    data object Submit : WeeklyMeditationEvent
    data object DismissError : WeeklyMeditationEvent
}

sealed interface WeeklyMeditationEffect {
    data object Saved : WeeklyMeditationEffect
    data class ShowError(val message: String) : WeeklyMeditationEffect
}

@HiltViewModel
class WeeklyMeditationViewModel @Inject constructor(
    private val getCurrentWeekMeditationUseCase: GetCurrentWeekMeditationUseCase,
    private val findMyMeditationSubmissionUseCase: FindMyMeditationSubmissionUseCase,
    private val submitMeditationReflectionUseCase: SubmitMeditationReflectionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeeklyMeditationUiState())
    val uiState: StateFlow<WeeklyMeditationUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<WeeklyMeditationEffect>()
    val effect: SharedFlow<WeeklyMeditationEffect> = _effect.asSharedFlow()

    init { observeCurrent() }

    private fun observeCurrent() {
        viewModelScope.launch {
            getCurrentWeekMeditationUseCase()
                .distinctUntilChangedBy { it?.id }
                .collect { meditation ->
                    if (meditation == null) {
                        _uiState.update {
                            it.copy(
                                meditation = null,
                                mySubmission = null,
                                isLoading = false
                            )
                        }
                        return@collect
                    }
                    val mine = runCatching {
                        findMyMeditationSubmissionUseCase(meditation.id)
                    }.getOrNull()
                    _uiState.update {
                        it.copy(
                            meditation = meditation,
                            mySubmission = mine,
                            reflectionText = if (it.isDirty) it.reflectionText
                                else mine?.reflectionText.orEmpty(),
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun onEvent(event: WeeklyMeditationEvent) {
        when (event) {
            is WeeklyMeditationEvent.ReflectionChanged ->
                _uiState.update {
                    it.copy(reflectionText = event.text, isDirty = true)
                }
            WeeklyMeditationEvent.Submit -> submit()
            WeeklyMeditationEvent.DismissError ->
                _uiState.update { it.copy(error = null) }
        }
    }

    private fun submit() {
        val s = _uiState.value
        val meditationId = s.meditation?.id ?: return
        if (!s.canSubmit) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            when (val r = submitMeditationReflectionUseCase(
                meditationId, s.reflectionText
            )) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, isDirty = false) }
                    val mine = runCatching {
                        findMyMeditationSubmissionUseCase(meditationId)
                    }.getOrNull()
                    _uiState.update { it.copy(mySubmission = mine) }
                    _effect.emit(WeeklyMeditationEffect.Saved)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isSaving = false, error = r.message)
                    }
                    _effect.emit(WeeklyMeditationEffect.ShowError(r.message))
                }
                Result.Loading -> Unit
            }
        }
    }
}
