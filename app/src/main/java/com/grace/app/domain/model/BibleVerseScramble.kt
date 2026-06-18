package com.grace.app.domain.model

data class BibleVerseScramble(
    val id: String,
    val reference: String,
    val text: String,
    val wordCount: Int,
    val isActive: Boolean = true
) {
    val correctWords: List<String> get() = text.trim().split(Regex("\\s+"))
}
