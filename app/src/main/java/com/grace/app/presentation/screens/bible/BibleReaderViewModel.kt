package com.grace.app.presentation.screens.bible

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.domain.model.BibleBook
import com.grace.app.domain.model.BibleSearchResult
import com.grace.app.domain.model.BibleVerse
import com.grace.app.domain.usecase.bible.GetBibleBooksUseCase
import com.grace.app.domain.usecase.bible.GetBibleVersesUseCase
import com.grace.app.domain.usecase.bible.GetChapterHighlightsUseCase
import com.grace.app.domain.usecase.bible.GetChapterNoteUseCase
import com.grace.app.domain.usecase.bible.SaveChapterNoteUseCase
import com.grace.app.domain.usecase.bible.SearchBibleUseCase
import com.grace.app.domain.usecase.bible.ToggleHighlightUseCase
import com.grace.app.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BibleReaderUiState(
    val isLoading: Boolean = true,
    val books: List<BibleBook> = emptyList(),
    val bookOrder: Int = 43,
    val chapter: Int = 1,
    val verses: List<BibleVerse> = emptyList(),
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<BibleSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val scrollTargetVerse: Int? = null,
    val highlights: Set<Int> = emptySet(),
    val noteContent: String = "",
    val notesPaneOpen: Boolean = false,
    val selectedVerse: BibleVerse? = null,
    val inSelectionMode: Boolean = false,
    val imageSelection: Set<Int> = emptySet()
) {
    val currentBook: BibleBook? get() = books.firstOrNull { it.order == bookOrder }
    val title: String get() = currentBook?.let { "${it.name} $chapter" } ?: "Bible"
    val hasPrev: Boolean get() = !(bookOrder <= 1 && chapter <= 1)
    val hasNext: Boolean
        get() = currentBook?.let { bookOrder < 66 || chapter < it.chapterCount } ?: false
}

