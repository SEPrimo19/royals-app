package com.grace.app.domain.model

data class Group(
    val id: String,
    val name: String,
    val leaderId: String?,
    val description: String?
)
