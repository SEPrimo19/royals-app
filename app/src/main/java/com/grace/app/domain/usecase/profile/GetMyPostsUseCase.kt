package com.grace.app.domain.usecase.profile

import com.grace.app.domain.model.Post
import com.grace.app.domain.repository.FeedRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetMyPostsUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(): Result<List<Post>> = feedRepository.getMyPosts()
}