@HiltViewModel
class BibleReaderViewModel @Inject constructor(
    private val getBooks: GetBibleBooksUseCase,
    private val getVerses: GetBibleVersesUseCase,
    private val searchBible: SearchBibleUseCase,
    private val getChapterNote: GetChapterNoteUseCase,
    private val saveChapterNote: SaveChapterNoteUseCase,
    private val getChapterHighlights: GetChapterHighlightsUseCase,
    private val toggleHighlightUseCase: ToggleHighlightUseCase,
    private val prefs: UserPreferencesRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(BibleReaderUiState())
    val uiState: StateFlow<BibleReaderUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var noteSaveJob: Job? = null
    private var loadedNoteContent: String = ""

    init {
        viewModelScope.launch {
            try {
                val books = getBooks()
                _uiState.update { it.copy(books = books) }
                loadChapter(prefs.bibleLastBook.first(), prefs.bibleLastChapter.first(), persist = false)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Couldn't load the Bible")
                }
            }
        }
    }

    fun jumpTo(bookOrder: Int, chapter: Int) = loadChapter(bookOrder, chapter, persist = true)

    fun prevChapter() {
        val s = _uiState.value
        when {
            s.chapter > 1 -> loadChapter(s.bookOrder, s.chapter - 1, persist = true)
            s.bookOrder > 1 -> {
                val prev = s.books.firstOrNull { it.order == s.bookOrder - 1 } ?: return
                loadChapter(prev.order, prev.chapterCount, persist = true)
            }
        }
    }

    fun nextChapter() {
        val s = _uiState.value
        val book = s.currentBook ?: return
        when {
            s.chapter < book.chapterCount -> loadChapter(s.bookOrder, s.chapter + 1, persist = true)
            s.bookOrder < 66 -> loadChapter(s.bookOrder + 1, 1, persist = true)
        }
    }


    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = query) }
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        _uiState.update { it.copy(isSearching = true) }
        searchJob = viewModelScope.launch {
            delay(250)
            val results = runCatching { searchBible(query, limit = 100) }.getOrDefault(emptyList())
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    fun openSearchResult(result: BibleSearchResult) {
        clearSearch()
        loadChapter(result.bookOrder, result.chapter, persist = true, scrollToVerse = result.verse)
    }

    private fun loadChapter(
        bookOrder: Int,
        chapter: Int,
        persist: Boolean,
        scrollToVerse: Int? = null
    ) {
        flushPendingNote()
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val verses = getVerses(bookOrder, chapter)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        bookOrder = bookOrder,
                        chapter = chapter,
                        verses = verses,
                        scrollTargetVerse = scrollToVerse,
                        error = null
                    )
                }
                if (persist) prefs.setBiblePosition(bookOrder, chapter)
                loadStudyForChapter(bookOrder, chapter)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Couldn't load that chapter")
                }
            }
        }
    }


    private suspend fun loadStudyForChapter(bookOrder: Int, chapter: Int) {
        val hl = (getChapterHighlights(bookOrder, chapter) as? Result.Success)?.data ?: emptySet()
        val note = (getChapterNote(bookOrder, chapter) as? Result.Success)?.data
        loadedNoteContent = note?.content ?: ""
        _uiState.update { it.copy(highlights = hl, noteContent = note?.content ?: "") }
    }

    fun toggleNotesPane() {
        _uiState.update { it.copy(notesPaneOpen = !it.notesPaneOpen) }
    }

    fun onVerseTap(verse: BibleVerse) {
        val s = _uiState.value
        if (s.inSelectionMode) {
            val next = if (verse.verse in s.imageSelection) s.imageSelection - verse.verse
                       else s.imageSelection + verse.verse
            _uiState.update { it.copy(imageSelection = next, inSelectionMode = next.isNotEmpty()) }
        } else {
            _uiState.update { it.copy(selectedVerse = verse) }
        }
    }

    fun onVerseLongPress(verse: BibleVerse) {
        _uiState.update {
            it.copy(
                inSelectionMode = true,
                imageSelection = it.imageSelection + verse.verse,
                selectedVerse = null
            )
        }
    }

    fun exitSelection() {
        _uiState.update { it.copy(inSelectionMode = false, imageSelection = emptySet()) }
    }

    fun buildSelectionForImage(): Pair<String, String>? {
        val s = _uiState.value
        if (s.imageSelection.isEmpty()) return null
        val sorted = s.imageSelection.sorted()
        val text = s.verses
            .filter { it.verse in s.imageSelection }
            .sortedBy { it.verse }
            .joinToString(" ") { it.text }
        val ref = "${s.currentBook?.name ?: ""} ${s.chapter}:${formatRange(sorted)}"
        return text to ref
    }

    private fun formatRange(sorted: List<Int>): String {
        if (sorted.isEmpty()) return ""
        val parts = mutableListOf<String>()
        var start = sorted.first()
        var prev = start
        for (v in sorted.drop(1)) {
            if (v == prev + 1) prev = v
            else { parts += if (start == prev) "$start" else "$start-$prev"; start = v; prev = v }
        }
        parts += if (start == prev) "$start" else "$start-$prev"
        return parts.joinToString(",")
    }

    fun dismissVerseSheet() {
        _uiState.update { it.copy(selectedVerse = null) }
    }

    fun toggleHighlight(verse: Int) {
        val s = _uiState.value
        val on = verse !in s.highlights
        _uiState.update {
            it.copy(
                highlights = if (on) it.highlights + verse else it.highlights - verse,
                selectedVerse = null
            )
        }
        viewModelScope.launch { toggleHighlightUseCase(s.bookOrder, s.chapter, verse, on) }
    }

    fun onNoteChanged(text: String) {
        _uiState.update { it.copy(noteContent = text) }
        scheduleNoteSave()
    }

    fun addVerseToNote(verse: BibleVerse) {
        val s = _uiState.value
        val ref = "${s.currentBook?.name ?: ""} ${s.chapter}:${verse.verse} (KJV)"
        val addition = "$ref — ${verse.text}"
        val merged =
            if (s.noteContent.isBlank()) addition
            else s.noteContent.trimEnd() + "\n\n" + addition
        _uiState.update {
            it.copy(noteContent = merged, notesPaneOpen = true, selectedVerse = null)
        }
        scheduleNoteSave()
    }

    private fun scheduleNoteSave() {
        noteSaveJob?.cancel()
        noteSaveJob = viewModelScope.launch {
            delay(800)
            val s = _uiState.value
            saveChapterNote(s.bookOrder, s.chapter, s.noteContent)
            loadedNoteContent = s.noteContent
        }
    }

    private fun flushPendingNote() {
        noteSaveJob?.cancel()
        val s = _uiState.value
        if (s.noteContent != loadedNoteContent) {
            val book = s.bookOrder
            val chapter = s.chapter
            val content = s.noteContent
            loadedNoteContent = content
            viewModelScope.launch { saveChapterNote(book, chapter, content) }
        }
    }
}
