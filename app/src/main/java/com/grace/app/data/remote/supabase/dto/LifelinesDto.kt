package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Row shape returned by `get_lifelines()` and `use_lifeline(kind)` RPCs. */
@Serializable
data class LifelinesDto(
    @SerialName("joshua") val joshua: Int,
    @SerialName("daniel") val daniel: Int
)
