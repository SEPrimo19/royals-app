package com.grace.app.presentation.screens.lifegroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.IncomingJoinRequest
import com.grace.app.domain.usecase.lifegroup.ApproveJoinRequestUseCase
import com.grace.app.domain.usecase.lifegroup.ListIncomingJoinRequestsUseCase
import com.grace.app.domain.usecase.lifegroup.RejectJoinRequestUseCase
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

data class JoinRequestsInboxUiState(
    val isLoading: Boolean = true,
    val requests: List<IncomingJoinRequest> = emptyList(),
    val workingId: String? = null,
    val rejecting: IncomingJoinRequest? = null,
    val error: String? = null
)

sealed interface JoinInboxEvent {
    data object Refresh : JoinInboxEvent
    data class Approve(val requestId: String) : JoinInboxEvent
    data class StartReject(val request: IncomingJoinRequest) : JoinInboxEvent
    data object CancelReject : JoinInboxEvent
    data class ConfirmReject(val note: String) : JoinInboxEvent
}

sealed interface JoinInboxEffect {
    data class Toast(val message: String, val isError: Boolean) : JoinInboxEffect
}

@HiltViewModel
class JoinRequestsInboxViewModel @Inject constructor(
    private val listIncoming: ListIncomingJoinRequestsUseCase,
    private val approve: ApproveJoinRequestUseCase,
    private val reject: RejectJoinRequestUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(JoinRequestsInboxUiState())
    val uiState: StateFlow<JoinRequestsInboxUiState> = _ui.asStateFlow()

    private val _effect = MutableSharedFlow<JoinInboxEffect>()
    val effect: SharedFlow<JoinInboxEffect> = _effect.asSharedFlow()

    init { load() }

    fun onEvent(e: JoinInboxEvent) {
        when (e) {
            JoinInboxEvent.Refresh -> load()
            is JoinInboxEvent.Approve -> doApprove(e.requestId)
            is JoinInboxEvent.StartReject ->
                _ui.update { it.copy(rejecting = e.request) }
            JoinInboxEvent.CancelReject ->
                _ui.update { it.copy(rejecting = null) }
            is JoinInboxEvent.ConfirmReject -> doReject(e.note)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            when (val r = listIncoming()) {
                is Result.Success ->
                    _ui.update { it.copy(isLoading = false, requests = r.data) }
                is Result.Error ->
                    _ui.update { it.copy(isLoading = false, error = r.message) }
                Result.Loading -> Unit
            }
        }
    }

    private fun doApprove(requestId: String) {
        viewModelScope.launch {
            _ui.update { it.copy(workingId = requestId) }
            when (val r = approve(requestId)) {
                is Result.Success -> {
                    _effect.emit(JoinInboxEffect.Toast("Approved — member added.", false))
                    load()
                }
                is Result.Error -> {
                    _ui.update { it.copy(workingId = null) }
                    _effect.emit(JoinInboxEffect.Toast(r.message, true))
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun doReject(note: String) {
        val target = _ui.value.rejecting ?: return
        _ui.update { it.copy(rejecting = null, workingId = target.id) }
        viewModelScope.launch {
            when (val r = reject(target.id, note.trim().takeIf { it.isNotEmpty() })) {
                is Result.Success -> {
                    _effect.emit(JoinInboxEffect.Toast("Request declined.", false))
                    load()
                }
                is Result.Error -> {
                    _ui.update { it.copy(workingId = null) }
                    _effect.emit(JoinInboxEffect.Toast(r.message, true))
                }
                Result.Loading -> Unit
            }
        }
    }
}
