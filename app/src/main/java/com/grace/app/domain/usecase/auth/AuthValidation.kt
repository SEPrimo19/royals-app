package com.grace.app.domain.usecase.auth

internal val EMAIL_REGEX = Regex(
    "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
)

internal const val MIN_PASSWORD_LENGTH = 8
internal const val MIN_NAME_LENGTH = 2

internal fun isValidEmail(email: String): Boolean =
    email.isNotBlank() && EMAIL_REGEX.matches(email.trim())
