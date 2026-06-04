package com.grace.app.domain.model

/**
 * A single multiple-choice trivia question. Each question has exactly 4
 * options (UI constraint) and one correct index. `explanation` is optional
 * "why" text shown after the user answers — biblical literacy reinforcement,
 * not a punishment for a wrong answer.
 *
 * `isActive` is the soft-delete flag. Inactive questions never appear in
 * Daily Challenges or Practice; leaders can flip them via the curation
 * screen instead of hard-deleting (preserves history of past attempts).
 */
data class BibleQuestion(
    val id: String,
    val category: QuestionCategory,
    val difficulty: GameDifficulty,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String? = null,
    val sourceRef: String? = null,
    val isActive: Boolean = true
)
