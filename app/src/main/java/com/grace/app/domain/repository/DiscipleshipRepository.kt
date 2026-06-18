package com.grace.app.domain.repository

import com.grace.app.domain.model.ActivityCategory
import com.grace.app.domain.model.DiscipleshipActivity
import com.grace.app.domain.model.DurationTag
import com.grace.app.domain.util.Result

interface DiscipleshipRepository {

    suspend fun pickTodaysActivity(): Result<DiscipleshipActivity?>

    suspend fun swapTodaysActivity(activityId: String): Result<Unit>

    suspend fun markCompleted(activityId: String, reflection: String?): Result<Unit>

    suspend fun cellCompletionCountToday(): Result<Int>

    suspend fun myStreak(): Result<Int>

    suspend fun listAllActivities(): Result<List<DiscipleshipActivity>>

    suspend fun isCompletedToday(activityId: String): Result<Boolean>

    suspend fun listTodaysCompletedIds(): Result<Set<String>>

    suspend fun createActivity(
        title: String,
        description: String,
        category: ActivityCategory,
        durationTag: DurationTag
    ): Result<Unit>

    suspend fun updateActivity(
        id: String,
        title: String,
        description: String,
        category: ActivityCategory,
        durationTag: DurationTag,
        isActive: Boolean
    ): Result<Unit>

    suspend fun deleteActivity(id: String): Result<Unit>
}
