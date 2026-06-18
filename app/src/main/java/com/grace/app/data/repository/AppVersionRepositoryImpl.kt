package com.grace.app.data.repository

import com.grace.app.data.remote.supabase.dto.AppVersionDto
import com.grace.app.data.remote.supabase.dto.toDomain
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.model.AppVersion
import com.grace.app.domain.repository.AppVersionRepository
import com.grace.app.domain.util.Result
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppVersionRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val networkMonitor: NetworkMonitor
) : AppVersionRepository {

    override suspend fun getLatestVersion(): Result<AppVersion?> {
        if (!networkMonitor.isOnline) return Result.Success(null)
        return try {
            val row = supabase.from("app_versions")
                .select {
                    order("version_code", Order.DESCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<AppVersionDto>()
            Result.Success(row?.toDomain())
        } catch (e: Exception) {
            Result.Success(null)
        }
    }
}
