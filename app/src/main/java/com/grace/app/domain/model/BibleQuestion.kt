package com.grace.app.domain.model

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
