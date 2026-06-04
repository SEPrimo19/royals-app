package com.grace.app.domain.model

import java.time.LocalDateTime

/** One answered question, persisted to power scoring + leaderboards. */
data class GameAttempt(
    val id: String,
    val userId: String,
    val mode: GameMode,
    val questionId: String? = null,
    val passageId: String? = null,
    val correct: Boolean,
    val pointsEarned: Int,
    val isDaily: Boolean,
    val playedAt: LocalDateTime
)
