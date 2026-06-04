package com.grace.app.domain.model

/**
 * One Bible reference paired with a short verse snippet for Memory Cards.
 * A board uses 6 random pairs (12 cards total) on a 3×4 grid.
 *
 * [fullText] is optional — if present it's shown on the board-complete
 * card as the verse in context.
 */
data class MemoryCardPair(
    val id: String,
    val reference: String,
    val verseSnippet: String,
    val fullText: String? = null,
    val isActive: Boolean = true
)
