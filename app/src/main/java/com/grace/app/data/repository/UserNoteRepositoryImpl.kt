package com.grace.app.data.repository

import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.remote.supabase.dto.UserNoteHeartInsertDto
import com.grace.app.data.remote.supabase.dto.UserNoteRow
import com.grace.app.data.remote.supabase.dto.UserNoteUpsertDto
import com.grace.app.data.remote.supabase.dto.toDomain
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.model.UserNote
import com.grace.app.domain.repository.UserNoteRepository
import com.grace.app.domain.util.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserNoteRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val networkMonitor: NetworkMonitor,
    private val prefs: UserPreferencesRepo
) : UserNoteRepository {

    private suspend fun currentUid(): String? =
        supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()

    override suspend fun listVisibleNotes(): Result<List<UserNote>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to see notes.")
        }
        return try {
            val rows = supabase
                .pluginManager
                .getPlugin(Postgrest)
                .rpc(function = "list_visible_notes")
                .decodeList<UserNoteRow>()
            Result.Success(rows.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun postMyNote(content: String): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to post.")
        }
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return Result.Error("Your note can't be empty.")
        }
        if (trimmed.length > 200) {
            return Result.Error("Notes are limited to 200 characters.")
        }
        return try {
            val uid = currentUid()
                ?: return Result.Error("Your session expired. Please sign in again.")

            val now = Instant.now()
            val nowIso = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .format(now.atOffset(ZoneOffset.UTC))
            val expiresIso = DateTimeFormatter.ISO_OFFSET_DATE_TIME
                .format(now.plusSeconds(24 * 3600).atOffset(ZoneOffset.UTC))

            supabase.from("user_notes").upsert(
                UserNoteUpsertDto(
                    userId = uid,
                    content = trimmed,
                    createdAt = nowIso,
                    expiresAt = expiresIso
                ),
                onConflict = "user_id"
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun deleteMyNote(): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to delete.")
        }
        return try {
            val uid = currentUid()
                ?: return Result.Error("Your session expired. Please sign in again.")
            supabase.from("user_notes").delete {
                filter { eq("user_id", uid) }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun toggleHeart(noteUserId: String): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline.")
        }
        return try {
            val uid = currentUid()
                ?: return Result.Error("Your session expired. Please sign in again.")
            val existing = supabase.from("user_note_hearts")
                .select {
                    filter {
                        eq("note_user_id", noteUserId)
                        eq("hearter_id", uid)
                    }
                    limit(1)
                }
                .decodeList<UserNoteHeartInsertDto>()
            if (existing.isNotEmpty()) {
                supabase.from("user_note_hearts").delete {
                    filter {
                        eq("note_user_id", noteUserId)
                        eq("hearter_id", uid)
                    }
                }
            } else {
                supabase.from("user_note_hearts").insert(
                    UserNoteHeartInsertDto(noteUserId = noteUserId, hearterId = uid)
                )
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun hideNote(noteUserId: String): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline.")
        }
        return try {
            val uid = currentUid()
                ?: return Result.Error("Your session expired. Please sign in again.")
            supabase.from("user_notes").update({
                set("is_hidden", true)
                set("hidden_by", uid)
                set("hidden_at", DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    .format(Instant.now().atOffset(ZoneOffset.UTC)))
            }) {
                filter { eq("user_id", noteUserId) }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun unhideNote(noteUserId: String): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline.")
        }
        return try {
            supabase.from("user_notes").update({
                set("is_hidden", false)
            }) {
                filter { eq("user_id", noteUserId) }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    private fun friendly(e: Exception): String {
        val raw = e.message.orEmpty()
        return when {
            raw.contains("row-level security", true) ->
                "You don't have permission to do that."
            raw.contains("violates check constraint", true) &&
                raw.contains("content", true) ->
                "Notes are 1-200 characters."
            else -> "Couldn't reach the server. Try again."
        }
    }

    private val channelCleanupScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun subscribeToNoteChanges(): Flow<Unit> = callbackFlow {
        val channel = supabase.channel("user_notes_global")
        val changes = channel.postgresChangeFlow<PostgresAction>(
            schema = "public"
        ) {
            table = "user_notes"
        }
        val job = launch {
            changes.collect { trySend(Unit) }
        }
        channel.subscribe()

        awaitClose {
            job.cancel()
            channelCleanupScope.launch(NonCancellable) {
                runCatching { supabase.realtime.removeChannel(channel) }
            }
        }
    }
}
