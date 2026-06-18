package com.grace.app.domain.model

data class MemoryCardPair(
    val id: String,
    val reference: String,
    val verseSnippet: String,
    val fullText: String? = null,
    val isActive: Boolean = true
)
