package com.grace.app.domain.usecase.lifegroup

import com.grace.app.domain.model.LifeGroupDetail
import com.grace.app.domain.repository.LifeGroupRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetMyLifeGroupUseCase @Inject constructor(
    private val lifeGroupRepository: LifeGroupRepository
) {
    suspend operator fun invoke(): Result<LifeGroupDetail?> =
        lifeGroupRepository.getMyLifeGroup()
}
