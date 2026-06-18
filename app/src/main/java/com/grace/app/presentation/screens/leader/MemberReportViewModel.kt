package com.grace.app.presentation.screens.leader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.domain.model.ProgressSnapshot
import com.grace.app.domain.repository.LeaderRepository
import com.grace.app.domain.usecase.leader.GetMyMenteesUseCase
import com.grace.app.domain.usecase.meditation.GetAllMeditationsUseCase
import com.grace.app.domain.usecase.meditation.GetMeditationSubmissionsForUserUseCase
import com.grace.app.domain.util.Result
import com.grace.app.presentation.screens.progress.ExportPeriod
import com.grace.app.presentation.screens.progress.MyProgressUiState
import com.grace.app.presentation.screens.progress.ProgressReflectionItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberReportUiState(
    val isLoading: Boolean = true,
    val progressState: MyProgressUiState = MyProgressUiState(),
    val generatedByLeaderName: String = "",
    val memberName: String = "",
    val error: String? = null
)

sealed interface MemberReportEvent {
    data class IncludeAllChanged(val on: Boolean) : MemberReportEvent
    data class IncludeAttendanceChanged(val on: Boolean) : MemberReportEvent
    data class IncludeMeditationChanged(val on: Boolean) : MemberReportEvent
    data class PeriodChanged(val period: ExportPeriod) : MemberReportEvent
}

@HiltViewModel
class MemberReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val leaderRepository: LeaderRepository,
    private val getMyMenteesUseCase: GetMyMenteesUseCase,
    private val getMeditationSubmissionsForUserUseCase: GetMeditationSubmissionsForUserUseCase,
    private val getAllMeditationsUseCase: GetAllMeditationsUseCase,
    private val prefs: UserPreferencesRepo
) : ViewModel() {

    private val memberId: String = savedStateHandle.get<String>("memberId").orEmpty()

    private val _uiState = MutableStateFlow(MemberReportUiState())
    val uiState: StateFlow<MemberReportUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val mentees = (getMyMenteesUseCase() as? Result.Success)?.data.orEmpty()
            val member = mentees.firstOrNull { it.user.id == memberId }?.user

            val attended = (leaderRepository.getMemberAttendance(memberId)
                as? Result.Success)?.data.orEmpty()

            val subs = (getMeditationSubmissionsForUserUseCase(memberId)
                as? Result.Success)?.data.orEmpty()
            val meds = runCatching {
                getAllMeditationsUseCase().first()
            }.getOrDefault(emptyList())
            val medsById = meds.associateBy { it.id }
            val reflections = subs.map { ProgressReflectionItem(it, medsById[it.meditationId]) }

            val leaderName = prefs.userName.first().orEmpty()

            val snapshot = ProgressSnapshot()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    memberName = member?.name?.takeIf { n -> n.isNotBlank() }
                        ?: member?.email
                        ?: "Member",
                    generatedByLeaderName = leaderName,
                    progressState = MyProgressUiState(
                        isLoading = false,
                        snapshot = snapshot,
                        attendedEvents = attended,
                        reflections = reflections,
                        me = member,
                    )
                )
            }
        }
    }

    fun onEvent(event: MemberReportEvent) {
        when (event) {
            is MemberReportEvent.IncludeAllChanged -> _uiState.update {
                it.copy(progressState = it.progressState.copy(includeAll = event.on))
            }
            is MemberReportEvent.IncludeAttendanceChanged -> _uiState.update {
                it.copy(
                    progressState = it.progressState.copy(includeAttendance = event.on)
                )
            }
            is MemberReportEvent.IncludeMeditationChanged -> _uiState.update {
                it.copy(
                    progressState = it.progressState.copy(includeMeditation = event.on)
                )
            }
            is MemberReportEvent.PeriodChanged -> _uiState.update {
                it.copy(progressState = it.progressState.copy(period = event.period))
            }
        }
    }
}
