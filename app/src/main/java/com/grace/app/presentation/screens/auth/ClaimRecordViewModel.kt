package com.grace.app.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.User
import com.grace.app.domain.repository.AuthRepository
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

data class ClaimRecordUiState(
    val isLoading: Boolean = true,
    val proxy: User? = null,
    val isClaiming: Boolean = false,
    val claimSuccess: Boolean = false,
    val error: String? = null
)

sealed interface ClaimRecordEvent {
    data object ConfirmClaim : ClaimRecordEvent

    data object Dismiss : ClaimRecordEvent
}

sealed interface ClaimRecordEffect {
    data object Done : ClaimRecordEffect

    data object Claimed : ClaimRecordEffect
}

@HiltViewModel
class ClaimRecordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClaimRecordUiState())
    val uiState: StateFlow<ClaimRecordUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ClaimRecordEffect>()
    val effect: SharedFlow<ClaimRecordEffect> = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            when (val r = authRepository.findClaimableProxy()) {
                is Result.Success -> {
                    val proxy = r.data
                    _uiState.update { it.copy(isLoading = false, proxy = proxy) }
                    if (proxy == null) {
                        _effect.emit(ClaimRecordEffect.Done)
                    }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = r.message) }
                    _effect.emit(ClaimRecordEffect.Done)
                }
                else -> Unit
            }
        }
    }

    fun onEvent(event: ClaimRecordEvent) {
        when (event) {
            ClaimRecordEvent.ConfirmClaim -> claim()
            ClaimRecordEvent.Dismiss -> viewModelScope.launch {
                _effect.emit(ClaimRecordEffect.Done)
            }
        }
    }

    private fun claim() {
        val proxyId = _uiState.value.proxy?.id ?: return
        _uiState.update { it.copy(isClaiming = true, error = null) }
        viewModelScope.launch {
            when (val r = authRepository.claimProxyRecord(proxyId)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(isClaiming = false, claimSuccess = true)
                    }
                    _effect.emit(ClaimRecordEffect.Claimed)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isClaiming = false, error = r.message)
                }
                else -> Unit
            }
        }
    }
}
