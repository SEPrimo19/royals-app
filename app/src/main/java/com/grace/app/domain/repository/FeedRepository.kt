package com.grace.app.domain.repository

import com.grace.app.domain.model.Post
import com.grace.app.domain.model.PostType
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface FeedRepository {
    fun getPosts(): Flow<Result<List<Post>>>

    suspend fun createPost(
        type: PostType,
        content: String,
        imageUri: String?,
        verseRef: String?
    ): Result<Unit>

    suspend fun react(postId: String, reactionType: String): Result<Unit>

    suspend fun getMyPosts(): Result<List<Post>>

    suspend fun updatePostContent(postId: String, content: String): Result<Unit>

    suspend fun deletePost(postId: String): Result<Unit>
}
