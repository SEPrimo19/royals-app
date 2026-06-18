package com.grace.app.domain.model

import java.time.LocalDate

data class JournalEntry(
    val devoId: String,
    val completedAt: LocalDate,
    val devoTitle: String,
    val verseRef: String,
    val verseText: String,
    val journalPrompt: String,
    val entry: String,
    val isReadable: Boolean
)
