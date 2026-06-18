package com.grace.app.domain.model

data class BibleEvent(
    val id: String,
    val title: String,
    val description: String? = null,
    val chronologicalOrder: Int,
    val approxYearText: String? = null,
    val sourceRef: String? = null,
    val isActive: Boolean = true
)
