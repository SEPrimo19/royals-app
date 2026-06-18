package com.grace.app.data.repository

import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.remote.supabase.dto.BibleHighlightDto
import com.grace.app.data.remote.supabase.dto.BibleNoteDto
import com.grace.app.data.remote.supabase.dto.BibleNoteUpsertDto
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.model.BibleNote
import com.grace.app.domain.repository.BibleStudyRepository
import com.grace.app.domain.util.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.first
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BibleStudyRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val prefs: UserPreferencesRepo,
    private val networkMonitor: NetworkMonitor
) : BibleStudyRepository {

    private suspend fun uid(): String? =
        supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()

    override suspend fun getChapterNote(bookOrder: Int, chapter: Int): Result<BibleNote?> {
        if (!networkMonitor.isOnline) return Result.Success(null)
        return try {
            val uid = uid() ?: return Result.Success(null)
            val row = supabase.from("bible_notes").select {
                filter {
                    eq("user_id", uid)
                    eq("book_order", bookOrder)
                    eq("chapter", chapter)
                }
            }.decodeList<BibleNoteDto>().firstOrNull()
            Result.Success(row?.toDomain())
        } catch (e: Exception) {
            Result.Error("Couldn't load your note.", e)
        }
    }

    override suspend fun saveChapterNote(
        bookOrder: Int,
        chapter: Int,
        content: String
    ): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to save your note.")
        }
        return try {
            val uid = uid() ?: return Result.Error("Your session expired. Please sign in again.")
            val nowIso = OffsetDateTime.now().toString()
            val existing = supabase.from("bible_notes").select {
                filter {
                    eq("user_id", uid)
                    eq("book_order", bookOrder)
                    eq("chapter", chapter)
                }
            }.decodeList<BibleNoteDto>().firstOrNull()
            if (existing != null) {
                supabase.from("bible_notes").update({
                    set("content", content)
                    set("updated_at", nowIso)
                }) {
                    filter { eq("id", existing.id) }
                }
            } else {
                supabase.from("bible_notes").insert(
                    BibleNoteUpsertDto(
                        userId = uid,
                        bookOrder = bookOrder,
                        chapter = chapter,
                        content = content,
                        updatedAt = nowIso
                    )
                )
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Couldn't save your note.", e)
        }
    }

    override suspend fun getChapterHighlights(
        bookOrder: Int,
        chapter: Int
    ): Result<Set<Int>> {
        if (!networkMonitor.isOnline) return Result.Success(emptySet())
        return try {
            val uid = uid() ?: return Result.Success(emptySet())
            val rows = supabase.from("bible_highlights").select {
                filter {
                    eq("user_id", uid)
                    eq("book_order", bookOrder)
                    eq("chapter", chapter)
                }
            }.decodeList<BibleHighlightDto>()
            Result.Success(rows.map { it.verse }.toSet())
        } catch (e: Exception) {
            Result.Error("Couldn't load highlights.", e)
        }
    }

    override suspend fun setHighlight(
        bookOrder: Int,
        chapter: Int,
        verse: Int,
        on: Boolean
    ): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to highlight.")
        }
        return try {
            val uid = uid() ?: return Result.Error("Your session expired. Please sign in again.")
            if (on) {
                supabase.from("bible_highlights").upsert(
                    value = BibleHighlightDto(uid, bookOrder, chapter, verse),
                    onConflict = "user_id,book_order,chapter,verse"
                )
            } else {
                supabase.from("bible_highlights").delete {
                    filter {
                        eq("user_id", uid)
                        eq("book_order", bookOrder)
                        eq("chapter", chapter)
                        eq("verse", verse)
                    }
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Couldn't update the highlight.", e)
        }
    }


    override suspend fun listNotes(): Result<List<BibleNote>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to see your notes.")
        }
        return try {
            val uid = uid() ?: return Result.Error("Your session expired. Please sign in again.")
            val rows = supabase.from("bible_notes").select {
                filter { eq("user_id", uid) }
                order("updated_at", Order.DESCENDING)
            }.decodeList<BibleNoteDto>()
            Result.Success(rows.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error("Couldn't load your notes.", e)
        }
    }

    override suspend fun getNote(id: String): Result<BibleNote?> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to open this note.")
        }
        return try {
            val row = supabase.from("bible_notes").select {
                filter { eq("id", id) }
            }.decodeList<BibleNoteDto>().firstOrNull()
            Result.Success(row?.toDomain())
        } catch (e: Exception) {
            Result.Error("Couldn't open that note.", e)
        }
    }

    override suspend fun createSessionNote(title: String): Result<BibleNote> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to create a note.")
        }
        return try {
            val uid = uid() ?: return Result.Error("Your session expired. Please sign in again.")
            val nowIso = OffsetDateTime.now().toString()
            val created = supabase.from("bible_notes").insert(
                BibleNoteUpsertDto(
                    userId = uid,
                    title = title,
                    bookOrder = null,
                    chapter = null,
                    content = "",
                    updatedAt = nowIso
                )
            ) { select() }.decodeSingle<BibleNoteDto>()
            Result.Success(created.toDomain())
        } catch (e: Exception) {
            Result.Error("Couldn't create the note.", e)
        }
    }

    override suspend fun saveNote(id: String, title: String?, content: String): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to save your note.")
        }
        return try {
            val nowIso = OffsetDateTime.now().toString()
            supabase.from("bible_notes").update({
                if (title != null) set("title", title)
                set("content", content)
                set("updated_at", nowIso)
            }) {
                filter { eq("id", id) }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Couldn't save your note.", e)
        }
    }

    override suspend fun deleteNote(id: String): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to delete this note.")
        }
        return try {
            supabase.from("bible_notes").delete {
                filter { eq("id", id) }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Couldn't delete the note.", e)
        }
    }
}
