package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String? = null,
    @SerialName("name") val name: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("role") val role: String = "member",
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("fcm_token") val fcmToken: String? = null,
    @SerialName("streak") val streak: Int = 0,
    @SerialName("last_devo_at") val lastDevoAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("messenger_url") val messengerUrl: String? = null,
    @SerialName("messenger_public") val messengerPublic: Boolean = false,
    @SerialName("is_compassion") val isCompassion: Boolean = false,
    @SerialName("compassion_number") val compassionNumber: String? = null,
    @SerialName("emergency_contact") val emergencyContact: String? = null,
    @SerialName("is_proxy_only") val isProxyOnly: Boolean = false,
    @SerialName("created_by_proxy") val createdByProxy: String? = null,
    @SerialName("birthdate") val birthdate: String? = null,
    @SerialName("sex") val sex: String? = null
)
