package com.grace.app.domain.usecase.profile

import com.grace.app.domain.repository.FeedRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class UpdatePostUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postId: String, content: String): Result<Unit> {
        val trimmed = content.trim()
        if (trimmed.length < 3) {
            return Result.Error("Post must be at least 3 characters.")
        }
        return feedRepository.updatePostContent(postId, trimmed)
    }
}
