package com.grace.app.data.remote.supabase.dto.mapper

import com.grace.app.data.local.entity.UserEntity
import com.grace.app.data.remote.supabase.dto.UserDto
import com.grace.app.domain.model.User
import com.grace.app.domain.model.UserRole

// Unknown/garbage role strings must never crash — fall back to MEMBER.
private fun parseRole(raw: String?): UserRole = when (raw?.trim()?.lowercase()) {
    "cell_leader" -> UserRole.CELL_LEADER
    "youth_president" -> UserRole.YOUTH_PRESIDENT
    "pastor" -> UserRole.PASTOR
    "admin" -> UserRole.ADMIN
    else -> UserRole.MEMBER
}

private fun UserRole.toDbValue(): String = name.lowercase()

fun UserDto.toDomain(): User = User(
    id = id,
    // Proxy members can have NULL email at the DB layer; normalize to ""
    // for the domain model so UI code doesn't have to handle two empty
    // states. Real accounts always have a non-null value.
    email = email ?: "",
    name = name,
    avatarUrl = avatarUrl,
    role = parseRole(role),
    groupId = groupId,
    streak = streak,
    lastDevoAt = parseDateOrNull(lastDevoAt),
    fcmToken = fcmToken,
    bio = bio,
    messengerUrl = messengerUrl,
    messengerPublic = messengerPublic,
    isCompassion = isCompassion,
    compassionNumber = compassionNumber,
    emergencyContact = emergencyContact,
    isProxyOnly = isProxyOnly,
    birthdate = parseDateOrNull(birthdate),
    sex = sex
)

fun UserDto.toEntity(): UserEntity = UserEntity(
    id = id,
    email = email ?: "",
    name = name,
    avatarUrl = avatarUrl,
    role = role,
    groupId = groupId,
    fcmToken = fcmToken,
    streak = streak,
    lastDevoAt = lastDevoAt,
    createdAt = createdAt,
    isCompassion = isCompassion,
    compassionNumber = compassionNumber,
    emergencyContact = emergencyContact,
    isProxyOnly = isProxyOnly,
    birthdate = birthdate,
    sex = sex
)

fun UserEntity.toDomain(): User = User(
    id = id,
    email = email,
    name = name,
    avatarUrl = avatarUrl,
    role = parseRole(role),
    groupId = groupId,
    streak = streak,
    lastDevoAt = parseDateOrNull(lastDevoAt),
    fcmToken = fcmToken,
    isCompassion = isCompassion,
    compassionNumber = compassionNumber,
    emergencyContact = emergencyContact,
    isProxyOnly = isProxyOnly,
    birthdate = parseDateOrNull(birthdate),
    sex = sex
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    email = email,
    name = name,
    avatarUrl = avatarUrl,
    role = role.toDbValue(),
    groupId = groupId,
    fcmToken = fcmToken,
    streak = streak,
    lastDevoAt = lastDevoAt?.toString(),
    createdAt = null,
    isCompassion = isCompassion,
    compassionNumber = compassionNumber,
    emergencyContact = emergencyContact,
    isProxyOnly = isProxyOnly,
    birthdate = birthdate?.toString(),
    sex = sex
)
