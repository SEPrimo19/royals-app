package com.grace.app.domain.model

import java.time.LocalDate

enum class UserRole { MEMBER, CELL_LEADER, YOUTH_PRESIDENT, PASTOR, ADMIN }

data class User(
    val id: String,
    val email: String,                // empty string for proxy-only members
    val name: String,
    val avatarUrl: String?,
    val role: UserRole,
    val groupId: String?,
    val streak: Int,
    val lastDevoAt: LocalDate?,
    val fcmToken: String?,
    // Profile editor (Phase 1.3) — optional self-set fields.
    val bio: String? = null,
    val messengerUrl: String? = null,
    val messengerPublic: Boolean = false,
    // Compassion participant tracking — set at signup, editable by admin.
    val isCompassion: Boolean = false,
    val compassionNumber: String? = null,
    val emergencyContact: String? = null,
    // Leader Proxy Mode (Phase P.1) — TRUE means this user has no auth
    // account; only their cell leader can act on their behalf. birthdate +
    // sex are required for Compassion compliance reports even when the
    // member never logs in themselves.
    val isProxyOnly: Boolean = false,
    val birthdate: LocalDate? = null,
    val sex: String? = null
)
