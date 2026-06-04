package com.grace.app.presentation.screens.lifegroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.domain.model.LifeGroupDetail
import com.grace.app.domain.model.User
import com.grace.app.domain.model.UserRole
import com.grace.app.domain.usecase.lifegroup.AddMemberUseCase
import com.grace.app.domain.usecase.lifegroup.CreateLifeGroupUseCase
import com.grace.app.domain.usecase.lifegroup.GetMyLifeGroupUseCase
import com.grace.app.domain.usecase.lifegroup.ListInvitableUsersUseCase
import com.grace.app.domain.usecase.lifegroup.RemoveMemberUseCase
import com.grace.app.domain.usecase.lifegroup.SearchInvitableUsersUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LifeGroupUiState(
    val detail: LifeGroupDetail? = null,
    val myUserId: String? = null,
    val myRole: UserRole = UserRole.MEMBER,
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val error: String? = null,
    // Search state for the Add-Member dialog (debounced server query).
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val isSearching: Boolean = false,
    // Browse-all-invitable state for the secondary "see everyone without
    // a group" modal. The full list is loaded once when the modal opens,
    // then filtered client-side via [browseFilter] so typing is instant.
    val browseOpen: Boolean = false,
    val browseAll: List<User> = emptyList(),
    val browseFilter: String = "",
    val isBrowseLoading: Boolean = false
) {
    /** Client-side filter on top of the browse pool. */
    val browseFiltered: List<User>
        get() {
            val f = browseFilter.trim().lowercase()
            return if (f.isBlank()) browseAll
            else browseAll.filter {
                it.name.lowercase().contains(f) ||
                    it.email.lowercase().contains(f)
            }
        }

    val isLeaderOfThisGroup: Boolean
        get() = detail?.group?.leaderId != null && detail.group.leaderId == myUserId

    /** Can manage: leader of this group OR senior leader. */
    val canManage: Boolean
        get() = isLeaderOfThisGroup ||
            myRole == UserRole.YOUTH_PRESIDENT ||
            myRole == UserRole.PASTOR ||
            myRole == UserRole.ADMIN

    /** Can create: any role at/above cell_leader who isn't already in a group. */
    val canCreate: Boolean
        get() = detail == null && (
            myRole == UserRole.CELL_LEADER ||
                myRole == UserRole.YOUTH_PRESIDENT ||
                myRole == UserRole.PASTOR ||
                myRole == UserRole.ADMIN
            )
}

sealed interface LifeGroupEvent {
    data class CreateGroup(val name: String, val description: String) : LifeGroupEvent
    data class AddMember(val userId: String) : LifeGroupEvent
    data class RemoveMember(val userId: String) : LifeGroupEvent
    data class SearchQueryChanged(val query: String) : LifeGroupEvent
    data object ClearSearch : LifeGroupEvent
    data object LeaveGroup : LifeGroupEvent
    data object Refresh : LifeGroupEvent
    data object DismissError : LifeGroupEvent
    // Browse-all modal events.
    data object OpenBrowse : LifeGroupEvent
    data object CloseBrowse : LifeGroupEvent
    data class BrowseFilterChanged(val query: String) : LifeGroupEvent
}

sealed interface LifeGroupEffect {
    data class ShowError(val message: String) : LifeGroupEffect
    data class ShowSuccess(val message: String) : LifeGroupEffect
}

