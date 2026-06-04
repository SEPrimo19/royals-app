package com.grace.app.domain.model

/**
 * One biblical event for Timeline Sorting. [chronologicalOrder] is the
 * canonical sort key — lower = earlier in Bible history. [approxYearText]
 * is shown on the reveal screen for educational context (optional;
 * Creation, Fall, etc. don't have meaningful dates).
 */
data class BibleEvent(
    val id: String,
    val title: String,
    val description: String? = null,
    val chronologicalOrder: Int,
    val approxYearText: String? = null,
    val sourceRef: String? = null,
    val isActive: Boolean = true
)
