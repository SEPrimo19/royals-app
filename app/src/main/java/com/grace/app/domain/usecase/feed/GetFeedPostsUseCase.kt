package com.grace.app.domain.usecase.feed

import com.grace.app.domain.model.Post
import com.grace.app.domain.repository.FeedRepository
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFeedPostsUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    operator fun invoke(): Flow<Result<List<Post>>> = feedRepository.getPosts()
}
