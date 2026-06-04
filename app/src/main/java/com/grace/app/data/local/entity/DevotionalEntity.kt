package com.grace.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devotionals")
data class DevotionalEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "scheduled_date") val scheduledDate: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "verse_ref") val verseRef: String,
    @ColumnInfo(name = "verse_text") val verseText: String,
    @ColumnInfo(name = "reflection") val reflection: String,
    @ColumnInfo(name = "prayer_starter") val prayerStarter: String,
    @ColumnInfo(name = "journal_prompt") val journalPrompt: String,
    @ColumnInfo(name = "plan_id") val planId: String?,
    @ColumnInfo(name = "created_by") val createdBy: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?
)
