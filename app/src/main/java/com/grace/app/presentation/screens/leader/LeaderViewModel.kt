package com.grace.app.presentation.screens.leader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.User
import com.grace.app.domain.repository.LeaderRepository
import com.grace.app.domain.usecase.leader.GetAllLeadersUseCase
import com.grace.app.domain.usecase.leader.GetMyLeaderUseCase
import com.grace.app.domain.usecase.leader.SubmitCheckInUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderUiState(
    val myLeader: User? = null,
    val allLeaders: List<User> = emptyList(),
    val checkInStep: Int = 0,
    val checkInAnswers: List<String> = listOf("", "", ""),
    val checkInDone: Boolean = false,
    val isSubmittingCheckIn: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed interface LeaderEvent {
    data class CheckInAnswerChanged(val step: Int, val text: String) : LeaderEvent
    data object NextCheckInStep : LeaderEvent
    data object PreviousCheckInStep : LeaderEvent
    data object SubmitCheckIn : LeaderEvent
    data object DismissError : LeaderEvent
}

sealed interface LeaderEffect {
    data class ShowError(val message: String) : LeaderEffect
}

private val checkInQuestions = listOf(
    "How's your faith walk this week? (1 = struggling, 5 = thriving)",
    "What's your biggest struggle right now?",
    "What can your leader specifically pray for you this week?"
)

@HiltViewModel
class LeaderViewModel @Inject constructor(
    private val getMyLeaderUseCase: GetMyLeaderUseCase,
    private val getAllLeadersUseCase: GetAllLeadersUseCase,
    private val submitCheckInUseCase: SubmitCheckInUseCase,
    private val leaderRepository: LeaderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderUiState())
    val uiState: StateFlow<LeaderUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<LeaderEffect>()
    val effect: SharedFlow<LeaderEffect> = _effect.asSharedFlow()

    val questions: List<String> = checkInQuestions

    private var myLeaderJob: Job? = null
    private var allLeadersJob: Job? = null

    init { refresh() }

    fun refresh() {
        myLeaderJob?.cancel()
        myLeaderJob = viewModelScope.launch {
            getMyLeaderUseCase().collect { r ->
                if (r is Result.Success) _uiState.update { it.copy(myLeader = r.data) }
            }
        }
        allLeadersJob?.cancel()
        allLeadersJob = viewModelScope.launch {
            getAllLeadersUseCase().collect { r ->
                when (r) {
                    is Result.Success ->
                        _uiState.update { it.copy(allLeaders = r.data, isLoading = false) }
                    is Result.Error ->
                        _uiState.update { it.copy(isLoading = false, error = r.message) }
                    Result.Loading -> Unit
                }
            }
        }
        viewModelScope.launch {
            val existing = (leaderRepository.getCurrentWeekCheckIn()
                as? Result.Success)?.data
            if (existing != null) {
                _uiState.update {
                    it.copy(
                        checkInDone = true,
                        checkInAnswers = listOf(
                            existing["q1"].orEmpty(),
                            existing["q2"].orEmpty(),
                            existing["q3"].orEmpty()
                        )
                    )
                }
            }
        }
    }

    fun onEvent(event: LeaderEvent) {
        when (event) {
            is LeaderEvent.CheckInAnswerChanged -> _uiState.update {
                val updated = it.checkInAnswers.toMutableList()
                updated[event.step] = event.text
                it.copy(checkInAnswers = updated)
            }
            LeaderEvent.NextCheckInStep -> _uiState.update {
                it.copy(checkInStep = (it.checkInStep + 1).coerceAtMost(2))
            }
            LeaderEvent.PreviousCheckInStep -> _uiState.update {
                it.copy(checkInStep = (it.checkInStep - 1).coerceAtLeast(0))
            }
            LeaderEvent.SubmitCheckIn -> submit()
            LeaderEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun submit() {
        val s = _uiState.value
        if (s.isSubmittingCheckIn) return
        if (s.checkInAnswers.any { it.isBlank() }) {
            _uiState.update {
                it.copy(error = "Please answer all three questions before submitting.")
            }
            return
        }
        _uiState.update { it.copy(isSubmittingCheckIn = true, error = null) }
        viewModelScope.launch {
            val answers = mapOf(
                "q1" to s.checkInAnswers[0],
                "q2" to s.checkInAnswers[1],
                "q3" to s.checkInAnswers[2]
            )
            when (val r = submitCheckInUseCase(answers)) {
                is Result.Success ->
                    _uiState.update {
                        it.copy(isSubmittingCheckIn = false, checkInDone = true)
                    }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isSubmittingCheckIn = false, error = r.message)
                    }
                    _effect.emit(LeaderEffect.ShowError(r.message))
                }
                Result.Loading -> Unit
            }
        }
    }
}
