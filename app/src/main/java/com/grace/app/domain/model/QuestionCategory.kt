package com.grace.app.domain.model

/** Trivia category — drives filter chips in the Practice tab. */
enum class QuestionCategory {
    OLD_TESTAMENT,
    NEW_TESTAMENT,
    CHARACTER;

    val dbValue: String get() = when (this) {
        OLD_TESTAMENT -> "old_testament"
        NEW_TESTAMENT -> "new_testament"
        CHARACTER -> "character"
    }

    companion object {
        fun fromDb(raw: String?): QuestionCategory =
            when (raw?.trim()?.lowercase()) {
                "new_testament" -> NEW_TESTAMENT
                "character" -> CHARACTER
                else -> OLD_TESTAMENT
            }
    }
}
