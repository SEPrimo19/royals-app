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

/**
 * Insert payload — no `id` field so Postgres uses its `DEFAULT
 * uuid_generate_v4()`. Sending `id = ""` makes Postgres choke on
 * "invalid input syntax for type uuid" and we get a misleading generic
 * "Couldn't reach the server" error in the UI.
 */
@Serializable
data class GroupInsertDto(
    @SerialName("name") val name: String,
    @SerialName("leader_id") val leaderId: String?,
    @SerialName("description") val description: String? = null
)
