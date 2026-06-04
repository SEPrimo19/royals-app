package com.grace.app.data.sync

import kotlinx.serialization.Serializable

// Stable action identifiers + payload schemas shared by the repositories that
// enqueue offline mutations and the OfflineSyncWorker that drains them.
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
    // userId of the poster at the time of queueing. The worker refuses to
    // drain to a different signed-in user (covers the sign-out / different
    // user signs in case). Nullable + defaulted so older queued entries
    // written before this field existed still decode.
    val userId: String? = null,
    // localId of the optimistic PrayerEntity sitting in Room. getPrayers()
    // uses this to PROTECT the row from the reconcile-deleteNotIn() pass
    // until the worker successfully drains; without it the user's offline
    // draft disappears the moment Prayer Wall is opened online.
    val localId: String? = null
)

@Serializable
data class MarkDevoCompletePayload(
    val userId: String,
    val devoId: String,
    // AES-256-GCM ciphertext (base64) — already encrypted before queueing.
    val encryptedJournal: String
)

@Serializable
data class InterceedePayload(val prayerId: String)
