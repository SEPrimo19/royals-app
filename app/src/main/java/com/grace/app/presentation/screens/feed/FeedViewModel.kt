package com.grace.app.presentation.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.Post
import com.grace.app.domain.model.PostType
import com.grace.app.domain.usecase.feed.CreatePostUseCase
import com.grace.app.domain.usecase.feed.GetFeedPostsUseCase
import com.grace.app.domain.usecase.feed.ReactToPostUseCase
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

data class FeedUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val showCompose: Boolean = false,
    val draftText: String = "",
    val draftImageUri: String? = null,
    val draftVerseRef: String? = null,
    val isPosting: Boolean = false,
    // Optimistic local overlay of the current user's reaction per post.
    val myReactions: Map<String, String> = emptyMap(),
    val error: String? = null
)

sealed interface FeedEvent {
    data object ToggleCompose : FeedEvent
    data class DraftTextChanged(val text: String) : FeedEvent
    data class ImagePicked(val uri: String?) : FeedEvent
    data class VerseRefChanged(val ref: String) : FeedEvent
    data object SubmitPost : FeedEvent
    data class React(val postId: String, val reactionType: String) : FeedEvent
    data object DismissError : FeedEvent
}

sealed interface FeedEffect {
    data object PostCreated : FeedEffect
    data class ShowError(val message: String) : FeedEffect
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getFeedPostsUseCase: GetFeedPostsUseCase,
    private val createPostUseCase: CreatePostUseCase,
    private val reactToPostUseCase: ReactToPostUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<FeedEffect>()
    val effect: SharedFlow<FeedEffect> = _effect.asSharedFlow()

    private var feedJob: kotlinx.coroutines.Job? = null

    init { observeFeed() }

    private fun observeFeed() {
        feedJob?.cancel()
        feedJob = viewModelScope.launch {
            getFeedPostsUseCase().collect { result ->
                when (result) {
                    is Result.Success ->
                        _uiState.update {
                            it.copy(posts = result.data, isLoading = false, error = null)
                        }
                    is Result.Error ->
                        _uiState.update { it.copy(isLoading = false, error = result.message) }
                    Result.Loading ->
                        _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Re-runs the remote fetch. The bottom-bar saveState/restoreState keeps
     * this VM alive across tabs, so init's fetch only fires once per app
     * lifetime. The screen calls this on each entry so new posts, deletes,
     * and updated reaction counts always show up.
     */
    fun refresh() { observeFeed() }

    /** Lets the screen route effect-bus errors into the visible error line. */
    fun surfaceError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun onEvent(event: FeedEvent) {
        when (event) {
            FeedEvent.ToggleCompose ->
                _uiState.update { it.copy(showCompose = !it.showCompose) }
            is FeedEvent.DraftTextChanged ->
                _uiState.update { it.copy(draftText = event.text) }
            is FeedEvent.ImagePicked ->
                _uiState.update { it.copy(draftImageUri = event.uri) }
            is FeedEvent.VerseRefChanged ->
                _uiState.update { it.copy(draftVerseRef = event.ref.ifBlank { null }) }
            FeedEvent.SubmitPost -> submitPost()
            is FeedEvent.React -> react(event.postId, event.reactionType)
            FeedEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun submitPost() {
        // Atomic claim — without this a rapid double-tap can race past the
        // simple `if (isPosting) return` check, and EACH tap uploads its own
        // image file before the first call sets isPosting=true. That's what
        // produced two identical files in the storage bucket.
        var claimed = false
        _uiState.update { current ->
            if (current.isPosting) current
            else {
                claimed = true
                current.copy(isPosting = true, error = null)
            }
        }
        if (!claimed) return
        val s = _uiState.value
        val type = when {
            s.draftImageUri != null -> PostType.PHOTO
            s.draftVerseRef != null -> PostType.SCRIPTURE
            else -> PostType.TEXT
        }
        viewModelScope.launch {
            when (val r = createPostUseCase(
                type, s.draftText, s.draftImageUri, s.draftVerseRef
            )) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isPosting = false,
                            showCompose = false,
                            draftText = "",
                            draftImageUri = null,
                            draftVerseRef = null
                        )
                    }
                    _effect.emit(FeedEffect.PostCreated)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isPosting = false, error = r.message) }
                    _effect.emit(FeedEffect.ShowError(r.message))
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun react(postId: String, reactionType: String) {
        // Optimistic toggle of the local overlay.
        _uiState.update {
            val current = it.myReactions[postId]
            val next =
                if (current == reactionType) it.myReactions - postId
                else it.myReactions + (postId to reactionType)
            it.copy(myReactions = next)
        }
        viewModelScope.launch {
            val r = reactToPostUseCase(postId, reactionType)
            if (r is Result.Error) {
                _effect.emit(FeedEffect.ShowError(r.message))
            } else {
                // Counts come from the server aggregate — refresh to pull them.
                observeFeed()
            }
        }
    }
}
