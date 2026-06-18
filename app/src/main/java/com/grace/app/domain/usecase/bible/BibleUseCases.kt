package com.grace.app.domain.usecase.bible

import com.grace.app.domain.model.BibleBook
import com.grace.app.domain.model.BibleNote
import com.grace.app.domain.model.BibleSearchResult
import com.grace.app.domain.model.BibleVerse
import com.grace.app.domain.repository.BibleRepository
import com.grace.app.domain.repository.BibleStudyRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetBibleBooksUseCase @Inject constructor(
    private val repo: BibleRepository
) {
    suspend operator fun invoke(): List<BibleBook> = repo.getBooks()
}

class GetBibleVersesUseCase @Inject constructor(
    private val repo: BibleRepository
) {
    suspend operator fun invoke(bookOrder: Int, chapter: Int): List<BibleVerse> =
        repo.getVerses(bookOrder, chapter)
}

class SearchBibleUseCase @Inject constructor(
    private val repo: BibleRepository
) {
    suspend operator fun invoke(query: String, limit: Int = 100): List<BibleSearchResult> =
        repo.searchVerses(query, limit)
}


class GetChapterNoteUseCase @Inject constructor(
    private val repo: BibleStudyRepository
) {
    suspend operator fun invoke(bookOrder: Int, chapter: Int): Result<BibleNote?> =
        repo.getChapterNote(bookOrder, chapter)
}

class SaveChapterNoteUseCase @Inject constructor(
    private val repo: BibleStudyRepository
) {
    suspend operator fun invoke(bookOrder: Int, chapter: Int, content: String): Result<Unit> =
        repo.saveChapterNote(bookOrder, chapter, content)
}

class GetChapterHighlightsUseCase @Inject constructor(
    private val repo: BibleStudyRepository
) {
    suspend operator fun invoke(bookOrder: Int, chapter: Int): Result<Set<Int>> =
        repo.getChapterHighlights(bookOrder, chapter)
}

class ToggleHighlightUseCase @Inject constructor(
    private val repo: BibleStudyRepository
) {
    suspend operator fun invoke(bookOrder: Int, chapter: Int, verse: Int, on: Boolean): Result<Unit> =
        repo.setHighlight(bookOrder, chapter, verse, on)
}

class ListBibleNotesUseCase @Inject constructor(
    private val repo: BibleStudyRepository
) {
    suspend operator fun invoke(): Result<List<BibleNote>> = repo.listNotes()
}

class GetBibleNoteUseCase @Inject constructor(
    private val repo: BibleStudyRepository
) {
    suspend operator fun invoke(id: String): Result<BibleNote?> = repo.getNote(id)
}

class CreateSessionNoteUseCase @Inject constructor(
    private val repo: BibleStudyRepository
) {
    suspend operator fun invoke(title: String): Result<BibleNote> = repo.createSessionNote(title)
}

class SaveNoteUseCase @Inject constructor(
    private val repo: BibleStudyRepository
) {
    suspend operator fun invoke(id: String, title: String?, content: String): Result<Unit> =
        repo.saveNote(id, title, content)
}

class DeleteNoteUseCase @Inject constructor(
    private val repo: BibleStudyRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> = repo.deleteNote(id)
}
