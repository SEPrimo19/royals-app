package com.grace.app.presentation.screens.leader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.CheckIn
import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.Mentee
import com.grace.app.domain.model.WeeklyMeditation
import com.grace.app.domain.usecase.leader.GetMemberCheckInUseCase
import com.grace.app.domain.usecase.leader.GetMyMenteesUseCase
import com.grace.app.domain.usecase.meditation.GetAllMeditationsUseCase
import com.grace.app.domain.usecase.meditation.GetMeditationSubmissionsForUserUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberReflectionItem(
    val submission: MeditationSubmission,
    val meditation: WeeklyMeditation?
)

data class MemberDetailUiState(
    val isLoading: Boolean = true,
    val mentee: Mentee? = null,
    val latestCheckIn: CheckIn? = null,
    val reflections: List<MemberReflectionItem> = emptyList(),
    val isLoadingReflections: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMyMenteesUseCase: GetMyMenteesUseCase,
    private val getMemberCheckInUseCase: GetMemberCheckInUseCase,
    private val getMeditationSubmissionsForUserUseCase: GetMeditationSubmissionsForUserUseCase,
    private val getAllMeditationsUseCase: GetAllMeditationsUseCase
) : ViewModel() {

    private val memberId: String = savedStateHandle.get<String>("memberId").orEmpty()

    private val _uiState = MutableStateFlow(MemberDetailUiState())
    val uiState: StateFlow<MemberDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isLoadingReflections = true) }
            val mentee = (getMyMenteesUseCase() as? Result.Success)?.data
                ?.firstOrNull { it.user.id == memberId }
            val checkIn = (getMemberCheckInUseCase(memberId) as? Result.Success)?.data
            _uiState.update {
                it.copy(isLoading = false, mentee = mentee, latestCheckIn = checkIn)
            }
            val subs = (getMeditationSubmissionsForUserUseCase(memberId)
                as? Result.Success)?.data.orEmpty()
            val meds = runCatching {
                getAllMeditationsUseCase().first()
            }.getOrDefault(emptyList())
            val medsById = meds.associateBy { it.id }
            val items = subs.map { MemberReflectionItem(it, medsById[it.meditationId]) }
            _uiState.update {
                it.copy(reflections = items, isLoadingReflections = false)
            }
        }
    }
}
