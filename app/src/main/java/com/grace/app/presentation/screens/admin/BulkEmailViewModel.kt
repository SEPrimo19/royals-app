package com.grace.app.presentation.screens.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.Group
import com.grace.app.domain.model.UserRole
import com.grace.app.domain.repository.AdminRepository
import com.grace.app.domain.repository.EmailAudience
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AudienceKind { ALL, ROLES, GROUP }

data class BulkEmailUiState(
    val subject: String = "",
    val message: String = "",
    val audienceKind: AudienceKind = AudienceKind.ALL,
    val selectedRoles: Set<UserRole> = setOf(UserRole.MEMBER),
    val selectedGroupId: String? = null,
    val groups: List<Group> = emptyList(),
    val isSending: Boolean = false,
    val lastResultText: String? = null,
    val error: String? = null
)

sealed class BulkEmailEvent {
    data class SubjectChanged(val v: String) : BulkEmailEvent()
    data class MessageChanged(val v: String) : BulkEmailEvent()
    data class AudienceKindChanged(val v: AudienceKind) : BulkEmailEvent()
    data class ToggleRole(val role: UserRole) : BulkEmailEvent()
    data class GroupSelected(val groupId: String?) : BulkEmailEvent()
    data object Send : BulkEmailEvent()
    data object DismissError : BulkEmailEvent()
}

sealed class BulkEmailEffect {
    data class Sent(val recipients: Int, val sent: Int) : BulkEmailEffect()
    data class ShowError(val message: String) : BulkEmailEffect()
}

@HiltViewModel
class BulkEmailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        BulkEmailUiState(
            // EventsScreen can pre-fill the form when sending an event blast
            // by passing ?subject= and ?message= as nav args. Optional —
            // bulk-email entry from Admin screen sets neither.
            subject = savedStateHandle.get<String>("subject").orEmpty(),
            message = savedStateHandle.get<String>("message").orEmpty()
        )
    )
    val uiState: StateFlow<BulkEmailUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<BulkEmailEffect>()
    val effect = _effect.asSharedFlow()

    init {
        viewModelScope.launch {
            when (val r = adminRepository.listGroups()) {
                is Result.Success -> _uiState.value =
                    _uiState.value.copy(groups = r.data)
                is Result.Error -> _uiState.value =
                    _uiState.value.copy(error = r.message)
                Result.Loading -> Unit
            }
        }
    }

    fun onEvent(event: BulkEmailEvent) {
        when (event) {
            is BulkEmailEvent.SubjectChanged ->
                _uiState.value = _uiState.value.copy(subject = event.v)
            is BulkEmailEvent.MessageChanged ->
                _uiState.value = _uiState.value.copy(message = event.v)
            is BulkEmailEvent.AudienceKindChanged ->
                _uiState.value = _uiState.value.copy(audienceKind = event.v)
            is BulkEmailEvent.ToggleRole -> {
                val s = _uiState.value
                val next = s.selectedRoles.toMutableSet().apply {
                    if (!add(event.role)) remove(event.role)
                }
                _uiState.value = s.copy(selectedRoles = next)
            }
            is BulkEmailEvent.GroupSelected ->
                _uiState.value = _uiState.value.copy(selectedGroupId = event.groupId)
            BulkEmailEvent.Send -> send()
            BulkEmailEvent.DismissError ->
                _uiState.value = _uiState.value.copy(error = null)
        }
    }

    private fun send() {
        val s = _uiState.value
        val audience: EmailAudience = when (s.audienceKind) {
            AudienceKind.ALL -> EmailAudience.All
            AudienceKind.ROLES -> EmailAudience.Roles(s.selectedRoles)
            AudienceKind.GROUP -> {
                val gid = s.selectedGroupId
                if (gid.isNullOrBlank()) {
                    _uiState.value = s.copy(error = "Pick a cell group first.")
                    return
                }
                EmailAudience.Group(gid)
            }
        }
        viewModelScope.launch {
            _uiState.value = s.copy(isSending = true, error = null, lastResultText = null)
            when (val r = adminRepository.sendBulkEmail(s.subject, s.message, audience)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        lastResultText = "Sent to ${r.data.sent} of ${r.data.recipients} recipients."
                    )
                    _effect.emit(BulkEmailEffect.Sent(r.data.recipients, r.data.sent))
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false, error = r.message
                    )
                    _effect.emit(BulkEmailEffect.ShowError(r.message))
                }
                Result.Loading -> Unit
            }
        }
    }
}
