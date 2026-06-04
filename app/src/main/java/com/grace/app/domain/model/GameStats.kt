package com.grace.app.domain.model

import java.time.LocalDateTime

/**
 * Per-user game progression. Distinct from devotional streak — see the
 * `bible-games-v1-design` memory for the rationale.
 *
 * Four independent daily rounds: Easy / Medium / Hard trivia + Daily Verse
 * (Fill-in-the-Blank). Each tracks its own 24h unlock. The streak fires
 * when this is the FIRST daily completion of the day across all four —
 * so a player who only ever does Easy still maintains their fire.
 */
data class GameStats(
    val userId: String,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalPoints: Long = 0L,
    val lastPlayedAt: LocalDateTime? = null,
    val lastEasyAt: LocalDateTime? = null,
    val lastMediumAt: LocalDateTime? = null,
    val lastHardAt: LocalDateTime? = null,
    val lastFitbAt: LocalDateTime? = null
) {
    fun lastFor(difficulty: GameDifficulty): LocalDateTime? = when (difficulty) {
        GameDifficulty.EASY -> lastEasyAt
        GameDifficulty.MEDIUM -> lastMediumAt
        GameDifficulty.HARD -> lastHardAt
    }

    /** True when 24h have passed since the user last completed this difficulty. */
    fun canPlayDaily(
        difficulty: GameDifficulty,
        now: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        val last = lastFor(difficulty) ?: return true
        return java.time.Duration.between(last, now).toHours() >= 24
    }

    /** True when 24h have passed since the user last completed Daily Verse. */
    fun canPlayFitb(now: LocalDateTime = LocalDateTime.now()): Boolean {
        val last = lastFitbAt ?: return true
        return java.time.Duration.between(last, now).toHours() >= 24
    }
}
