package com.grace.app.domain.model

data class BibleCharacter(
    val id: String,
    val name: String,
    val category: QuestionCategory,
    val difficulty: GameDifficulty,
    val clues: List<String>,
    val distractors: List<String>,
    val sourceRef: String? = null,
    val explanation: String? = null,
    val isActive: Boolean = true
) {
    fun shuffledOptions(seed: Long = id.hashCode().toLong()): List<String> {
        val all = (distractors + name).distinct()
        return all.shuffled(kotlin.random.Random(seed))
    }
}

fun pointsForCluesUsed(cluesUsed: Int): Int = when (cluesUsed) {
    1 -> 40
    2 -> 25
    3 -> 15
    4 -> 5
    else -> 0
}
