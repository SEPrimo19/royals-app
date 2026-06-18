package com.grace.app.domain.model

import java.time.LocalDateTime

data class Mentee(
    val user: User,
    val lastCheckInAt: LocalDateTime? = null
)
