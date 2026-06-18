package com.grace.app.domain.model

enum class GameMode {
    TRIVIA,
    FILL_IN_THE_BLANK,
    WHO_AM_I,
    MEMORY_MATCH,
    VERSE_SCRAMBLE,
    TIMELINE_SORT;

    val dbValue: String get() = when (this) {
        TRIVIA -> "trivia"
        FILL_IN_THE_BLANK -> "fitb"
        WHO_AM_I -> "who_am_i"
        MEMORY_MATCH -> "memory_match"
        VERSE_SCRAMBLE -> "verse_scramble"
        TIMELINE_SORT -> "timeline_sort"
    }

    companion object {
        fun fromDb(raw: String?): GameMode = when (raw?.trim()?.lowercase()) {
            "fitb" -> FILL_IN_THE_BLANK
            "who_am_i" -> WHO_AM_I
            "memory_match" -> MEMORY_MATCH
            "verse_scramble" -> VERSE_SCRAMBLE
            "timeline_sort" -> TIMELINE_SORT
            else -> TRIVIA
        }
    }
}
