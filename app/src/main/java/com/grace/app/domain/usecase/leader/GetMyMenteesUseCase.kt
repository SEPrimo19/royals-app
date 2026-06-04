package com.grace.app.domain.usecase.leader

import com.grace.app.domain.model.Mentee
import com.grace.app.domain.repository.LeaderRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetMyMenteesUseCase @Inject constructor(
    private val leaderRepository: LeaderRepository
) {
    suspend operator fun invoke(): Result<List<Mentee>> =
        leaderRepository.getMyMentees()
}
