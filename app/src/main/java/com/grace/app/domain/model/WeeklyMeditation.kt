package com.grace.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/** Six rotating themes from the Royals 2026 meditation plan. */
enum class MeditationTheme {
    JESUS, EDUCATION, FAMILY, FRIENDS, CHURCH, RELATIONSHIPS;

    val label: String get() = name.lowercase().replaceFirstChar { it.uppercase() }
}

/**
 * One week's meditation entry. Admin-curated content; users don't write
 * these — they write [MeditationSubmission]s in response.
 */
data class WeeklyMeditation(
    val id: String,
    val weekNumber: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val theme: MeditationTheme,
    val title: String,
    val scriptureRef: String,
    val scriptureText: String,
    val reflectionPrompt: String,
    val furtherReadingLabel: String? = null,
    val furtherReadingUrl: String? = null,
    val isActive: Boolean = true
) {
    /** True when `today` falls within [startDate, endDate] inclusive. */
    fun isCurrent(today: LocalDate = LocalDate.now()): Boolean =
        !today.isBefore(startDate) && !today.isAfter(endDate)
}

/**
 * A user's written reflection on a [WeeklyMeditation]. Visible to the user,
 * their own cell leader (group_id match), and senior leaders — see
 * supabase/feature-weekly-meditation.sql for the RLS. Different privacy
 * scope from the devotional journal, which is encrypted/private.
 */
data class MeditationSubmission(
    val id: String,
    val userId: String,
    val meditationId: String,
    val reflectionText: String,
    val submittedAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    // Phase P.3 (Leader Proxy Mode) — non-null when a cell leader logged
    // this reflection on behalf of the member (paper journal handed in).
    // UI on member-only surfaces can show "(logged by {leader})".
    val submittedByProxy: String? = null
)
