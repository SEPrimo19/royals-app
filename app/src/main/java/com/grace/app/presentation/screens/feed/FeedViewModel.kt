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
    val myReactions: Map<String, String> = emptyMap(),
    val error: String? = null
)

sealed interface FeedEvent {
    data object ToggleCompose : FeedEvent
    data class DraftTextChanged(val text: String) : FeedEvent
    data class ImagePicked(val uri: String?) : FeedEvent
    data class VerseRefChanged(val ref: String) : FeedEvent
    data class InsertScripture(val reference: String, val text: String) : FeedEvent
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

    fun refresh() { observeFeed() }

    fun surfaceError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun onEvent(event: FeedEvent) {
        when (event) {
            FeedEvent.ToggleCompose ->
                _uiState.update { it.copy(showCompose = !it.showCompose) }
            is FeedEvent.DraftTextChanged ->
                _uiState.update { it.copy(draftText = event.text.take(MAX_POST_LEN)) }
            is FeedEvent.ImagePicked ->
                _uiState.update { it.copy(draftImageUri = event.uri) }
            is FeedEvent.VerseRefChanged ->
                _uiState.update {
                    it.copy(draftVerseRef = event.ref.take(MAX_REF_LEN).ifBlank { null })
                }
            is FeedEvent.InsertScripture ->
                _uiState.update { s ->
                    val body =
                        if (s.draftText.isBlank()) event.text
                        else s.draftText.trimEnd() + "\n\n" + event.text
                    s.copy(
                        draftText = body.take(MAX_POST_LEN),
                        draftVerseRef = event.reference.take(MAX_REF_LEN)
                    )
                }
            FeedEvent.SubmitPost -> submitPost()
            is FeedEvent.React -> react(event.postId, event.reactionType)
            FeedEvent.DismissError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun submitPost() {
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
        val cleanedText = cleanScripturePaste(s.draftText)
        viewModelScope.launch {
            when (val r = createPostUseCase(
                type, cleanedText, s.draftImageUri, s.draftVerseRef
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

    companion object {
        const val MAX_POST_LEN = 4000
        const val MAX_REF_LEN = 80

        fun cleanScripturePaste(raw: String): String =
            raw
                .replace(Regex("""https?://\S*bible\.com/\S*"""), "")
                .replace(Regex("""\[\d+]"""), "")
                .replace(Regex("""[ \t]{2,}"""), " ")
                .replace(Regex("""\n{3,}"""), "\n\n")
                .trim()
    }

    private fun react(postId: String, reactionType: String) {
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
                observeFeed()
            }
        }
    }
}
