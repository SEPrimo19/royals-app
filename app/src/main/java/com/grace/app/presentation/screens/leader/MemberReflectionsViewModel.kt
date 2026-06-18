package com.grace.app.presentation.screens.leader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.Mentee
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

data class MemberReflectionsUiState(
    val isLoading: Boolean = true,
    val mentee: Mentee? = null,
    val items: List<MemberReflectionItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class MemberReflectionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMyMenteesUseCase: GetMyMenteesUseCase,
    private val getMeditationSubmissionsForUserUseCase: GetMeditationSubmissionsForUserUseCase,
    private val getAllMeditationsUseCase: GetAllMeditationsUseCase
) : ViewModel() {

    private val memberId: String = savedStateHandle.get<String>("memberId").orEmpty()

    private val _uiState = MutableStateFlow(MemberReflectionsUiState())
    val uiState: StateFlow<MemberReflectionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val mentee = (getMyMenteesUseCase() as? Result.Success)?.data
                ?.firstOrNull { it.user.id == memberId }

            val subs = (getMeditationSubmissionsForUserUseCase(memberId)
                as? Result.Success)?.data.orEmpty()
            val meds = runCatching {
                getAllMeditationsUseCase().first()
            }.getOrDefault(emptyList())
            val medsById = meds.associateBy { it.id }
            val items = subs.map { MemberReflectionItem(it, medsById[it.meditationId]) }

            _uiState.update {
                it.copy(isLoading = false, mentee = mentee, items = items)
            }
        }
    }
}
