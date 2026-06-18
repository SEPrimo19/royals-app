package com.grace.app.data.remote.supabase.dto

import com.grace.app.domain.model.BibleNote
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BibleNoteDto(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("title") val title: String? = null,
    @SerialName("book_order") val bookOrder: Int? = null,
    @SerialName("chapter") val chapter: Int? = null,
    @SerialName("content") val content: String = ""
) {
    fun toDomain() = BibleNote(
        id = id,
        title = title,
        bookOrder = bookOrder,
        chapter = chapter,
        content = content
    )
}

@Serializable
data class BibleNoteUpsertDto(
    @SerialName("user_id") val userId: String,
    @SerialName("title") val title: String? = null,
    @SerialName("book_order") val bookOrder: Int? = null,
    @SerialName("chapter") val chapter: Int? = null,
    @SerialName("content") val content: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class BibleHighlightDto(
    @SerialName("user_id") val userId: String,
    @SerialName("book_order") val bookOrder: Int,
    @SerialName("chapter") val chapter: Int,
    @SerialName("verse") val verse: Int
)
