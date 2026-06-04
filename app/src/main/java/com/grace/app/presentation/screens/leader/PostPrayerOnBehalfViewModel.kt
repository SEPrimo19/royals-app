package com.grace.app.presentation.screens.leader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.PrayerCategory
import com.grace.app.domain.repository.LeaderRepository
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

data class PostPrayerOnBehalfUiState(
    val content: String = "",
    val category: PrayerCategory = PrayerCategory.PERSONAL,
    val isSubmitting: Boolean = false,
    val error: String? = null
) {
    val canSubmit: Boolean
        get() = content.trim().length >= 3 && !isSubmitting
}

sealed interface PostPrayerOnBehalfEvent {
    data class ContentChanged(val v: String) : PostPrayerOnBehalfEvent
    data class CategoryChanged(val v: PrayerCategory) : PostPrayerOnBehalfEvent
    data object Submit : PostPrayerOnBehalfEvent
    data object DismissError : PostPrayerOnBehalfEvent
}

sealed interface PostPrayerOnBehalfEffect {
    data object Posted : PostPrayerOnBehalfEffect
}

@HiltViewModel
class PostPrayerOnBehalfViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val leaderRepository: LeaderRepository
) : ViewModel() {

    private val memberId: String = savedStateHandle.get<String>("memberId").orEmpty()

    private val _uiState = MutableStateFlow(PostPrayerOnBehalfUiState())
    val uiState: StateFlow<PostPrayerOnBehalfUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<PostPrayerOnBehalfEffect>()
    val effect: SharedFlow<PostPrayerOnBehalfEffect> = _effect.asSharedFlow()

    fun onEvent(event: PostPrayerOnBehalfEvent) {
        when (event) {
            is PostPrayerOnBehalfEvent.ContentChanged ->
                _uiState.update { it.copy(content = event.v) }
            is PostPrayerOnBehalfEvent.CategoryChanged ->
                _uiState.update { it.copy(category = event.v) }
            PostPrayerOnBehalfEvent.DismissError ->
                _uiState.update { it.copy(error = null) }
            PostPrayerOnBehalfEvent.Submit -> submit()
        }
    }

    private fun submit() {
        val s = _uiState.value
        if (!s.canSubmit) return
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            val r = leaderRepository.postPrayerOnBehalf(
                memberId = memberId,
                content = s.content,
                category = s.category
            )
            when (r) {
                is Result.Success -> {
                    _effect.emit(PostPrayerOnBehalfEffect.Posted)
                    _uiState.update { it.copy(isSubmitting = false) }
                }
                is Result.Error -> _uiState.update {
                    it.copy(isSubmitting = false, error = r.message)
                }
                else -> _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