@HiltViewModel
class LifeGroupViewModel @Inject constructor(
    private val getMyLifeGroupUseCase: GetMyLifeGroupUseCase,
    private val createLifeGroupUseCase: CreateLifeGroupUseCase,
    private val addMemberUseCase: AddMemberUseCase,
    private val removeMemberUseCase: RemoveMemberUseCase,
    private val searchInvitableUsersUseCase: SearchInvitableUsersUseCase,
    private val listInvitableUsersUseCase: ListInvitableUsersUseCase,
    private val prefs: UserPreferencesRepo
) : ViewModel() {

    // Holds the latest in-flight search so a fast typer doesn't render a
    // result that was already obsolete by the time the network came back.
    private var searchJob: Job? = null

    private val _uiState = MutableStateFlow(LifeGroupUiState())
    val uiState: StateFlow<LifeGroupUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<LifeGroupEffect>()
    val effect: SharedFlow<LifeGroupEffect> = _effect.asSharedFlow()

    init { load() }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val myId = prefs.userId.first()
            val roleStr = prefs.userRole.first() ?: "member"
            val role = parseRole(roleStr)
            when (val r = getMyLifeGroupUseCase()) {
                is Result.Success -> _uiState.update {
                    it.copy(
                        detail = r.data,
                        myUserId = myId,
                        myRole = role,
                        isLoading = false
                    )
                }
                is Result.Error -> _uiState.update {
                    it.copy(
                        myUserId = myId,
                        myRole = role,
                        isLoading = false,
                        error = r.message
                    )
                }
                Result.Loading -> Unit
            }
        }
    }

    fun onEvent(event: LifeGroupEvent) {
        when (event) {
            is LifeGroupEvent.CreateGroup -> createGroup(event.name, event.description)
            is LifeGroupEvent.AddMember -> addMember(event.userId)
            is LifeGroupEvent.RemoveMember -> removeMember(event.userId)
            is LifeGroupEvent.SearchQueryChanged -> onSearchQueryChanged(event.query)
            LifeGroupEvent.ClearSearch -> {
                searchJob?.cancel()
                _uiState.update {
                    it.copy(
                        searchQuery = "",
                        searchResults = emptyList(),
                        isSearching = false
                    )
                }
            }
            LifeGroupEvent.LeaveGroup -> leaveGroup()
            LifeGroupEvent.Refresh -> load()
            LifeGroupEvent.DismissError -> _uiState.update { it.copy(error = null) }
            LifeGroupEvent.OpenBrowse -> openBrowse()
            LifeGroupEvent.CloseBrowse -> _uiState.update {
                it.copy(browseOpen = false, browseFilter = "")
            }
            is LifeGroupEvent.BrowseFilterChanged -> _uiState.update {
                it.copy(browseFilter = event.query)
            }
        }
    }

    private fun openBrowse() {
        _uiState.update {
            it.copy(browseOpen = true, isBrowseLoading = true, browseFilter = "")
        }
        viewModelScope.launch {
            when (val r = listInvitableUsersUseCase(limit = 100)) {
                is Result.Success -> _uiState.update {
                    it.copy(browseAll = r.data, isBrowseLoading = false)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isBrowseLoading = false, error = r.message)
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = query) }
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        _uiState.update { it.copy(isSearching = true) }
        searchJob = viewModelScope.launch {
            // 300ms debounce — cancellation above means a fast typer never
            // gets stale results overwriting fresh ones.
            delay(300)
            when (val r = searchInvitableUsersUseCase(query)) {
                is Result.Success -> _uiState.update {
                    it.copy(searchResults = r.data, isSearching = false)
                }
                is Result.Error -> _uiState.update {
                    it.copy(isSearching = false, error = r.message)
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun createGroup(name: String, description: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            val r = createLifeGroupUseCase(name, description.takeIf { it.isNotBlank() })
            _uiState.update { it.copy(isWorking = false) }
            when (r) {
                is Result.Success -> {
                    _effect.emit(LifeGroupEffect.ShowSuccess("Life Group created."))
                    load()
                }
                is Result.Error -> _effect.emit(LifeGroupEffect.ShowError(r.message))
                Result.Loading -> Unit
            }
        }
    }

    private fun addMember(userId: String) {
        val groupId = _uiState.value.detail?.group?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            val r = addMemberUseCase(groupId, userId)
            _uiState.update {
                // Clear search after a successful add so the dialog state
                // resets cleanly for the next pick. Also strip the user
                // out of the browse-all pool so the leader can keep
                // adding from the list without seeing stale rows.
                it.copy(
                    isWorking = false,
                    searchQuery = "",
                    searchResults = emptyList(),
                    browseAll = it.browseAll.filterNot { u -> u.id == userId }
                )
            }
            when (r) {
                is Result.Success -> {
                    _effect.emit(LifeGroupEffect.ShowSuccess("Member added."))
                    load()
                }
                is Result.Error -> _effect.emit(LifeGroupEffect.ShowError(r.message))
                Result.Loading -> Unit
            }
        }
    }

    private fun removeMember(userId: String) {
        val groupId = _uiState.value.detail?.group?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            val r = removeMemberUseCase(groupId, userId)
            _uiState.update { it.copy(isWorking = false) }
            when (r) {
                is Result.Success -> {
                    _effect.emit(LifeGroupEffect.ShowSuccess("Removed."))
                    load()
                }
                is Result.Error -> _effect.emit(LifeGroupEffect.ShowError(r.message))
                Result.Loading -> Unit
            }
        }
    }

    private fun leaveGroup() {
        val myId = _uiState.value.myUserId ?: return
        removeMember(myId)
    }

    private fun parseRole(raw: String): UserRole = when (raw.trim().lowercase()) {
        "cell_leader" -> UserRole.CELL_LEADER
        "youth_president" -> UserRole.YOUTH_PRESIDENT
        "pastor" -> UserRole.PASTOR
        "admin" -> UserRole.ADMIN
        else -> UserRole.MEMBER
    }
}
