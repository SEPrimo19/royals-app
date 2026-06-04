package com.grace.app.data.repository

import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.remote.supabase.dto.GroupDto
import com.grace.app.data.remote.supabase.dto.GroupInsertDto
import com.grace.app.data.remote.supabase.dto.GroupMemberDto
import com.grace.app.data.remote.supabase.dto.UserDto
import com.grace.app.data.remote.supabase.dto.mapper.toDomain
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.model.Group
import com.grace.app.domain.model.LifeGroupDetail
import com.grace.app.domain.repository.LifeGroupRepository
import com.grace.app.domain.util.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LifeGroupRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val networkMonitor: NetworkMonitor,
    private val prefs: UserPreferencesRepo
) : LifeGroupRepository {

    override suspend fun getMyLifeGroup(): Result<LifeGroupDetail?> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to load your Life Group.")
        }
        return try {
            val uid = currentUid()
                ?: return Result.Error("Your session expired. Please sign in again.")

            // Primary lookup: group_members. Falls back to the legacy
            // users.group_id (set by ProfileSetup before Life Groups existed).
            val groupId = supabase.from("group_members")
                .select { filter { eq("user_id", uid) } }
                .decodeList<GroupMemberDto>()
                .firstOrNull()?.groupId
                ?: prefs.groupId.first()

            if (groupId.isNullOrBlank()) return Result.Success(null)

            val groupDto = supabase.from("groups")
                .select { filter { eq("id", groupId) } }
                .decodeSingleOrNull<GroupDto>()
                ?: return Result.Success(null)

            val memberRows = supabase.from("group_members")
                .select { filter { eq("group_id", groupId) } }
                .decodeList<GroupMemberDto>()

            val memberIds = memberRows.map { it.userId }
            val members = if (memberIds.isEmpty()) emptyList()
            else supabase.from("users")
                .select {
                    filter { isIn("id", memberIds) }
                    order("name", Order.ASCENDING)
                }
                .decodeList<UserDto>()
                .map { it.toDomain() }

            val leader = members.firstOrNull { it.id == groupDto.leaderId }
            Result.Success(
                LifeGroupDetail(
                    group = groupDto.toDomain(),
                    leader = leader,
                    members = members
                )
            )
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun createLifeGroup(
        name: String,
        description: String?
    ): Result<Group> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to create your Life Group.")
        }
        return try {
            val uid = currentUid()
                ?: return Result.Error("Your session expired. Please sign in again.")

            // Insert + decodeSingle pattern: server returns the canonical row
            // with its real UUID — avoids the optimistic-temp-id duplication
            // bug we hit in earlier features. Use GroupInsertDto (no id field)
            // so Postgres uses DEFAULT uuid_generate_v4(); sending id = ""
            // causes "invalid input syntax for type uuid" and a misleading
            // "Couldn't reach the server" surfacing in the UI.
            val created = supabase.from("groups")
                .insert(
                    GroupInsertDto(name = name, leaderId = uid, description = description)
                ) { select() }
                .decodeSingle<GroupDto>()

            // Self-insert into group_members so the leader appears in the roster.
            supabase.from("group_members")
                .insert(GroupMemberDto(userId = uid, groupId = created.id))

            // Mirror to users.group_id so existing screens (HomeScreen's
            // "My Leader" card, ProfileSetup display) stay consistent.
            runCatching {
                supabase.from("users").update({ set("group_id", created.id) }) {
                    filter { eq("id", uid) }
                }
                prefs.setRoleAndGroup(
                    role = prefs.userRole.first() ?: "cell_leader",
                    groupId = created.id
                )
            }
            Result.Success(created.toDomain())
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun updateLifeGroup(
        groupId: String,
        name: String,
        description: String?
    ): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to save changes.")
        }
        return try {
            supabase.from("groups").update({
                set("name", name)
                set("description", description)
            }) {
                filter { eq("id", groupId) }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun addMemberById(
        groupId: String,
        userId: String
    ): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to add members.")
        }
        return try {
            // Idempotent: skip if already a member (search excludes them but
            // race conditions or stale-cache calls could still hit this).
            val existing = supabase.from("group_members")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("group_id", groupId)
                    }
                }
                .decodeList<GroupMemberDto>()
            if (existing.isNotEmpty()) {
                return Result.Error("That person is already in this Life Group.")
            }
            supabase.from("group_members")
                .insert(GroupMemberDto(userId = userId, groupId = groupId))
            // Mirror to users.group_id so the Bible Games leaderboard RPC
            // (and any legacy screens that still read users.group_id) see
            // the membership. Without this, members appear in the Life
            // Group roster but get an empty leaderboard. The member's
            // device picks up the new group_id on next syncProfileFromServer
            // (cold start) — no sign-out required.
            runCatching {
                supabase.from("users").update({ set("group_id", groupId) }) {
                    filter { eq("id", userId) }
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun searchInvitableUsers(
        query: String
    ): Result<List<com.grace.app.domain.model.User>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to search members.")
        }
        val q = query.trim()
        if (q.length < 2) return Result.Success(emptyList())
        return try {
            // Pull a small candidate pool of plain members matching name OR
            // email, then exclude anyone already in a Life Group with a
            // second batched lookup. Two queries because Postgrest doesn't
            // expose a NOT IN subquery — but both are tiny + indexed.
            val candidates = supabase.from("users")
                .select {
                    filter {
                        eq("role", "member")
                        or {
                            ilike("name", "%$q%")
                            ilike("email", "%$q%")
                        }
                    }
                    limit(25) // a few extra so the post-filter still hits 10
                }
                .decodeList<UserDto>()
            if (candidates.isEmpty()) return Result.Success(emptyList())

            val candidateIds = candidates.map { it.id }
            val alreadyInAGroup = supabase.from("group_members")
                .select { filter { isIn("user_id", candidateIds) } }
                .decodeList<GroupMemberDto>()
                .map { it.userId }
                .toSet()

            val filtered = candidates
                .filter { it.id !in alreadyInAGroup }
                .map { it.toDomain() }
                .take(10)
            Result.Success(filtered)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun listInvitableUsers(
        limit: Int
    ): Result<List<com.grace.app.domain.model.User>> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to browse members.")
        }
        return try {
            // Same shape as searchInvitableUsers but without the name/email
            // ilike — pull a candidate pool of plain members, exclude
            // anyone already in a group via a batched group_members lookup.
            val candidates = supabase.from("users")
                .select {
                    filter { eq("role", "member") }
                    limit(limit.coerceAtMost(500).toLong())
                    order("name", Order.ASCENDING)
                }
                .decodeList<UserDto>()
            if (candidates.isEmpty()) return Result.Success(emptyList())

            val candidateIds = candidates.map { it.id }
            val alreadyInAGroup = supabase.from("group_members")
                .select { filter { isIn("user_id", candidateIds) } }
                .decodeList<GroupMemberDto>()
                .map { it.userId }
                .toSet()

            val filtered = candidates
                .filter { it.id !in alreadyInAGroup }
                .map { it.toDomain() }
            Result.Success(filtered)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun removeMember(groupId: String, userId: String): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to remove members.")
        }
        return try {
            supabase.from("group_members").delete {
                filter {
                    eq("group_id", groupId)
                    eq("user_id", userId)
                }
            }
            // If a user removed themselves, clear their cached group_id too.
            val uid = currentUid()
            if (uid == userId) {
                runCatching {
                    // Explicit String? — bare `null` triggers overload ambiguity.
                    supabase.from("users").update({ set("group_id", null as String?) }) {
                        filter { eq("id", uid) }
                    }
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    private suspend fun currentUid(): String? =
        supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()

    private fun friendly(e: Exception): String = when {
        e.message?.contains("row-level security", ignoreCase = true) == true ->
            "You don't have permission to do that."
        e.message?.contains("duplicate key", ignoreCase = true) == true ->
            "That person is already in this Life Group."
        else -> "Couldn't reach the server. Try again."
    }
}
