package com.grace.app.domain.model

enum class GameDifficulty(val points: Int) {
    EASY(10),
    MEDIUM(20),
    HARD(30);

    val dbValue: String get() = name.lowercase()

    companion object {
        fun fromDb(raw: String?): GameDifficulty =
            when (raw?.trim()?.lowercase()) {
                "easy" -> EASY
                "hard" -> HARD
                else -> MEDIUM
            }
    }
}
