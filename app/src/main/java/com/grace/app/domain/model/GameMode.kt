package com.grace.app.domain.model

/** Game modes shipped in v1. More land in v2+ (see bible-games-vision memory). */
enum class GameMode {
    TRIVIA,
    FILL_IN_THE_BLANK,
    /** v2: "Who Am I?" — 4 progressive clues about a biblical character. */
    WHO_AM_I,
    /** v2: Memory Cards — match Bible reference ↔ verse snippet. */
    MEMORY_MATCH,
    /** v2: Verse Scramble — reassemble a scrambled verse word-by-word. */
    VERSE_SCRAMBLE,
    /** v2: Timeline Sorting — arrange 5 biblical events in chronological order. */
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
