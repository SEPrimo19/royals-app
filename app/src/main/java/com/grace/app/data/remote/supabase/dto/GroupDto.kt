package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("leader_id") val leaderId: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class GroupInsertDto(
    @SerialName("name") val name: String,
    @SerialName("leader_id") val leaderId: String?,
    @SerialName("description") val description: String? = null
)
