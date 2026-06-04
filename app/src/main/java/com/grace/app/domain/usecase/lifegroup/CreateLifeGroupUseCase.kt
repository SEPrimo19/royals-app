package com.grace.app.domain.usecase.lifegroup

import com.grace.app.domain.model.Group
import com.grace.app.domain.repository.LifeGroupRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class CreateLifeGroupUseCase @Inject constructor(
    private val lifeGroupRepository: LifeGroupRepository
) {
    suspend operator fun invoke(name: String, description: String?): Result<Group> {
        val trimmedName = name.trim()
        if (trimmedName.length < 2) {
            return Result.Error("Life Group name must be at least 2 characters.")
        }
        return lifeGroupRepository.createLifeGroup(trimmedName, description?.trim())
    }
}
