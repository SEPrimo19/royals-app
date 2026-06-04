package com.grace.app.domain.usecase.lifegroup

import com.grace.app.domain.model.User
import com.grace.app.domain.repository.LifeGroupRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class SearchInvitableUsersUseCase @Inject constructor(
    private val lifeGroupRepository: LifeGroupRepository
) {
    suspend operator fun invoke(query: String): Result<List<User>> =
        lifeGroupRepository.searchInvitableUsers(query)
}
