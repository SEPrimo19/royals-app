package com.grace.app.domain.model

import java.time.LocalDateTime

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

    fun canPlayDaily(
        difficulty: GameDifficulty,
        now: LocalDateTime = LocalDateTime.now()
    ): Boolean {
        val last = lastFor(difficulty) ?: return true
        return !now.isBefore(nextDailyUnlock(last))
    }

    fun canPlayFitb(now: LocalDateTime = LocalDateTime.now()): Boolean {
        val last = lastFitbAt ?: return true
        return !now.isBefore(nextDailyUnlock(last))
    }

    companion object {
        fun nextDailyUnlock(last: LocalDateTime): LocalDateTime =
            last.toLocalDate().plusDays(1).atStartOfDay()
    }
}
