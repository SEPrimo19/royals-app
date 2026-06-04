package com.grace.app.domain.model

/**
 * One "Who Am I?" character with their 4 progressive clues and 3 wrong-answer
 * names. Player peels clues one at a time and picks from 4 MCQ options
 * (correct name + the 3 distractors). Scoring: fewer clues used = higher
 * points (see [pointsForCluesUsed]).
 */
data class BibleCharacter(
    val id: String,
    val name: String,
    val category: QuestionCategory,
    val difficulty: GameDifficulty,
    val clues: List<String>,        // exactly 4 entries
    val distractors: List<String>,  // exactly 3 entries
    val sourceRef: String? = null,
    val explanation: String? = null,
    val isActive: Boolean = true
) {
    /** All four MCQ options (correct + distractors), in stable order seeded by id. */
    fun shuffledOptions(seed: Long = id.hashCode().toLong()): List<String> {
        val all = (distractors + name).distinct()
        return all.shuffled(kotlin.random.Random(seed))
    }
}

/**
 * Score for a Who-Am-I round. Mirrors the v9 SQL design:
 *   1 clue → 40 · 2 → 25 · 3 → 15 · 4 → 5
 * Above 4 (max attempts exhausted) is 0.
 */
fun pointsForCluesUsed(cluesUsed: Int): Int = when (cluesUsed) {
    1 -> 40
    2 -> 25
    3 -> 15
    4 -> 5
    else -> 0
}
