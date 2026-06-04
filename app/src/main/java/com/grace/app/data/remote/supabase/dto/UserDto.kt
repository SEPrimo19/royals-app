package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    // Nullable because Leader Proxy Mode (feature-leader-proxy.sql) made
    // users.email nullable for proxy-only members. Real accounts still
    // have non-null emails. Mapper normalizes null → "" when building the
    // domain User so call sites don't have to handle two empty states.
    @SerialName("email") val email: String? = null,
    @SerialName("name") val name: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("role") val role: String = "member",
    @SerialName("group_id") val groupId: String? = null,
    @SerialName("fcm_token") val fcmToken: String? = null,
    @SerialName("streak") val streak: Int = 0,
    @SerialName("last_devo_at") val lastDevoAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    // Profile editor fields (added by feature-profile-edit.sql).
    @SerialName("bio") val bio: String? = null,
    @SerialName("messenger_url") val messengerUrl: String? = null,
    @SerialName("messenger_public") val messengerPublic: Boolean = false,
    // Compassion participant tracking (added by feature-compassion.sql).
    // Defaults ensure rows from before this migration decode cleanly.
    @SerialName("is_compassion") val isCompassion: Boolean = false,
    @SerialName("compassion_number") val compassionNumber: String? = null,
    @SerialName("emergency_contact") val emergencyContact: String? = null,
    // Leader Proxy Mode (feature-leader-proxy.sql).
    @SerialName("is_proxy_only") val isProxyOnly: Boolean = false,
    @SerialName("created_by_proxy") val createdByProxy: String? = null,
    @SerialName("birthdate") val birthdate: String? = null,    // ISO yyyy-MM-dd
    @SerialName("sex") val sex: String? = null                  // 'M' | 'F'
)
