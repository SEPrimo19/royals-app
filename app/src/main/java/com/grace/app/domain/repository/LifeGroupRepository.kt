package com.grace.app.domain.repository

import com.grace.app.domain.model.BrowsableGroup
import com.grace.app.domain.model.Group
import com.grace.app.domain.model.IncomingJoinRequest
import com.grace.app.domain.model.LifeGroupDetail
import com.grace.app.domain.model.User
import com.grace.app.domain.util.Result

interface LifeGroupRepository {

    suspend fun getMyLifeGroup(): Result<LifeGroupDetail?>

    suspend fun createLifeGroup(name: String, description: String?): Result<Group>

    suspend fun updateLifeGroup(
        groupId: String,
        name: String,
        description: String?
    ): Result<Unit>

    suspend fun deleteLifeGroup(groupId: String): Result<Unit>

    suspend fun addMemberById(groupId: String, userId: String): Result<Unit>

    suspend fun searchInvitableUsers(query: String): Result<List<User>>

    suspend fun listInvitableUsers(limit: Int = 100): Result<List<User>>

    suspend fun removeMember(groupId: String, userId: String): Result<Unit>


    suspend fun listBrowsableGroups(): Result<List<BrowsableGroup>>

    suspend fun requestToJoinGroup(groupId: String): Result<Unit>

    suspend fun cancelMyJoinRequest(requestId: String): Result<Unit>

    suspend fun listIncomingJoinRequests(): Result<List<IncomingJoinRequest>>

    suspend fun approveJoinRequest(requestId: String): Result<Unit>

    suspend fun rejectJoinRequest(requestId: String, note: String?): Result<Unit>
}
