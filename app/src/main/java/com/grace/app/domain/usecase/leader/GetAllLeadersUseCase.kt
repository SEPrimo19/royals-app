package com.grace.app.domain.usecase.leader

import com.grace.app.domain.model.User
import com.grace.app.domain.repository.LeaderRepository
import com.grace.app.domain.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllLeadersUseCase @Inject constructor(
    private val leaderRepository: LeaderRepository
) {
    operator fun invoke(): Flow<Result<List<User>>> = leaderRepository.getAllLeaders()
}
