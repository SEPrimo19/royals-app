package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PrayerIntercessionDto(
    @SerialName("prayer_id") val prayerId: String,
    @SerialName("user_id") val userId: String
)

@Serializable
data class PrayerIntercessionRow(
    @SerialName("prayer_id") val prayerId: String
)
