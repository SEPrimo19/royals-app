package com.grace.app.domain.model

data class BibleBook(
    val order: Int,
    val name: String,
    val testament: String,
    val chapterCount: Int
)

data class BibleVerse(
    val verse: Int,
    val text: String
)

data class BibleNote(
    val id: String,
    val title: String?,
    val bookOrder: Int?,
    val chapter: Int?,
    val content: String
) {
    val isSession: Boolean get() = bookOrder == null
}

data class BibleSearchResult(
    val bookOrder: Int,
    val bookName: String,
    val chapter: Int,
    val verse: Int,
    val text: String
) {
    val reference: String get() = "$bookName $chapter:$verse"
}
