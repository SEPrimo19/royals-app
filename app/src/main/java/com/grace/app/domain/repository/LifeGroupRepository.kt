package com.grace.app.domain.repository

import com.grace.app.domain.model.Group
import com.grace.app.domain.model.LifeGroupDetail
import com.grace.app.domain.model.User
import com.grace.app.domain.util.Result

interface LifeGroupRepository {

    /**
     * Returns the current user's Life Group (members + leader), or
     * [Result.Success] with null if they aren't in one yet.
     */
    suspend fun getMyLifeGroup(): Result<LifeGroupDetail?>

    /**
     * Creates a new Life Group with the current user as leader and inserts
     * the creator into `group_members`. RLS rejects this unless the caller
     * is `cell_leader` or above.
     */
    suspend fun createLifeGroup(name: String, description: String?): Result<Group>

    /**
     * Renames / updates description. Leader (or senior leaders) only.
     */
    suspend fun updateLifeGroup(
        groupId: String,
        name: String,
        description: String?
    ): Result<Unit>

    /**
     * Adds a user (by id) to the group. Leader (or senior leaders) only —
     * enforced by RLS. The id is supplied from a server-side search rather
     * than a raw email so the leader never sees nor types other users'
     * emails.
     */
    suspend fun addMemberById(groupId: String, userId: String): Result<Unit>

    /**
     * Type-to-search invitable users: matches name OR email substring,
     * restricted to plain `member` role AND not already in any Life Group.
     * Returns up to 10 results — the picker is for finding-not-browsing.
     */
    suspend fun searchInvitableUsers(query: String): Result<List<User>>

    /**
     * Lists ALL "member"-role users who are NOT in any Life Group. Used by
     * the Browse modal alongside the typed search — for leaders who don't
     * know exact names yet. Capped at [limit] so the modal stays scrollable
     * (no pagination); the client applies a name/email filter on top.
     */
    suspend fun listInvitableUsers(limit: Int = 100): Result<List<User>>

    /**
     * Removes a user from `group_members`. Anyone can remove themselves;
     * leader (or senior leaders) can remove anyone.
     */
    suspend fun removeMember(groupId: String, userId: String): Result<Unit>
}
