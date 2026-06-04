package com.grace.app.domain.usecase.profile

import com.grace.app.domain.repository.FeedRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class DeletePostUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(postId: String): Result<Unit> =
        feedRepository.deletePost(postId)
}
