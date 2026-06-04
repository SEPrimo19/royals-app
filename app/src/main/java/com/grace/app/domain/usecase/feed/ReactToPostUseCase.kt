package com.grace.app.domain.usecase.feed

import com.grace.app.domain.repository.FeedRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class ReactToPostUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postId: String, reactionType: String): Result<Unit> =
        feedRepository.react(postId, reactionType)
}
