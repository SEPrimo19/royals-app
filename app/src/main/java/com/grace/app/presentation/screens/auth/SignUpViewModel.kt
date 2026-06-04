package com.grace.app.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.usecase.auth.SignUpUseCase
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

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmVisible: Boolean = false,
    val isLoading: Boolean = false,
    val generalError: String? = null
)

sealed interface SignUpEvent {
    data class NameChanged(val value: String) : SignUpEvent
    data class EmailChanged(val value: String) : SignUpEvent
    data class PasswordChanged(val value: String) : SignUpEvent
    data class ConfirmPasswordChanged(val value: String) : SignUpEvent
    data object PasswordVisibilityToggled : SignUpEvent
    data object ConfirmVisibilityToggled : SignUpEvent
    data object SignUpClicked : SignUpEvent
    data class GoogleSignInFailed(val message: String) : SignUpEvent
    data object NavigateToLogin : SignUpEvent
}

sealed interface SignUpEffect {
    data object NavigateToProfileSetup : SignUpEffect
    data object NavigateToLogin : SignUpEffect
    data class ShowError(val message: String) : SignUpEffect
}

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<SignUpEffect>()
    val effect: SharedFlow<SignUpEffect> = _effect.asSharedFlow()

    fun onEvent(event: SignUpEvent) {
        when (event) {
            is SignUpEvent.NameChanged ->
                _uiState.update { it.copy(name = event.value, generalError = null) }
            is SignUpEvent.EmailChanged ->
                _uiState.update { it.copy(email = event.value, generalError = null) }
            is SignUpEvent.PasswordChanged ->
                _uiState.update { it.copy(password = event.value, generalError = null) }
            is SignUpEvent.ConfirmPasswordChanged ->
                _uiState.update { it.copy(confirmPassword = event.value, generalError = null) }
            SignUpEvent.PasswordVisibilityToggled ->
                _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
            SignUpEvent.ConfirmVisibilityToggled ->
                _uiState.update { it.copy(isConfirmVisible = !it.isConfirmVisible) }
            SignUpEvent.NavigateToLogin ->
                viewModelScope.launch { _effect.emit(SignUpEffect.NavigateToLogin) }
            SignUpEvent.SignUpClicked -> submit()
            is SignUpEvent.GoogleSignInFailed -> viewModelScope.launch {
                _uiState.update { it.copy(generalError = event.message) }
                _effect.emit(SignUpEffect.ShowError(event.message))
            }
        }
    }

    private fun submit() {
        val s = _uiState.value
        if (s.isLoading) return
        _uiState.update { it.copy(isLoading = true, generalError = null) }
        viewModelScope.launch {
            when (val result = signUpUseCase(s.name, s.email, s.password, s.confirmPassword)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(SignUpEffect.NavigateToProfileSetup)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isLoading = false, generalError = result.message)
                    }
                    _effect.emit(SignUpEffect.ShowError(result.message))
                }
                Result.Loading -> Unit
            }
        }
    }
}
