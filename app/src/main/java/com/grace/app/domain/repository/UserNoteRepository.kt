package com.grace.app.domain.repository

import com.grace.app.domain.model.UserNote
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow

interface UserNoteRepository {

    suspend fun listVisibleNotes(): Result<List<UserNote>>

    suspend fun postMyNote(content: String): Result<Unit>

    suspend fun deleteMyNote(): Result<Unit>

    suspend fun toggleHeart(noteUserId: String): Result<Unit>

    suspend fun hideNote(noteUserId: String): Result<Unit>

    suspend fun unhideNote(noteUserId: String): Result<Unit>

    fun subscribeToNoteChanges(): Flow<Unit>
}
