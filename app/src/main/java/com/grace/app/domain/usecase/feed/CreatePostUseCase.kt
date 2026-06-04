package com.grace.app.domain.usecase.feed

import com.grace.app.domain.model.PostType
import com.grace.app.domain.repository.FeedRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class CreatePostUseCase @Inject constructor(
    private val feedRepository: FeedRepository
) {
    suspend operator fun invoke(
        type: PostType,
        content: String,
        imageUri: String?,
        verseRef: String?
    ): Result<Unit> {
        if (content.isBlank() && imageUri == null) {
            return Result.Error("Write something or add a photo first.")
        }
        return feedRepository.createPost(type, content.trim(), imageUri, verseRef)
    }
}
