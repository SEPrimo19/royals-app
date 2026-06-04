package com.grace.app.domain.model


data class BiblePassage(
    val id: String,
    val reference: String,
    val text: String,
    val blankWord: String,
    val distractors: List<String>,
    val isActive: Boolean = true
) {
    /** All 4 options for the player, shuffled with a per-call seed. */
    fun optionsShuffled(seed: Long = System.currentTimeMillis()): List<String> {
        val all = (distractors + blankWord).distinct()
        return all.shuffled(kotlin.random.Random(seed))
    }
}
