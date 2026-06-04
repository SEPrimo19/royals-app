package com.grace.app.domain.model

/**
 * Aggregated personal progress numbers shown on My Progress.
 *
 * Each field is filled independently — a failure in one repo doesn't blank
 * the whole screen; that field just stays at 0 / null.
 */
data class ProgressSnapshot(
    val devoStreak: Int = 0,
    val devotionalsCompleted: Int = 0,
    val prayersPosted: Int = 0,
    val prayersAnswered: Int = 0,
    val prayersInterceded: Int = 0,
    val postsShared: Int = 0,
    val eventsAttended: Int = 0
)
