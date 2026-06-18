package com.grace.app.domain.usecase.discipleship

import com.grace.app.domain.model.ActivityCategory
import com.grace.app.domain.model.DiscipleshipActivity
import com.grace.app.domain.model.DurationTag
import com.grace.app.domain.repository.DiscipleshipRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class GetTodaysActivityUseCase @Inject constructor(
    private val repo: DiscipleshipRepository
) { suspend operator fun invoke(): Result<DiscipleshipActivity?> = repo.pickTodaysActivity() }

class SwapTodaysActivityUseCase @Inject constructor(
    private val repo: DiscipleshipRepository
) { suspend operator fun invoke(id: String): Result<Unit> = repo.swapTodaysActivity(id) }

class MarkActivityCompletedUseCase @Inject constructor(
    private val repo: DiscipleshipRepository
) {
    suspend operator fun invoke(id: String, reflection: String? = null): Result<Unit> =
        repo.markCompleted(id, reflection)
}

class GetCellCompletionCountUseCase @Inject constructor(
    private val repo: DiscipleshipRepository
) { suspend operator fun invoke(): Result<Int> = repo.cellCompletionCountToday() }

class GetMyDiscipleshipStreakUseCase @Inject constructor(
    private val repo: DiscipleshipRepository
) { suspend operator fun invoke(): Result<Int> = repo.myStreak() }

class IsActivityCompletedTodayUseCase @Inject constructor(
    private val repo: DiscipleshipRepository
) { suspend operator fun invoke(id: String): Result<Boolean> = repo.isCompletedToday(id) }

class ListTodaysCompletedIdsUseCase @Inject constructor(
    private val repo: DiscipleshipRepository
) { suspend operator fun invoke(): Result<Set<String>> = repo.listTodaysCompletedIds() }

class ListAllActivitiesUseCase @Inject constructor(
    private val repo: DiscipleshipRepository
) { suspend operator fun invoke(): Result<List<DiscipleshipActivity>> = repo.listAllActivities() }

class CreateActivityUseCase @Inject constructor(
    private val repo: DiscipleshipRepository
) {
    suspend operator fun invoke(
        title: String,
        description: String,
        category: ActivityCategory,
        durationTag: DurationTag
    ): Result<Unit> = repo.createActivity(title, description, category, durationTag)
}

class UpdateActivityUseCase @Inject constructor(
    private val repo: DiscipleshipRepository
) {
    suspend operator fun invoke(
        id: String,
        title: String,
        description: String,
        category: ActivityCategory,
        durationTag: DurationTag,
        isActive: Boolean
    ): Result<Unit> = repo.updateActivity(id, title, description, category, durationTag, isActive)
}

class DeleteActivityUseCase @Inject constructor(
    private val repo: DiscipleshipRepository
) { suspend operator fun invoke(id: String): Result<Unit> = repo.deleteActivity(id) }
