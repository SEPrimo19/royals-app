package com.grace.app.domain.model

import java.time.LocalDateTime

data class CheckIn(
    val id: String,
    val userId: String,
    val leaderId: String?,
    val answers: Map<String, String>,
    val submittedAt: LocalDateTime
)
