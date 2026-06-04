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

/**
 * State for the ClaimRecordScreen. We load the proxy match once on init.
 * Three terminal outcomes:
 *   - `proxy == null` → nothing to claim, screen should immediately
 *     emit DoneNoClaim and let MainActivity proceed to Home
 *   - `claimSuccess` → the SQL function returned OK; show a "Welcome
 *     back!" message briefly then route to Home
 *   - `error` → show the message, let the user dismiss without claiming
 */
data class ClaimRecordUiState(
    val isLoading: Boolean = true,
    val proxy: User? = null,
    val isClaiming: Boolean = false,
    val claimSuccess: Boolean = false,
    val error: String? = null
)

sealed interface ClaimRecordEvent {
    /** User confirmed the prompt — call the SQL function. */
    data object ConfirmClaim : ClaimRecordEvent

    /** User said "Not me" — skip without claiming. */
    data object Dismiss : ClaimRecordEvent
}

sealed interface ClaimRecordEffect {
    /** Either dismissed or no proxy match found — proceed to Home. */
    data object Done : ClaimRecordEffect

    /** Claim succeeded — same proceed-to-Home but caller may show a
     *  one-shot toast first. */
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
                        // Nothing to do — emit Done immediately so the
                        // host can route past this screen without flashing.
                        _effect.emit(ClaimRecordEffect.Done)
                    }
                }
                is Result.Error -> {
                    // Treat error as "no claim available" — the user
                    // shouldn't be blocked from entering the app over a
                    // background check failure. We still surface the error
                    // briefly via state.
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
