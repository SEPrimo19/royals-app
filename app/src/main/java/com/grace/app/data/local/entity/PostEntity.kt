package com.grace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "user_name") val userName: String?,
    @ColumnInfo(name = "user_avatar_url") val userAvatarUrl: String?,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "verse_ref") val verseRef: String?,
    @ColumnInfo(name = "is_highlighted") val isHighlighted: Boolean = false,
    @ColumnInfo(name = "highlighted_by") val highlightedBy: String?,
    @ColumnInfo(name = "is_flagged") val isFlagged: Boolean = false,
    @ColumnInfo(name = "comment_count") val commentCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: String?
)
