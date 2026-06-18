package com.grace.app.data.repository

import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.local.dao.DiscipleshipDao
import com.grace.app.data.local.entity.DiscipleshipActivityEntity
import com.grace.app.data.local.entity.DiscipleshipTodayPickEntity
import com.grace.app.data.remote.supabase.dto.CompletionInsertDto
import com.grace.app.data.remote.supabase.dto.DiscipleshipActivityDto
import com.grace.app.data.remote.supabase.dto.DiscipleshipActivityInsertDto
import com.grace.app.data.remote.supabase.dto.TodaysActivityRow
import com.grace.app.data.remote.supabase.dto.toDomain
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.model.ActivityCategory as DomainActivityCategory
import com.grace.app.domain.model.DurationTag as DomainDurationTag
import com.grace.app.domain.model.ActivityCategory
import com.grace.app.domain.model.DiscipleshipActivity
import com.grace.app.domain.model.DurationTag
import com.grace.app.domain.repository.DiscipleshipRepository
import com.grace.app.domain.util.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscipleshipRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val networkMonitor: NetworkMonitor,
    private val prefs: UserPreferencesRepo,
    private val dao: DiscipleshipDao
) : DiscipleshipRepository {

    private suspend fun currentUid(): String? =
        supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()

    private fun todayIsoPHT(): String =
        java.time.LocalDate.now(java.time.ZoneId.of("Asia/Manila")).toString()

    private fun DiscipleshipActivity.toEntity() = DiscipleshipActivityEntity(
        id = id,
        title = title,
        description = description,
        category = category.slug,
        durationTag = durationTag.slug,
        isActive = isActive,
        createdBy = createdBy,
        createdAt = createdAt.toString()
    )

    private fun DiscipleshipActivityEntity.toDomainCached(): DiscipleshipActivity =
        DiscipleshipActivity(
            id = id,
            title = title,
            description = description,
            category = DomainActivityCategory.fromSlug(category),
            durationTag = DomainDurationTag.fromSlug(durationTag),
            isActive = isActive,
            createdBy = createdBy,
            createdAt = java.time.LocalDateTime.parse(
                createdAt.substringBefore('+').substringBefore('Z').trim('T')
                    .let { if (it.contains('T')) it else it.replaceFirst(' ', 'T') }
            )
        )

    override suspend fun pickTodaysActivity(): Result<DiscipleshipActivity?> {
        if (!networkMonitor.isOnline) return Result.Error("You're offline.")
        return try {
            val rows = supabase.pluginManager.getPlugin(Postgrest)
                .rpc(function = "pick_todays_activity")
                .decodeList<TodaysActivityRow>()
            Result.Success(rows.firstOrNull()?.toDomain())
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun swapTodaysActivity(activityId: String): Result<Unit> {
        if (!networkMonitor.isOnline) return Result.Error("You're offline.")
        return try {
            supabase.pluginManager.getPlugin(Postgrest)
                .rpc(
                    function = "swap_todays_activity",
                    parameters = buildJsonObject {
                        put("p_activity_id", JsonPrimitive(activityId))
                    }
                )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun markCompleted(
        activityId: String,
        reflection: String?
    ): Result<Unit> {
        if (!networkMonitor.isOnline) return Result.Error("You're offline.")
        return try {
            val uid = currentUid()
                ?: return Result.Error("Your session expired. Please sign in again.")
            supabase.from("user_discipleship_completions").upsert(
                CompletionInsertDto(
                    userId = uid,
                    activityId = activityId,
                    reflection = reflection?.trim()?.takeIf { it.isNotEmpty() }
                ),
                onConflict = "user_id,activity_id,completed_date"
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun cellCompletionCountToday(): Result<Int> {
        if (!networkMonitor.isOnline) return Result.Success(0)
        return try {
            val raw = supabase.pluginManager.getPlugin(Postgrest)
                .rpc(function = "cell_completion_count_today")
                .decodeAs<kotlinx.serialization.json.JsonElement>()
            val n = when (raw) {
                is kotlinx.serialization.json.JsonArray ->
                    raw.firstOrNull()?.jsonPrimitive?.intOrNull ?: 0
                is kotlinx.serialization.json.JsonPrimitive ->
                    raw.intOrNull ?: 0
                else -> 0
            }
            Result.Success(n)
        } catch (_: Exception) {
            Result.Success(0)
        }
    }

    override suspend fun myStreak(): Result<Int> {
        if (!networkMonitor.isOnline) return Result.Success(0)
        return try {
            val raw = supabase.pluginManager.getPlugin(Postgrest)
                .rpc(function = "my_discipleship_streak")
                .decodeAs<kotlinx.serialization.json.JsonElement>()
            val n = when (raw) {
                is kotlinx.serialization.json.JsonArray ->
                    raw.firstOrNull()?.jsonPrimitive?.intOrNull ?: 0
                is kotlinx.serialization.json.JsonPrimitive ->
                    raw.intOrNull ?: 0
                else -> 0
            }
            Result.Success(n)
        } catch (_: Exception) {
            Result.Success(0)
        }
    }

    override suspend fun listAllActivities(): Result<List<DiscipleshipActivity>> {
        if (!networkMonitor.isOnline) return Result.Error("You're offline.")
        return try {
            val rows = supabase.from("discipleship_activities")
                .select {
                    filter { eq("is_active", true) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<DiscipleshipActivityDto>()
            Result.Success(rows.map { it.toDomain() })
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun isCompletedToday(activityId: String): Result<Boolean> {
        if (!networkMonitor.isOnline) return Result.Success(false)
        return try {
            val uid = currentUid() ?: return Result.Success(false)
            val today = java.time.LocalDate
                .now(java.time.ZoneId.of("Asia/Manila"))
                .toString()
            val rows = supabase.from("user_discipleship_completions")
                .select {
                    filter {
                        eq("user_id", uid)
                        eq("activity_id", activityId)
                        eq("completed_date", today)
                    }
                    limit(1)
                }
                .decodeList<CompletionInsertDto>()
            Result.Success(rows.isNotEmpty())
        } catch (_: Exception) {
            Result.Success(false)
        }
    }

    override suspend fun listTodaysCompletedIds(): Result<Set<String>> {
        if (!networkMonitor.isOnline) return Result.Success(emptySet())
        return try {
            val uid = currentUid() ?: return Result.Success(emptySet())
            val today = todayIsoPHT()
            val rows = supabase.from("user_discipleship_completions")
                .select {
                    filter {
                        eq("user_id", uid)
                        eq("completed_date", today)
                    }
                }
                .decodeList<CompletionInsertDto>()
            Result.Success(rows.map { it.activityId }.toSet())
        } catch (_: Exception) {
            Result.Success(emptySet())
        }
    }


    override suspend fun createActivity(
        title: String,
        description: String,
        category: ActivityCategory,
        durationTag: DurationTag
    ): Result<Unit> {
        if (!networkMonitor.isOnline) return Result.Error("You're offline.")
        return try {
            val uid = currentUid()
                ?: return Result.Error("Your session expired. Please sign in again.")
            supabase.from("discipleship_activities").insert(
                DiscipleshipActivityInsertDto(
                    title = title.trim(),
                    description = description.trim(),
                    category = category.slug,
                    durationTag = durationTag.slug,
                    createdBy = uid
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun updateActivity(
        id: String,
        title: String,
        description: String,
        category: ActivityCategory,
        durationTag: DurationTag,
        isActive: Boolean
    ): Result<Unit> {
        if (!networkMonitor.isOnline) return Result.Error("You're offline.")
        return try {
            supabase.from("discipleship_activities").update({
                set("title", title.trim())
                set("description", description.trim())
                set("category", category.slug)
                set("duration_tag", durationTag.slug)
                set("is_active", isActive)
                set("updated_at", java.time.OffsetDateTime.now().toString())
            }) {
                filter { eq("id", id) }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun deleteActivity(id: String): Result<Unit> {
        if (!networkMonitor.isOnline) return Result.Error("You're offline.")
        return try {
            supabase.from("discipleship_activities").delete {
                filter { eq("id", id) }
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
            raw.contains("violates check constraint", true) ->
                "Title 1-80 chars, description 1-600 chars."
            else -> "Couldn't reach the server. Try again."
        }
    }
}
