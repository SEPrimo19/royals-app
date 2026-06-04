package com.grace.app.domain.model

import java.time.LocalDate

/**
 * One past devotional + the user's own decrypted journal reflection.
 * Built by joining `user_devo_progress` with the `devotionals` it points
 * to. The plaintext lives in memory only — disk persists the ciphertext.
 *
 * `entry` is empty when:
 *   - the user submitted no text (rare; UI disables the button), OR
 *   - the ciphertext came from a different install / Keystore key and
 *     can't be decrypted on this device.
 * In the second case the UI shows a "can't read on this device" note
 * so the user understands why the row is blank.
 */
data class JournalEntry(
    val devoId: String,
    val completedAt: LocalDate,
    val devoTitle: String,
    val verseRef: String,
    val verseText: String,
    val journalPrompt: String,
    val entry: String,
    val isReadable: Boolean
)
