package com.grace.app.data.remote.supabase.dto

import com.grace.app.domain.model.BrowsableGroup
import com.grace.app.domain.model.IncomingJoinRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupJoinRequestInsertDto(
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id")  val userId: String
)

@Serializable
data class GroupJoinRequestDecisionDto(
    @SerialName("status")       val status: String,
    @SerialName("decided_by")   val decidedBy: String? = null,
    @SerialName("decided_note") val decidedNote: String? = null
)

@Serializable
data class BrowsableGroupRow(
    @SerialName("id")            val id: String,
    @SerialName("name")          val name: String,
    @SerialName("description")   val description: String? = null,
    @SerialName("leader_id")     val leaderId: String? = null,
    @SerialName("leader_name")   val leaderName: String = "—",
    @SerialName("member_count")  val memberCount: Long = 0,
    @SerialName("is_my_group")   val isMyGroup: Boolean = false,
    @SerialName("my_pending_id") val myPendingId: String? = null
)

@Serializable
data class IncomingJoinRequestRow(
    @SerialName("id")                 val id: String,
    @SerialName("group_id")           val groupId: String,
    @SerialName("group_name")         val groupName: String,
    @SerialName("user_id")            val userId: String,
    @SerialName("user_name")          val userName: String,
    @SerialName("user_email")         val userEmail: String,
    @SerialName("user_avatar_url")    val userAvatarUrl: String? = null,
    @SerialName("user_is_compassion") val userIsCompassion: Boolean = false,
    @SerialName("user_current_group") val userCurrentGroup: String? = null,
    @SerialName("created_at")         val createdAt: String
)

fun BrowsableGroupRow.toDomain() = BrowsableGroup(
    id = id,
    name = name,
    description = description,
    leaderId = leaderId,
    leaderName = leaderName,
    memberCount = memberCount.toInt(),
    isMyGroup = isMyGroup,
    myPendingRequestId = myPendingId
)

fun IncomingJoinRequestRow.toDomain() = IncomingJoinRequest(
    id = id,
    groupId = groupId,
    groupName = groupName,
    userId = userId,
    userName = userName,
    userEmail = userEmail,
    userAvatarUrl = userAvatarUrl,
    userIsCompassion = userIsCompassion,
    userCurrentGroup = userCurrentGroup,
    createdAtIso = createdAt
)
