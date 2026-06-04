package com.grace.app.domain.model

import java.time.LocalDateTime

enum class PrayerCategory { FAMILY, SCHOOL, FAITH, HEALTH, PERSONAL, NATIONS }

enum class PrayerStatus { ACTIVE, ANSWERED, ARCHIVED }

data class Prayer(
    val id: String,
    // null for anonymous prayers — identity is never surfaced to the UI layer.
    val userId: String?,
    val userName: String?,
    val content: String,
    val isAnonymous: Boolean,
    val category: PrayerCategory,
    val status: PrayerStatus,
    val prayCount: Int,
    val isFlagged: Boolean,
    val createdAt: LocalDateTime,
    // Phase P.3 (Leader Proxy Mode) — when a leader posts on behalf of a
    // member. The id is stored in the DB; the leader's name is populated
    // by the repository via a batched join lookup so UI can render
    // "(via {name})" without an extra fetch per prayer.
    val postedByProxy: String? = null,
    val proxyLeaderName: String? = null
)
