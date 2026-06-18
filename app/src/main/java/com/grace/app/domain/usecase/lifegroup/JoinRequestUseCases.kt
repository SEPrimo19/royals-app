package com.grace.app.domain.usecase.lifegroup

import com.grace.app.domain.model.BrowsableGroup
import com.grace.app.domain.model.IncomingJoinRequest
import com.grace.app.domain.repository.LifeGroupRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class ListBrowsableGroupsUseCase @Inject constructor(
    private val repo: LifeGroupRepository
) {
    suspend operator fun invoke(): Result<List<BrowsableGroup>> =
        repo.listBrowsableGroups()
}

class RequestToJoinGroupUseCase @Inject constructor(
    private val repo: LifeGroupRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> =
        repo.requestToJoinGroup(groupId)
}

class CancelMyJoinRequestUseCase @Inject constructor(
    private val repo: LifeGroupRepository
) {
    suspend operator fun invoke(requestId: String): Result<Unit> =
        repo.cancelMyJoinRequest(requestId)
}

class ListIncomingJoinRequestsUseCase @Inject constructor(
    private val repo: LifeGroupRepository
) {
    suspend operator fun invoke(): Result<List<IncomingJoinRequest>> =
        repo.listIncomingJoinRequests()
}

class ApproveJoinRequestUseCase @Inject constructor(
    private val repo: LifeGroupRepository
) {
    suspend operator fun invoke(requestId: String): Result<Unit> =
        repo.approveJoinRequest(requestId)
}

class RejectJoinRequestUseCase @Inject constructor(
    private val repo: LifeGroupRepository
) {
    suspend operator fun invoke(requestId: String, note: String? = null): Result<Unit> =
        repo.rejectJoinRequest(requestId, note)
}
