package com.grace.app.domain.repository

import com.grace.app.domain.model.BibleNote
import com.grace.app.domain.util.Result

interface BibleStudyRepository {

    suspend fun getChapterNote(bookOrder: Int, chapter: Int): Result<BibleNote?>

    suspend fun saveChapterNote(bookOrder: Int, chapter: Int, content: String): Result<Unit>

    suspend fun getChapterHighlights(bookOrder: Int, chapter: Int): Result<Set<Int>>

    suspend fun setHighlight(bookOrder: Int, chapter: Int, verse: Int, on: Boolean): Result<Unit>


    suspend fun listNotes(): Result<List<BibleNote>>

    suspend fun getNote(id: String): Result<BibleNote?>

    suspend fun createSessionNote(title: String): Result<BibleNote>

    suspend fun saveNote(id: String, title: String?, content: String): Result<Unit>

    suspend fun deleteNote(id: String): Result<Unit>
}
