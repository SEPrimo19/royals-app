package com.grace.app.domain.model

import java.time.LocalDate

enum class UserRole { MEMBER, CELL_LEADER, COUNCIL, YOUTH_PRESIDENT, PASTOR, ADMIN }

data class User(
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String?,
    val role: UserRole,
    val groupId: String?,
    val streak: Int,
    val lastDevoAt: LocalDate?,
    val fcmToken: String?,
    val bio: String? = null,
    val messengerUrl: String? = null,
    val messengerPublic: Boolean = false,
    val isCompassion: Boolean = false,
    val compassionNumber: String? = null,
    val emergencyContact: String? = null,
    val isProxyOnly: Boolean = false,
    val birthdate: LocalDate? = null,
    val sex: String? = null
)
