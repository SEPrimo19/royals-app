package com.grace.app.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Insert/upsert payload for prayer_intercessions (composite PK prayer_id,user_id).
@Serializable
data class PrayerIntercessionDto(
    @SerialName("prayer_id") val prayerId: String,
    @SerialName("user_id") val userId: String
)

// Lightweight row used only to count intercessions for a prayer.
@Serializable
data class PrayerIntercessionRow(
    @SerialName("prayer_id") val prayerId: String
)
