package com.grace.app.data.sync

import kotlinx.serialization.Serializable

object SyncActions {
    const val POST_PRAYER = "POST_PRAYER"
    const val MARK_DEVO_COMPLETE = "MARK_DEVO_COMPLETE"
    const val INTERCEDE = "INTERCEDE"
}

@Serializable
data class PostPrayerPayload(
    val content: String,
    val isAnonymous: Boolean,
    val category: String,
    val userId: String? = null,
    val localId: String? = null
)

@Serializable
data class MarkDevoCompletePayload(
    val userId: String,
    val devoId: String,
    val encryptedJournal: String
)

@Serializable
data class InterceedePayload(val prayerId: String)
