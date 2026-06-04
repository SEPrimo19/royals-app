package com.grace.app.data.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.grace.app.worker.OfflineSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for connectivity.
 *
 * IMPORTANT: We deliberately do NOT require NET_CAPABILITY_VALIDATED for
 * [isOnline]. Android's validation pings clients3.google.com/generate_204
 * to set that capability; on networks where the captive-portal endpoint is
 * slow, blocked (some Filipino ISPs do this), or simply hasn't been hit
 * yet, VALIDATED is false even though the network actually works — the
 * user sees the "!" indicator next to the WiFi icon. Requiring VALIDATED
 * caused every prayer post on those networks to be quarantined to the
 * offline queue with nothing to drain it.
 *
 * The repositories already gracefully fall back to the offline queue when
 * a Supabase write throws, so being optimistic about `isOnline` is safe:
 * worst case a write attempt fails and falls through to the queue, which
 * the worker drains later. Best case (validation was wrong) the write
 * succeeds and the user sees their prayer sync immediately.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _networkState = MutableStateFlow(currentlyOnline())
    val networkState: StateFlow<Boolean> = _networkState.asStateFlow()

    val isOnline: Boolean get() = currentlyOnline()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _networkState.value = true
            // Drain immediately when ANY network attaches. The worker is
            // idempotent and resilient to per-item failures, so attempting
            // on a not-yet-validated network costs at most one retry burn.
            enqueueOfflineDrain()
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            // Second-chance trigger for networks where VALIDATED arrives
            // late (slow captive portal, delayed DNS, etc). Same UNIQUE_NAME
            // means we don't pile up duplicate workers.
            if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            ) {
                _networkState.value = true
                enqueueOfflineDrain()
            }
        }

        override fun onLost(network: Network) {
            _networkState.value = currentlyOnline()
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
    }

    private fun enqueueOfflineDrain() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            OfflineSyncWorker.UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<OfflineSyncWorker>().build()
        )
    }

    private fun currentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        // INTERNET only — see class-level comment for why VALIDATED is omitted.
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
