package com.grace.app.presentation.screens.admin

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

enum class AdminFilter(val label: String) {
    ALL("All"),
    MEMBER("Members"),
    CELL_LEADER("Cell Leaders"),
    COUNCIL("Council"),
    YOUTH_PRESIDENT("Youth President"),
    PASTOR("Pastors"),
    ADMIN("Admins"),
    COMPASSION("Compassion")
}

data class AdminUiState(
    val users: List<User> = emptyList(),
    val query: String = "",
    val filter: AdminFilter = AdminFilter.ALL,
    val sortAsc: Boolean = true,
    val isLoading: Boolean = true,
    val pendingUpdate: PendingUpdate? = null,
    val error: String? = null
) {
    val visibleUsers: List<User>
        get() {
            val filtered = when (filter) {
                AdminFilter.ALL -> users
                AdminFilter.COMPASSION -> users.filter { it.isCompassion }
                AdminFilter.MEMBER -> users.filter { it.role == UserRole.MEMBER }
                AdminFilter.CELL_LEADER -> users.filter { it.role == UserRole.CELL_LEADER }
                AdminFilter.COUNCIL -> users.filter { it.role == UserRole.COUNCIL }
                AdminFilter.YOUTH_PRESIDENT -> users.filter { it.role == UserRole.YOUTH_PRESIDENT }
                AdminFilter.PASTOR -> users.filter { it.role == UserRole.PASTOR }
                AdminFilter.ADMIN -> users.filter { it.role == UserRole.ADMIN }
            }
            val searched = if (query.isBlank()) filtered
            else filtered.filter {
                it.name.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
            }
            return if (sortAsc) searched.sortedBy { it.name.lowercase() }
            else searched.sortedByDescending { it.name.lowercase() }
        }
}

data class PendingUpdate(
    val user: User,
    val newRole: UserRole,
    val isSensitive: Boolean
)

sealed interface AdminEvent {
    data class QueryChanged(val query: String) : AdminEvent
    data class FilterChanged(val filter: AdminFilter) : AdminEvent
    data object ToggleSortDirection : AdminEvent
    data class StartRoleChange(val user: User, val newRole: UserRole) : AdminEvent
    data object ConfirmRoleChange : AdminEvent
    data object CancelRoleChange : AdminEvent
    data object Refresh : AdminEvent
    data object DismissError : AdminEvent
}

sealed interface AdminEffect {
    data class ShowError(val message: String) : AdminEffect
    data class ShowSuccess(val message: String) : AdminEffect
}

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val getAllUsersUseCase: GetAllUsersUseCase,
    private val updateUserRoleUseCase: UpdateUserRoleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<AdminEffect>()
    val effect: SharedFlow<AdminEffect> = _effect.asSharedFlow()

    init { loadUsers() }

    fun refresh() = loadUsers()

    private fun loadUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = getAllUsersUseCase()) {
                is Result.Success -> _uiState.update {
                    it.copy(users = r.data, isLoading = false)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = r.message) }
                }
                Result.Loading -> Unit
            }
        }
    }

    fun onEvent(event: AdminEvent) {
        when (event) {
            is AdminEvent.QueryChanged ->
                _uiState.update { it.copy(query = event.query) }

            is AdminEvent.FilterChanged ->
                _uiState.update { it.copy(filter = event.filter) }

            AdminEvent.ToggleSortDirection ->
                _uiState.update { it.copy(sortAsc = !it.sortAsc) }

            is AdminEvent.StartRoleChange -> {
                val sensitive = event.newRole == UserRole.PASTOR ||
                    event.newRole == UserRole.ADMIN ||
                    event.user.role == UserRole.PASTOR ||
                    event.user.role == UserRole.ADMIN
                _uiState.update {
                    it.copy(
                        pendingUpdate = PendingUpdate(event.user, event.newRole, sensitive)
                    )
                }
            }

            AdminEvent.CancelRoleChange ->
                _uiState.update { it.copy(pendingUpdate = null) }

            AdminEvent.ConfirmRoleChange -> commitRoleChange()
            AdminEvent.Refresh -> loadUsers()
            AdminEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun commitRoleChange() {
        val pending = _uiState.value.pendingUpdate ?: return
        _uiState.update { it.copy(pendingUpdate = null) }
        viewModelScope.launch {
            when (val r = updateUserRoleUseCase(pending.user.id, pending.newRole)) {
                is Result.Success -> {
                    _effect.emit(
                        AdminEffect.ShowSuccess(
                            "${pending.user.name} is now ${pending.newRole.label}."
                        )
                    )
                    loadUsers()
                }
                is Result.Error -> _effect.emit(AdminEffect.ShowError(r.message))
                Result.Loading -> Unit
            }
        }
    }
}

val UserRole.label: String
    get() = when (this) {
        UserRole.MEMBER -> "Member"
        UserRole.CELL_LEADER -> "Cell Leader"
        UserRole.COUNCIL -> "Council"
        UserRole.YOUTH_PRESIDENT -> "Youth President"
        UserRole.PASTOR -> "Pastor"
        UserRole.ADMIN -> "Admin"
    }
