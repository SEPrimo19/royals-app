package com.grace.app.presentation.screens.lifegroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.BrowsableGroup
import com.grace.app.domain.usecase.lifegroup.CancelMyJoinRequestUseCase
import com.grace.app.domain.usecase.lifegroup.ListBrowsableGroupsUseCase
import com.grace.app.domain.usecase.lifegroup.RequestToJoinGroupUseCase
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

data class FindCellUiState(
    val isLoading: Boolean = true,
    val groups: List<BrowsableGroup> = emptyList(),
    val query: String = "",
    val workingGroupId: String? = null,
    val error: String? = null
) {
    val visibleGroups: List<BrowsableGroup>
        get() = if (query.isBlank()) groups
        else groups.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.leaderName.contains(query, ignoreCase = true)
        }
}

sealed interface FindCellEvent {
    data class QueryChanged(val v: String) : FindCellEvent
    data class RequestToJoin(val groupId: String) : FindCellEvent
    data class CancelPending(val groupId: String, val requestId: String) : FindCellEvent
    data object Refresh : FindCellEvent
}

sealed interface FindCellEffect {
    data class Toast(val message: String, val isError: Boolean) : FindCellEffect
}

@HiltViewModel
class FindCellViewModel @Inject constructor(
    private val listBrowsable: ListBrowsableGroupsUseCase,
    private val requestToJoin: RequestToJoinGroupUseCase,
    private val cancelMine: CancelMyJoinRequestUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(FindCellUiState())
    val uiState: StateFlow<FindCellUiState> = _ui.asStateFlow()

    private val _effect = MutableSharedFlow<FindCellEffect>()
    val effect: SharedFlow<FindCellEffect> = _effect.asSharedFlow()

    init { load() }

    fun onEvent(e: FindCellEvent) {
        when (e) {
            is FindCellEvent.QueryChanged ->
                _ui.update { it.copy(query = e.v) }
            is FindCellEvent.RequestToJoin -> doRequest(e.groupId)
            is FindCellEvent.CancelPending -> doCancel(e.groupId, e.requestId)
            FindCellEvent.Refresh -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            when (val r = listBrowsable()) {
                is Result.Success ->
                    _ui.update { it.copy(isLoading = false, groups = r.data) }
                is Result.Error ->
                    _ui.update { it.copy(isLoading = false, error = r.message) }
                Result.Loading -> Unit
            }
        }
    }

    private fun doRequest(groupId: String) {
        viewModelScope.launch {
            _ui.update { it.copy(workingGroupId = groupId) }
            when (val r = requestToJoin(groupId)) {
                is Result.Success -> {
                    _effect.emit(
                        FindCellEffect.Toast("Request sent — your leader will be notified.", false)
                    )
                    load()
                }
                is Result.Error -> {
                    _ui.update { it.copy(workingGroupId = null) }
                    _effect.emit(FindCellEffect.Toast(r.message, true))
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun doCancel(groupId: String, requestId: String) {
        viewModelScope.launch {
            _ui.update { it.copy(workingGroupId = groupId) }
            when (val r = cancelMine(requestId)) {
                is Result.Success -> {
                    _effect.emit(FindCellEffect.Toast("Request cancelled.", false))
                    load()
                }
                is Result.Error -> {
                    _ui.update { it.copy(workingGroupId = null) }
                    _effect.emit(FindCellEffect.Toast(r.message, true))
                }
                Result.Loading -> Unit
            }
        }
    }
}
