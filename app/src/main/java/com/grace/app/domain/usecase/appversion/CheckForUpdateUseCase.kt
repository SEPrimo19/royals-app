package com.grace.app.domain.usecase.appversion

import com.grace.app.domain.model.AppVersion
import com.grace.app.domain.repository.AppVersionRepository
import com.grace.app.domain.util.Result
import javax.inject.Inject

class CheckForUpdateUseCase @Inject constructor(
    private val repo: AppVersionRepository
) {
    suspend operator fun invoke(currentVersionCode: Int): AppVersion? {
        val r = repo.getLatestVersion()
        val latest = (r as? Result.Success)?.data ?: return null
        return if (latest.versionCode > currentVersionCode) latest else null
    }
}
