package com.grace.app.domain.model

import java.time.LocalDateTime

/**
 * A weekly check-in submission. `answers` is the raw JSONB map saved
 * by the member — keys are stable (q1/q2/q3) so leaders can render
 * them next to the canonical question text.
 */
data class CheckIn(
    val id: String,
    val userId: String,
    val leaderId: String?,
    val answers: Map<String, String>,
    val submittedAt: LocalDateTime
)
