package com.grace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "avatar_url") val avatarUrl: String?,
    @ColumnInfo(name = "role") val role: String,
    @ColumnInfo(name = "group_id") val groupId: String?,
    @ColumnInfo(name = "fcm_token") val fcmToken: String?,
    @ColumnInfo(name = "streak") val streak: Int = 0,
    @ColumnInfo(name = "last_devo_at") val lastDevoAt: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    // Compassion fields cached locally so admin/leader screens can render
    // without round-tripping to Supabase.
    @ColumnInfo(name = "is_compassion", defaultValue = "0")
    val isCompassion: Boolean = false,
    @ColumnInfo(name = "compassion_number") val compassionNumber: String? = null,
    @ColumnInfo(name = "emergency_contact") val emergencyContact: String? = null,
    // Leader Proxy Mode — cached so My Members can show the "no app" badge
    // offline and avoid a refetch on every screen entry.
    @ColumnInfo(name = "is_proxy_only", defaultValue = "0")
    val isProxyOnly: Boolean = false,
    @ColumnInfo(name = "birthdate") val birthdate: String? = null,
    @ColumnInfo(name = "sex") val sex: String? = null
)
