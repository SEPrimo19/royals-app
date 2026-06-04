package com.grace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayers")
data class PrayerEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "user_id") val userId: String?,
    @ColumnInfo(name = "user_name") val userName: String?,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "is_anonymous") val isAnonymous: Boolean = false,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "status") val status: String = "active",
    @ColumnInfo(name = "pray_count") val prayCount: Int = 0,
    @ColumnInfo(name = "is_flagged") val isFlagged: Boolean = false,
    @ColumnInfo(name = "expires_at") val expiresAt: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    // Phase P.3 — leader user_id when posted via proxy, NULL for self-posts.
    @ColumnInfo(name = "posted_by_proxy") val postedByProxy: String? = null,
    // Resolved leader display name. Cached so the "via {leader}" tag survives
    // offline. Repo fills it via a batched users lookup during the network
    // refresh path.
    @ColumnInfo(name = "proxy_leader_name") val proxyLeaderName: String? = null
)
