package com.grace.app.domain.model

import java.time.LocalDate

data class Devotional(
    val id: String,
    val scheduledDate: LocalDate,
    val title: String,
    val verseRef: String,
    val verseText: String,
    val reflection: String,
    val prayerStarter: String,
    val journalPrompt: String,
    val planId: String?
)
