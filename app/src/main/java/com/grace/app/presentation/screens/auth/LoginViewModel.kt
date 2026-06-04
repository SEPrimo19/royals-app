package com.grace.app.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.repository.AuthRepository
import com.grace.app.domain.usecase.auth.SignInUseCase
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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    // ---- Forgot password modal --------------------------------------
    val showForgotPasswordDialog: Boolean = false,
    val forgotPasswordEmail: String = "",
    val forgotPasswordError: String? = null,
    val isSendingPasswordReset: Boolean = false,
    /** True after the reset email has been dispatched — UI swaps to a
     *  "Check your email" confirmation until the user closes the dialog. */
    val passwordResetSent: Boolean = false
)

sealed interface LoginEvent {
    data class EmailChanged(val value: String) : LoginEvent
    data class PasswordChanged(val value: String) : LoginEvent
    data object PasswordVisibilityToggled : LoginEvent
    data object LoginClicked : LoginEvent
    // Native One Tap surfaces success automatically via Supabase session;
    // this event only fires on failure so the form can show the error.
    data class GoogleSignInFailed(val message: String) : LoginEvent
    data object NavigateToSignUp : LoginEvent
    // Forgot password
    data object OpenForgotPasswordDialog : LoginEvent
    data object CloseForgotPasswordDialog : LoginEvent
    data class ForgotPasswordEmailChanged(val value: String) : LoginEvent
    data object SendPasswordReset : LoginEvent
}

sealed interface LoginEffect {
    data object NavigateToHome : LoginEffect
    data object NavigateToSignUp : LoginEffect
    data class ShowError(val message: String) : LoginEffect
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<LoginEffect>()
    val effect: SharedFlow<LoginEffect> = _effect.asSharedFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged ->
                _uiState.update { it.copy(email = event.value, emailError = null, generalError = null) }

            is LoginEvent.PasswordChanged ->
                _uiState.update { it.copy(password = event.value, passwordError = null, generalError = null) }

            LoginEvent.PasswordVisibilityToggled ->
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

            LoginEvent.NavigateToSignUp ->
                viewModelScope.launch { _effect.emit(LoginEffect.NavigateToSignUp) }

            LoginEvent.LoginClicked -> submit()

            is LoginEvent.GoogleSignInFailed -> viewModelScope.launch {
                _uiState.update { it.copy(generalError = event.message) }
                _effect.emit(LoginEffect.ShowError(event.message))
            }

            LoginEvent.OpenForgotPasswordDialog -> _uiState.update {
                // Pre-fill the email field from the sign-in form if the
                // user already typed it — saves a step.
                it.copy(
                    showForgotPasswordDialog = true,
                    forgotPasswordEmail = it.email,
                    forgotPasswordError = null,
                    passwordResetSent = false
                )
            }
            LoginEvent.CloseForgotPasswordDialog -> _uiState.update {
                it.copy(
                    showForgotPasswordDialog = false,
                    forgotPasswordError = null,
                    passwordResetSent = false,
                    isSendingPasswordReset = false
                )
            }
            is LoginEvent.ForgotPasswordEmailChanged -> _uiState.update {
                it.copy(forgotPasswordEmail = event.value, forgotPasswordError = null)
            }
            LoginEvent.SendPasswordReset -> sendReset()
        }
    }

    private fun sendReset() {
        val s = _uiState.value
        if (s.isSendingPasswordReset) return
        val email = s.forgotPasswordEmail.trim()
        // Cheap client-side check — Supabase will return an error too,
        // but a fast "looks-like-an-email" guard avoids one round trip.
        if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
            _uiState.update {
                it.copy(forgotPasswordError = "Enter a valid email address.")
            }
            return
        }
        _uiState.update {
            it.copy(isSendingPasswordReset = true, forgotPasswordError = null)
        }
        viewModelScope.launch {
            when (val r = authRepository.sendPasswordResetEmail(email)) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        isSendingPasswordReset = false,
                        passwordResetSent = true
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        isSendingPasswordReset = false,
                        forgotPasswordError = r.message
                    )
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.update { it.copy(isLoading = true, generalError = null) }
        viewModelScope.launch {
            when (val result = signInUseCase(state.email, state.password)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(LoginEffect.NavigateToHome)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, generalError = result.message)
                    }
                    _effect.emit(LoginEffect.ShowError(result.message))
                }
                Result.Loading -> Unit
            }
        }
    }
}
