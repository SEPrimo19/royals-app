package com.grace.app.domain.model

/**
 * One verse curated for Verse Scramble. Splitting [text] on whitespace
 * gives the chips the player reassembles, in correct order.
 */
data class BibleVerseScramble(
    val id: String,
    val reference: String,
    val text: String,
    val wordCount: Int,
    val isActive: Boolean = true
) {
    /** Words in correct order — the target answer. */
    val correctWords: List<String> get() = text.trim().split(Regex("\\s+"))
}
