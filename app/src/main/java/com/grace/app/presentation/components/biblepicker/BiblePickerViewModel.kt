package com.grace.app.presentation.components.biblepicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grace.app.domain.model.BibleBook
import com.grace.app.domain.model.BibleVerse
import com.grace.app.domain.usecase.bible.GetBibleBooksUseCase
import com.grace.app.domain.usecase.bible.GetBibleVersesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BiblePickerUiState(
    val isLoading: Boolean = true,
    val books: List<BibleBook> = emptyList(),
    val selectedBook: BibleBook? = null,
    val selectedChapter: Int? = null,
    val verses: List<BibleVerse> = emptyList(),
    val selectedVerses: Set<Int> = emptySet(),
    val error: String? = null
) {
    val canInsert: Boolean
        get() = selectedBook != null && selectedChapter != null && selectedVerses.isNotEmpty()
}

@HiltViewModel
class BiblePickerViewModel @Inject constructor(
    private val getBooks: GetBibleBooksUseCase,
    private val getVerses: GetBibleVersesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiblePickerUiState())
    val uiState: StateFlow<BiblePickerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val books = getBooks()
                _uiState.update { it.copy(isLoading = false, books = books) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Couldn't load the Bible")
                }
            }
        }
    }

    fun selectBook(book: BibleBook) {
        _uiState.update {
            it.copy(
                selectedBook = book,
                selectedChapter = null,
                verses = emptyList(),
                selectedVerses = emptySet()
            )
        }
    }

    fun selectChapter(chapter: Int) {
        val book = _uiState.value.selectedBook ?: return
        _uiState.update {
            it.copy(selectedChapter = chapter, verses = emptyList(), selectedVerses = emptySet())
        }
        viewModelScope.launch {
            try {
                val verses = getVerses(book.order, chapter)
                _uiState.update { it.copy(verses = verses) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Couldn't load that chapter") }
            }
        }
    }

    fun toggleVerse(verse: Int) {
        _uiState.update {
            val next = if (verse in it.selectedVerses) it.selectedVerses - verse
                       else it.selectedVerses + verse
            it.copy(selectedVerses = next)
        }
    }

    fun back() {
        _uiState.update {
            when {
                it.selectedChapter != null ->
                    it.copy(selectedChapter = null, verses = emptyList(), selectedVerses = emptySet())
                it.selectedBook != null ->
                    it.copy(selectedBook = null)
                else -> it
            }
        }
    }

    fun currentReference(): String? {
        val s = _uiState.value
        val book = s.selectedBook ?: return null
        val ch = s.selectedChapter ?: return null
        if (s.selectedVerses.isEmpty()) return null
        return "${book.name} $ch:${formatRange(s.selectedVerses.sorted())} (KJV)"
    }

    fun buildSelection(): Pair<String, String>? {
        val s = _uiState.value
        val ref = currentReference() ?: return null
        val text = s.verses
            .filter { it.verse in s.selectedVerses }
            .sortedBy { it.verse }
            .joinToString(" ") { it.text }
        return ref to text
    }

    companion object {
        fun formatRange(sorted: List<Int>): String {
            if (sorted.isEmpty()) return ""
            val parts = mutableListOf<String>()
            var start = sorted.first()
            var prev = start
            for (v in sorted.drop(1)) {
                if (v == prev + 1) {
                    prev = v
                } else {
                    parts += if (start == prev) "$start" else "$start-$prev"
                    start = v
                    prev = v
                }
            }
            parts += if (start == prev) "$start" else "$start-$prev"
            return parts.joinToString(",")
        }
    }
}
