package com.grace.app.domain.model

enum class JoinRequestStatus { PENDING, APPROVED, REJECTED, CANCELLED, EXPIRED }

data class BrowsableGroup(
    val id: String,
    val name: String,
    val description: String?,
    val leaderId: String?,
    val leaderName: String,
    val memberCount: Int,
    val isMyGroup: Boolean,
    val myPendingRequestId: String?
)

data class IncomingJoinRequest(
    val id: String,
    val groupId: String,
    val groupName: String,
    val userId: String,
    val userName: String,
    val userEmail: String,
    val userAvatarUrl: String?,
    val userIsCompassion: Boolean,
    val userCurrentGroup: String?,
    val createdAtIso: String
)
