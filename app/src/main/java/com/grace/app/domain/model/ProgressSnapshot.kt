package com.grace.app.domain.model

data class ProgressSnapshot(
    val devoStreak: Int = 0,
    val devotionalsCompleted: Int = 0,
    val prayersPosted: Int = 0,
    val prayersAnswered: Int = 0,
    val prayersInterceded: Int = 0,
    val postsShared: Int = 0,
    val eventsAttended: Int = 0
)
