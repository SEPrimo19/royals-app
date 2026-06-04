package com.grace.app.presentation.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.JournalEntry
import com.grace.app.domain.usecase.devotional.GetMyJournalUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyJournalUiState(
    val isLoading: Boolean = true,
    val entries: List<JournalEntry> = emptyList(),
    val expandedDevoIds: Set<String> = emptySet(),
    val error: String? = null
)

sealed class MyJournalEvent {
    data class ToggleExpanded(val devoId: String) : MyJournalEvent()
    data object DismissError : MyJournalEvent()
}

@HiltViewModel
class MyJournalViewModel @Inject constructor(
    private val getMyJournal: GetMyJournalUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyJournalUiState())
    val uiState: StateFlow<MyJournalUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getMyJournal().collect { res ->
                _uiState.value = when (res) {
                    is Result.Success -> _uiState.value.copy(
                        isLoading = false,
                        entries = res.data,
                        error = null
                    )
                    is Result.Error -> _uiState.value.copy(
                        isLoading = false,
                        error = res.message
                    )
                    Result.Loading -> _uiState.value.copy(isLoading = true)
                }
            }
        }
    }

    fun onEvent(event: MyJournalEvent) {
        when (event) {
            is MyJournalEvent.ToggleExpanded -> {
                val s = _uiState.value
                val next = s.expandedDevoIds.toMutableSet().apply {
                    if (!add(event.devoId)) remove(event.devoId)
                }
                _uiState.value = s.copy(expandedDevoIds = next)
            }
            MyJournalEvent.DismissError -> {
                _uiState.value = _uiState.value.copy(error = null)
            }
        }
    }
}
