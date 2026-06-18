package com.grace.app.worker

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.grace.app.domain.repository.DevotionalRepository
import com.grace.app.domain.util.Result as DomainResult
import com.grace.app.widget.VerseOfDayWidget
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DevoSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val devotionalRepository: DevotionalRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        when (devotionalRepository.syncUpcomingDevotionals()) {
            is DomainResult.Success -> {
                runCatching { VerseOfDayWidget().updateAll(appContext) }
                Result.success()
            }
            is DomainResult.Error -> Result.retry()
            DomainResult.Loading -> Result.retry()
        }

    companion object {
        const val UNIQUE_NAME = "grace_devo_sync"
    }
}
