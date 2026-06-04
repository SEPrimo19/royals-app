package com.grace.app.domain.repository

import com.grace.app.domain.model.Post
import com.grace.app.domain.model.PostType
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getPosts(): Flow<Result<List<Post>>>

    /**
     * [imageUri] is the string form of the picked content URI (or null). The
     * domain layer stays Android-free; the data layer resolves it to bytes.
     */
    suspend fun createPost(
        type: PostType,
        content: String,
        imageUri: String?,
        verseRef: String?
    ): Result<Unit>

    suspend fun react(postId: String, reactionType: String): Result<Unit>

    /** Posts authored by the current user. Used by My Content screen. */
    suspend fun getMyPosts(): Result<List<Post>>

    /** Owner-only (RLS-enforced): update the post's content. */
    suspend fun updatePostContent(postId: String, content: String): Result<Unit>

    /** Owner-only (RLS-enforced): delete the post + cascades reactions/comments. */
    suspend fun deletePost(postId: String): Result<Unit>
}
