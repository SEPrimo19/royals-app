package com.grace.app.domain.model

import java.time.LocalDateTime

/**
 * A row on the leader's My Members directory — a single mentee plus
 * the most recent signal we have about their walk (last weekly check-in).
 *
 * Mentorship outreach happens off-platform via Messenger (see
 * `User.messengerUrl`), so we don't track in-app conversation state here.
 */
data class Mentee(
    val user: User,
    val lastCheckInAt: LocalDateTime? = null
)
