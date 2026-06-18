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
import com.grace.app.data.remote.supabase.dto.BrowsableGroupRow
import com.grace.app.data.remote.supabase.dto.GroupJoinRequestInsertDto
import com.grace.app.data.remote.supabase.dto.IncomingJoinRequestRow
import com.grace.app.data.remote.supabase.dto.toDomain
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
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

            val created = supabase.from("groups")
                .insert(
                    GroupInsertDto(name = name, leaderId = uid, description = description)
                ) { select() }
                .decodeSingle<GroupDto>()

            supabase.from("group_members")
                .insert(GroupMemberDto(userId = uid, groupId = created.id))

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
            val candidates = supabase.from("users")
                .select {
                    filter {
                        isIn("role", listOf("member", "council"))
                        or {
                            ilike("name", "%$q%")
                            ilike("email", "%$q%")
                        }
                    }
                    limit(25)
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
                .filter { it.id !in alreadyInAGroup || it.role == "council" }
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
                .filter { it.id !in alreadyInAGroup || it.role == "council" }
                .map { it.toDomain() }
            Result.Success(filtered)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }

    override suspend fun deleteLifeGroup(groupId: String): Result<Unit> {
        if (!networkMonitor.isOnline) {
            return Result.Error("You're offline. Connect to delete this Life Group.")
        }
        return try {
            supabase.from("groups").delete { filter { eq("id", groupId) } }
            prefs.setRoleAndGroup(
                role = prefs.userRole.first() ?: "member",
                groupId = null
            )
            Result.Success(Unit)
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
            val uid = currentUid()
            if (uid == userId) {
                runCatching {
                    supabase.from("users").update({ set("group_id", null as String?) }) {
                        filter { eq("id", uid) }
                    }
                }
                prefs.setRoleAndGroup(
                    role = prefs.userRole.first() ?: "member",
                    groupId = null
                )
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(friendly(e), e)
        }
    }


    override suspend fun listBrowsableGroups(): com.grace.app.domain.util.Result<
        List<com.grace.app.domain.model.BrowsableGroup>
    > {
        if (!networkMonitor.isOnline) {
            return com.grace.app.domain.util.Result.Error(
                "You're offline. Connect to browse cells."
            )
        }
        return try {
            val rows = supabase
                .pluginManager
                .getPlugin(Postgrest)
                .rpc(function = "list_browsable_groups")
                .decodeList<BrowsableGroupRow>()
            com.grace.app.domain.util.Result.Success(rows.map { it.toDomain() })
        } catch (e: Exception) {
            com.grace.app.domain.util.Result.Error(friendlyJoin(e), e)
        }
    }

    override suspend fun requestToJoinGroup(
        groupId: String
    ): com.grace.app.domain.util.Result<Unit> {
        if (!networkMonitor.isOnline) {
            return com.grace.app.domain.util.Result.Error(
                "You're offline. Connect to send the request."
            )
        }
        return try {
            val uid = currentUid()
                ?: return com.grace.app.domain.util.Result.Error(
                    "Your session expired. Please sign in again."
                )
            supabase.from("group_join_requests").insert(
                GroupJoinRequestInsertDto(groupId = groupId, userId = uid)
            )
            com.grace.app.domain.util.Result.Success(Unit)
        } catch (e: Exception) {
            com.grace.app.domain.util.Result.Error(friendlyJoin(e), e)
        }
    }

    override suspend fun cancelMyJoinRequest(
        requestId: String
    ): com.grace.app.domain.util.Result<Unit> {
        if (!networkMonitor.isOnline) {
            return com.grace.app.domain.util.Result.Error(
                "You're offline. Connect to cancel."
            )
        }
        return try {
            supabase.from("group_join_requests").update(
                {
                    set("status", "cancelled")
                }
            ) {
                filter { eq("id", requestId) }
            }
            com.grace.app.domain.util.Result.Success(Unit)
        } catch (e: Exception) {
            com.grace.app.domain.util.Result.Error(friendlyJoin(e), e)
        }
    }

    override suspend fun listIncomingJoinRequests(): com.grace.app.domain.util.Result<
        List<com.grace.app.domain.model.IncomingJoinRequest>
    > {
        if (!networkMonitor.isOnline) {
            return com.grace.app.domain.util.Result.Error(
                "You're offline. Connect to load join requests."
            )
        }
        return try {
            val rows = supabase
                .pluginManager
                .getPlugin(Postgrest)
                .rpc(function = "list_incoming_join_requests")
                .decodeList<IncomingJoinRequestRow>()
            com.grace.app.domain.util.Result.Success(rows.map { it.toDomain() })
        } catch (e: Exception) {
            com.grace.app.domain.util.Result.Error(friendlyJoin(e), e)
        }
    }

    override suspend fun approveJoinRequest(
        requestId: String
    ): com.grace.app.domain.util.Result<Unit> = decideOnRequest(requestId, "approved", null)

    override suspend fun rejectJoinRequest(
        requestId: String,
        note: String?
    ): com.grace.app.domain.util.Result<Unit> = decideOnRequest(requestId, "rejected", note)

    private suspend fun decideOnRequest(
        requestId: String,
        newStatus: String,
        note: String?
    ): com.grace.app.domain.util.Result<Unit> {
        if (!networkMonitor.isOnline) {
            return com.grace.app.domain.util.Result.Error(
                "You're offline. Connect to decide on requests."
            )
        }
        return try {
            val uid = currentUid()
                ?: return com.grace.app.domain.util.Result.Error(
                    "Your session expired. Please sign in again."
                )
            supabase.from("group_join_requests").update(
                {
                    set("status", newStatus)
                    set("decided_by", uid)
                    if (!note.isNullOrBlank()) {
                        set("decided_note", note.trim().take(280))
                    }
                }
            ) {
                filter { eq("id", requestId) }
            }
            com.grace.app.domain.util.Result.Success(Unit)
        } catch (e: Exception) {
            com.grace.app.domain.util.Result.Error(friendlyJoin(e), e)
        }
    }

    private fun friendlyJoin(e: Exception): String {
        val raw = e.message.orEmpty()
        return when {
            raw.contains("3 pending requests", true) ->
                "You already have 3 pending join requests. Cancel one first."
            raw.contains("once per 24 hours", true) ->
                "You already requested this cell in the last 24 hours."
            raw.contains("recently declined", true) ->
                "This cell recently declined your request. Try again after 7 days."
            raw.contains("row-level security", true) ->
                "You don't have permission to do that."
            raw.contains("duplicate key", true) || raw.contains("23505") ->
                "You already have a pending request for this cell."
            else -> "Couldn't complete the request. Try again."
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
