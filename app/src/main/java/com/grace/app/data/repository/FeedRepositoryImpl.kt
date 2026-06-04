package com.grace.app.data.repository

import android.content.Context
import android.net.Uri
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.local.dao.PostDao
import com.grace.app.data.local.entity.PostEntity
import com.grace.app.data.remote.supabase.dto.PostDto
import com.grace.app.data.remote.supabase.dto.PostInsertDto
import com.grace.app.data.remote.supabase.dto.ReactionDto
import com.grace.app.data.remote.supabase.dto.mapper.toDbValue
import com.grace.app.data.remote.supabase.dto.mapper.toDomain
import com.grace.app.data.remote.supabase.dto.mapper.toEntity
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.model.Post
import com.grace.app.domain.model.PostType
import com.grace.app.domain.repository.FeedRepository
import com.grace.app.domain.util.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.OffsetDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// Phase A.3 — mirrors `storage.buckets.file_size_limit` for the `posts`
// bucket (5 MB). Kept here so the client-side guard and server-side cap
// stay in sync visually; if you change one, change the other.
private const val MAX_POST_IMAGE_BYTES = 5 * 1024 * 1024  // 5 MB; matches Storage bucket cap

@Singleton
class FeedRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val postDao: PostDao,
    private val supabase: SupabaseClient,
    private val networkMonitor: NetworkMonitor,
    private val prefs: UserPreferencesRepo
) : FeedRepository {

    override fun getPosts(): Flow<Result<List<Post>>> = flow {
        // Cached emit first — fast paint, counts unknown (0) offline.
        val cached = postDao.getAll().first().map { it.toDomain() }
        if (cached.isNotEmpty()) emit(Result.Success(cached))

        if (!networkMonitor.isOnline) {
            if (cached.isEmpty()) emit(Result.Success(emptyList()))
            return@flow
        }

        try {
            val remote = supabase.from("posts")
                .select { order("created_at", Order.DESCENDING) }
                .decodeList<PostDto>()

            // Reconcile cache: drop posts the server no longer has.
            if (remote.isEmpty()) postDao.clearAll()
            else postDao.deleteNotIn(remote.map { it.id })
            postDao.insertAll(remote.map { it.toEntity() })

            // One batched query for reactions across all visible posts, then
            // bucket them by post + figure out which one belongs to me.
            val postIds = remote.map { it.id }
            val reactions = if (postIds.isEmpty()) emptyList()
            else supabase.from("reactions")
                .select { filter { isIn("post_id", postIds) } }
                .decodeList<ReactionDto>()
            val byPost = reactions.groupBy { it.postId }
            val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()

            val enriched = remote.map { dto ->
                val rs = byPost[dto.id].orEmpty()
                val counts = rs.groupingBy { it.reactionType }.eachCount()
                val mine = rs.firstOrNull { it.userId == uid }?.reactionType
                dto.toDomain(reactions = counts, myReaction = mine)
            }
            emit(Result.Success(enriched))
        } catch (_: Exception) {
            emit(Result.Error("Couldn't refresh the feed. Showing saved posts."))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun createPost(
        type: PostType,
        content: String,
        imageUri: String?,
        verseRef: String?
    ): Result<Unit> {
        return try {
        val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()
        ?: return Result.Error("Your session expired. Please sign in again.")

        // Posts (especially with photos) require the network. Fail fast with a
        // friendly message instead of letting a host-resolution exception leak.
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to share posts.")
        }

        // Image upload must succeed BEFORE the post row is created — atomic.
        var imageUrl: String? = null
        if (imageUri != null) {
            val bytes = context.contentResolver
                .openInputStream(Uri.parse(imageUri))?.use { it.readBytes() }
                ?: return Result.Error("Couldn't read the selected image.")
            // A.3 client-side guard. Matches the server-side `posts` bucket
            // file_size_limit so the user gets a clean message instead of
            // a generic 413 from Supabase Storage. Keep both — the server
            // is the source of truth, this is just nicer UX.
            if (bytes.size > MAX_POST_IMAGE_BYTES) {
                return Result.Error(
                    "Image is too large. Please pick one under 5 MB."
                )
            }
            val path = "$uid/${UUID.randomUUID()}.jpg"
            supabase.storage.from("posts").upload(path, bytes)
            imageUrl = supabase.storage.from("posts").publicUrl(path)
        }

        // Insert AND get the server-generated row back, so the local copy uses
        // the real id. The { select() } block tells Postgrest to echo the
        // inserted row in the response body — without it the body is empty
        // and decodeSingle throws "Expected start of the array '['". The
        // insert itself still succeeded server-side, but the user saw a
        // "Couldn't share your post" toast and only spotted the post when
        // they navigated away and back. This was the exact symptom reported.
        val inserted = supabase.from("posts")
            .insert(
                PostInsertDto(uid, type.toDbValue(), content, imageUrl, verseRef)
            ) { select() }
            .decodeSingle<PostDto>()
        postDao.insert(inserted.toEntity())
        Result.Success(Unit)
    } catch (e: Exception) {
        // Map a few raw Supabase errors to messages a youth user can act on.
        val msg = when {
            e.message?.contains("Bucket not found", ignoreCase = true) == true ->
                "Photo sharing isn't set up yet. Please contact your church admin."
            e.message?.contains("row-level security", ignoreCase = true) == true ->
                "Posting isn't enabled yet. Please contact your church admin."
            else -> "Couldn't share your post. Try again."
        }
        Result.Error(msg, e)
    }
    }

    override suspend fun react(postId: String, reactionType: String): Result<Unit> = try {
        val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()
        ?: return Result.Error("Your session expired. Please sign in again.")

        val existing = supabase.from("reactions")
            .select {
                filter {
                    eq("post_id", postId)
                    eq("user_id", uid)
                }
            }
            .decodeList<ReactionDto>()
            .firstOrNull()

        when {
            existing == null ->
                supabase.from("reactions")
                    .insert(ReactionDto(postId, uid, reactionType))

            existing.reactionType == reactionType ->
                // Tapping the active reaction toggles it off.
                supabase.from("reactions").delete {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", uid)
                    }
                }

            else ->
                supabase.from("reactions")
                    .update({ set("reaction_type", reactionType) }) {
                        filter {
                            eq("post_id", postId)
                            eq("user_id", uid)
                        }
                    }
        }
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error("Couldn't react to the post.", e)
    }

    override suspend fun getMyPosts(): Result<List<Post>> {
        if (!networkMonitor.isOnline) {
            val uid = prefs.userId.first()
            val cached = postDao.getAll().first()
                .filter { uid != null && it.userId == uid }
                .map { it.toDomain() }
            return Result.Success(cached)
        }
        return try {
            val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()
            ?: return Result.Error("Your session expired. Please sign in again.")
            val dtos = supabase.from("posts")
                .select {
                    filter { eq("user_id", uid) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<PostDto>()
            Result.Success(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error("Couldn't load your posts. Try again.", e)
        }
    }

    override suspend fun updatePostContent(
        postId: String,
        content: String
    ): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to edit.")
        }
        return try {
            supabase.from("posts").update({ set("content", content) }) {
                filter { eq("id", postId) }
            }
            postDao.updateContent(postId, content)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Couldn't save your changes. Try again.", e)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to delete.")
        }
        return try {
            supabase.from("posts").delete { filter { eq("id", postId) } }
            postDao.deleteById(postId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error("Couldn't delete the post. Try again.", e)
        }
    }
}
