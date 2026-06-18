package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleEventDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("chronological_order") val chronologicalOrder: Int,
    @SerialName("approx_year_text") val approxYearText: String? = null,
    @SerialName("source_ref") val sourceRef: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)
