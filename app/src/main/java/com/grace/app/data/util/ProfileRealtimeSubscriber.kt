package com.grace.app.data.util

import com.grace.app.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRealtimeSubscriber @Inject constructor(
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var collectionJob: Job? = null
    private var channel: RealtimeChannel? = null
    private var subscribedUserId: String? = null

    fun start(userId: String) {
        if (userId.isBlank()) return
        if (subscribedUserId == userId && collectionJob?.isActive == true) {
            return
        }
        stop()
        subscribedUserId = userId
        collectionJob = scope.launch {
            runCatching {
                val ch = supabase.channel("profile_realtime_$userId")
                channel = ch
                @Suppress("DEPRECATION")
                val flow = ch.postgresChangeFlow<PostgresAction.Update>(
                    schema = "public"
                ) {
                    table = "users"
                    filter = "id=eq.$userId"
                }
                ch.subscribe()
                flow.collect {
                    runCatching { authRepository.syncProfileFromServer() }
                }
            }.onFailure {
                CrashReporter.recordNonFatal(it)
            }
        }
    }

    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
        val ch = channel
        channel = null
        subscribedUserId = null
        if (ch != null) {
            scope.launch(NonCancellable) {
                runCatching { supabase.realtime.removeChannel(ch) }
            }
        }
    }
}
