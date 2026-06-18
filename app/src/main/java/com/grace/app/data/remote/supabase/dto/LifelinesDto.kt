package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LifelinesDto(
    @SerialName("joshua") val joshua: Int,
    @SerialName("daniel") val daniel: Int
)
