package com.grace.app.domain.usecase.notes

import com.grace.app.domain.model.UserNote
import com.grace.app.domain.repository.UserNoteRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetVisibleNotesUseCase @Inject constructor(
    private val repo: UserNoteRepository
) {
    suspend operator fun invoke(): Result<List<UserNote>> = repo.listVisibleNotes()
}

class PostMyNoteUseCase @Inject constructor(
    private val repo: UserNoteRepository
) {
    suspend operator fun invoke(content: String): Result<Unit> = repo.postMyNote(content)
}

class DeleteMyNoteUseCase @Inject constructor(
    private val repo: UserNoteRepository
) {
    suspend operator fun invoke(): Result<Unit> = repo.deleteMyNote()
}

class ToggleNoteHeartUseCase @Inject constructor(
    private val repo: UserNoteRepository
) {
    suspend operator fun invoke(noteUserId: String): Result<Unit> =
        repo.toggleHeart(noteUserId)
}

class HideNoteUseCase @Inject constructor(
    private val repo: UserNoteRepository
) {
    suspend operator fun invoke(noteUserId: String): Result<Unit> =
        repo.hideNote(noteUserId)
}

class UnhideNoteUseCase @Inject constructor(
    private val repo: UserNoteRepository
) {
    suspend operator fun invoke(noteUserId: String): Result<Unit> =
        repo.unhideNote(noteUserId)
}

class SubscribeToNoteChangesUseCase @Inject constructor(
    private val repo: UserNoteRepository
) {
    operator fun invoke(): kotlinx.coroutines.flow.Flow<Unit> =
        repo.subscribeToNoteChanges()
}
