package com.grace.app.domain.usecase.lifegroup

import com.grace.app.domain.repository.LifeGroupRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class DeleteLifeGroupUseCase @Inject constructor(
    private val repo: LifeGroupRepository
) {
    suspend operator fun invoke(groupId: String): Result<Unit> =
        repo.deleteLifeGroup(groupId)
}
