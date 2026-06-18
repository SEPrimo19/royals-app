package com.grace.app.presentation.screens.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.User
import com.grace.app.domain.model.UserRole
import com.grace.app.domain.usecase.admin.GetAllUsersUseCase
import com.grace.app.domain.usecase.admin.UpdateUserRoleUseCase
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

data class AdminUserDetailUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val pendingRole: UserRole? = null,
    val pendingIsSensitive: Boolean = false,
    val isCommitting: Boolean = false,
    val error: String? = null
)

sealed interface AdminUserDetailEvent {
    data class StartRoleChange(val newRole: UserRole) : AdminUserDetailEvent
    data object ConfirmRoleChange : AdminUserDetailEvent
    data object CancelRoleChange : AdminUserDetailEvent
}

sealed interface AdminUserDetailEffect {
    data class ShowError(val message: String) : AdminUserDetailEffect
    data class ShowSuccess(val message: String) : AdminUserDetailEffect
}

@HiltViewModel
class AdminUserDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAllUsersUseCase: GetAllUsersUseCase,
    private val updateUserRoleUseCase: UpdateUserRoleUseCase
) : ViewModel() {

    private val userId: String = savedStateHandle.get<String>("userId").orEmpty()

    private val _uiState = MutableStateFlow(AdminUserDetailUiState())
    val uiState: StateFlow<AdminUserDetailUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<AdminUserDetailEffect>()
    val effect: SharedFlow<AdminUserDetailEffect> = _effect.asSharedFlow()

    init { load() }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = getAllUsersUseCase()) {
                is Result.Success -> {
                    val u = r.data.firstOrNull { it.id == userId }
                    _uiState.update {
                        it.copy(
                            user = u,
                            isLoading = false,
                            error = if (u == null) "User not found." else null
                        )
                    }
                }
                is Result.Error ->
                    _uiState.update { it.copy(isLoading = false, error = r.message) }
                Result.Loading -> Unit
            }
        }
    }

    fun onEvent(event: AdminUserDetailEvent) {
        when (event) {
            is AdminUserDetailEvent.StartRoleChange -> {
                val current = _uiState.value.user?.role
                val sensitive = event.newRole == UserRole.PASTOR ||
                    event.newRole == UserRole.ADMIN ||
                    current == UserRole.PASTOR ||
                    current == UserRole.ADMIN
                _uiState.update {
                    it.copy(
                        pendingRole = event.newRole,
                        pendingIsSensitive = sensitive
                    )
                }
            }

            AdminUserDetailEvent.CancelRoleChange ->
                _uiState.update {
                    it.copy(pendingRole = null, pendingIsSensitive = false)
                }

            AdminUserDetailEvent.ConfirmRoleChange -> commit()
        }
    }

    private fun commit() {
        val newRole = _uiState.value.pendingRole ?: return
        val user = _uiState.value.user ?: return
        _uiState.update {
            it.copy(
                isCommitting = true, pendingRole = null,
                pendingIsSensitive = false
            )
        }
        viewModelScope.launch {
            when (val r = updateUserRoleUseCase(user.id, newRole)) {
                is Result.Success -> {
                    _effect.emit(
                        AdminUserDetailEffect.ShowSuccess(
                            "${user.name} is now ${newRole.label}."
                        )
                    )
                    load()
                }
                is Result.Error -> {
                    _effect.emit(AdminUserDetailEffect.ShowError(r.message))
                }
                Result.Loading -> Unit
            }
            _uiState.update { it.copy(isCommitting = false) }
        }
    }
}
