package com.grace.app.domain.usecase.lifegroup

import com.grace.app.domain.repository.LifeGroupRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class AddMemberUseCase @Inject constructor(
    private val lifeGroupRepository: LifeGroupRepository
) {
    suspend operator fun invoke(groupId: String, userId: String): Result<Unit> =
        lifeGroupRepository.addMemberById(groupId, userId)
}
