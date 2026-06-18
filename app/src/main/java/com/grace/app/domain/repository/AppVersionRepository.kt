package com.grace.app.domain.repository

import com.grace.app.domain.model.AppVersion
import com.grace.app.domain.util.Result

interface AppVersionRepository {
    suspend fun getLatestVersion(): Result<AppVersion?>
}
