package com.grace.app.domain.usecase.leader

import com.grace.app.domain.model.CheckIn
import com.grace.app.domain.repository.LeaderRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetMemberCheckInUseCase @Inject constructor(
    private val leaderRepository: LeaderRepository
) {
    suspend operator fun invoke(memberId: String): Result<CheckIn?> =
        leaderRepository.getMemberLatestCheckIn(memberId)
}
