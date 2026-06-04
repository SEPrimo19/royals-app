package com.grace.app.presentation.screens.leader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.Mentee
import com.grace.app.domain.usecase.leader.GetMyMenteesUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyMembersUiState(
    val isLoading: Boolean = true,
    val mentees: List<Mentee> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class MyMembersViewModel @Inject constructor(
    private val getMyMenteesUseCase: GetMyMenteesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyMembersUiState())
    val uiState: StateFlow<MyMembersUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = getMyMenteesUseCase()) {
                is Result.Success ->
                    _uiState.update { it.copy(isLoading = false, mentees = r.data) }
                is Result.Error ->
                    _uiState.update { it.copy(isLoading = false, error = r.message) }
                Result.Loading -> Unit
            }
        }
    }
}
