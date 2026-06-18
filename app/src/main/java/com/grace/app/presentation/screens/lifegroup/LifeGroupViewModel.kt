package com.grace.app.presentation.screens.lifegroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.domain.model.LifeGroupDetail
import com.grace.app.domain.model.User
import com.grace.app.domain.model.UserRole
import com.grace.app.domain.usecase.lifegroup.AddMemberUseCase
import com.grace.app.domain.usecase.lifegroup.CreateLifeGroupUseCase
import com.grace.app.domain.usecase.lifegroup.DeleteLifeGroupUseCase
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
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val isSearching: Boolean = false,
    val browseOpen: Boolean = false,
    val browseAll: List<User> = emptyList(),
    val browseFilter: String = "",
    val isBrowseLoading: Boolean = false,
    val showCreateForm: Boolean = false
) {
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

    val canManage: Boolean
        get() = isLeaderOfThisGroup ||
            myRole == UserRole.YOUTH_PRESIDENT ||
            myRole == UserRole.PASTOR ||
            myRole == UserRole.ADMIN

    private val canLead: Boolean
        get() = myRole == UserRole.CELL_LEADER ||
            myRole == UserRole.COUNCIL ||
            myRole == UserRole.YOUTH_PRESIDENT ||
            myRole == UserRole.PASTOR ||
            myRole == UserRole.ADMIN

    val canCreate: Boolean
        get() = detail == null && canLead

    val canCreateSecondCell: Boolean
        get() = detail != null && myRole == UserRole.COUNCIL
}

sealed interface LifeGroupEvent {
    data class CreateGroup(val name: String, val description: String) : LifeGroupEvent
    data class AddMember(val userId: String) : LifeGroupEvent
    data class RemoveMember(val userId: String) : LifeGroupEvent
    data class SearchQueryChanged(val query: String) : LifeGroupEvent
    data object ClearSearch : LifeGroupEvent
    data object LeaveGroup : LifeGroupEvent
    data object ToggleCreateForm : LifeGroupEvent
    data object DeleteGroup : LifeGroupEvent
    data object Refresh : LifeGroupEvent
    data object DismissError : LifeGroupEvent
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
    private val deleteLifeGroupUseCase: DeleteLifeGroupUseCase,
    private val listInvitableUsersUseCase: ListInvitableUsersUseCase,
    private val prefs: UserPreferencesRepo
) : ViewModel() {

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
            LifeGroupEvent.ToggleCreateForm ->
                _uiState.update { it.copy(showCreateForm = !it.showCreateForm) }
            LifeGroupEvent.DeleteGroup -> deleteGroup()
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
                    _uiState.update { it.copy(showCreateForm = false) }
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

    private fun deleteGroup() {
        val groupId = _uiState.value.detail?.group?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            val r = deleteLifeGroupUseCase(groupId)
            _uiState.update { it.copy(isWorking = false) }
            when (r) {
                is Result.Success -> {
                    _effect.emit(LifeGroupEffect.ShowSuccess("Cell group deleted."))
                    load()
                }
                is Result.Error -> _effect.emit(LifeGroupEffect.ShowError(r.message))
                Result.Loading -> Unit
            }
        }
    }

    private fun parseRole(raw: String): UserRole = when (raw.trim().lowercase()) {
        "cell_leader" -> UserRole.CELL_LEADER
        "youth_president" -> UserRole.YOUTH_PRESIDENT
        "pastor" -> UserRole.PASTOR
        "admin" -> UserRole.ADMIN
        else -> UserRole.MEMBER
    }
}
