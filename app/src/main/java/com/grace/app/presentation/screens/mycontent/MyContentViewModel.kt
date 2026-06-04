package com.grace.app.presentation.screens.mycontent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.MeditationSubmission
import com.grace.app.domain.model.Post
import com.grace.app.domain.model.Prayer
import com.grace.app.domain.model.WeeklyMeditation
import com.grace.app.domain.usecase.meditation.GetAllMeditationsUseCase
import com.grace.app.domain.usecase.meditation.GetMyMeditationSubmissionsUseCase
import com.grace.app.domain.usecase.prayer.MarkPrayerAnsweredUseCase
import com.grace.app.domain.usecase.profile.DeletePostUseCase
import com.grace.app.domain.usecase.profile.DeletePrayerUseCase
import com.grace.app.domain.usecase.profile.GetMyPostsUseCase
import com.grace.app.domain.usecase.profile.GetMyPrayersUseCase
import com.grace.app.domain.usecase.profile.UpdatePostUseCase
import com.grace.app.domain.usecase.profile.UpdatePrayerUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
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

enum class MyContentTab { PRAYERS, POSTS, REFLECTIONS }

/**
 * One row in the "My Reflections" tab. The submission has the user's text +
 * timestamps; the meditation provides the surrounding context (title, theme,
 * scripture ref) for the card. meditation is nullable to defend against the
 * race where a meditation was deleted server-side but a submission remains.
 */
data class MyReflectionItem(
    val submission: MeditationSubmission,
    val meditation: WeeklyMeditation?
)

data class MyContentUiState(
    val activeTab: MyContentTab = MyContentTab.PRAYERS,
    val prayers: List<Prayer> = emptyList(),
    val posts: List<Post> = emptyList(),
    val reflections: List<MyReflectionItem> = emptyList(),
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val error: String? = null
)

sealed interface MyContentEvent {
    data class TabChanged(val tab: MyContentTab) : MyContentEvent
    data class UpdatePrayer(val prayerId: String, val content: String) : MyContentEvent
    data class DeletePrayer(val prayerId: String) : MyContentEvent
    data class MarkPrayerAnswered(val prayerId: String) : MyContentEvent
    data class UpdatePost(val postId: String, val content: String) : MyContentEvent
    data class DeletePost(val postId: String) : MyContentEvent
    data object Refresh : MyContentEvent
    data object DismissError : MyContentEvent
}

sealed interface MyContentEffect {
    data class ShowError(val message: String) : MyContentEffect
    data class ShowSuccess(val message: String) : MyContentEffect
}

@HiltViewModel
class MyContentViewModel @Inject constructor(
    private val getMyPrayersUseCase: GetMyPrayersUseCase,
    private val getMyPostsUseCase: GetMyPostsUseCase,
    private val updatePrayerUseCase: UpdatePrayerUseCase,
    private val deletePrayerUseCase: DeletePrayerUseCase,
    private val markPrayerAnsweredUseCase: MarkPrayerAnsweredUseCase,
    private val updatePostUseCase: UpdatePostUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val getMyMeditationSubmissionsUseCase: GetMyMeditationSubmissionsUseCase,
    private val getAllMeditationsUseCase: GetAllMeditationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyContentUiState())
    val uiState: StateFlow<MyContentUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<MyContentEffect>()
    val effect: SharedFlow<MyContentEffect> = _effect.asSharedFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // Sequential awaits — Kotlin 2.0 requires the explicit helper
            // below so smart-casting a star-projected Result.Success doesn't
            // lose the List<T> type.
            val prayers = getMyPrayersUseCase().dataOrEmpty()
            val posts = getMyPostsUseCase().dataOrEmpty()
            // Join my submissions with the meditation library so the cards
            // can show title + theme + week. Both flows are offline-first
            // via Room — first() grabs the latest cached value instantly.
            val subs = runCatching {
                getMyMeditationSubmissionsUseCase().first()
            }.getOrDefault(emptyList())
            val meds = runCatching {
                getAllMeditationsUseCase().first()
            }.getOrDefault(emptyList())
            val medsById = meds.associateBy { it.id }
            val reflections = subs.map {
                MyReflectionItem(it, medsById[it.meditationId])
            }
            _uiState.update {
                it.copy(
                    prayers = prayers,
                    posts = posts,
                    reflections = reflections,
                    isLoading = false
                )
            }
        }
    }

    fun onEvent(event: MyContentEvent) {
        when (event) {
            is MyContentEvent.TabChanged ->
                _uiState.update { it.copy(activeTab = event.tab) }
            is MyContentEvent.UpdatePrayer -> work {
                updatePrayerUseCase(event.prayerId, event.content)
                    .toToast("Prayer updated.")
            }
            is MyContentEvent.DeletePrayer -> work {
                deletePrayerUseCase(event.prayerId).toToast("Prayer deleted.")
            }
            is MyContentEvent.MarkPrayerAnswered -> work {
                markPrayerAnsweredUseCase(event.prayerId)
                    .toToast("Marked as answered.")
            }
            is MyContentEvent.UpdatePost -> work {
                updatePostUseCase(event.postId, event.content)
                    .toToast("Post updated.")
            }
            is MyContentEvent.DeletePost -> work {
                deletePostUseCase(event.postId).toToast("Post deleted.")
            }
            MyContentEvent.Refresh -> refresh()
            MyContentEvent.DismissError ->
                _uiState.update { it.copy(error = null) }
        }
    }

    private inline fun work(crossinline block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true) }
            block()
            _uiState.update { it.copy(isWorking = false) }
            refresh()
        }
    }

    /** Tiny typed helper so the smart-cast survives — Result<List<T>> in,
     *  List<T> out. Saves repeating `(result as? Success<List<T>>)?.data` casts. */
    private fun <T> Result<List<T>>.dataOrEmpty(): List<T> = when (this) {
        is Result.Success -> data
        else -> emptyList()
    }

    private suspend fun Result<Unit>.toToast(success: String) {
        when (this) {
            is Result.Success -> _effect.emit(MyContentEffect.ShowSuccess(success))
            is Result.Error -> _effect.emit(MyContentEffect.ShowError(message))
            Result.Loading -> Unit
        }
    }
}
