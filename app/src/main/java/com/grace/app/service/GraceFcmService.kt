package com.grace.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.grace.app.MainActivity
import com.grace.app.R
import com.grace.app.core.NotificationChannels
import com.grace.app.data.datastore.UserPreferencesRepo
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class GraceFcmService : FirebaseMessagingService() {

    @Inject lateinit var supabase: SupabaseClient
    @Inject lateinit var prefs: UserPreferencesRepo

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            prefs.setFcmToken(token)
            val uid = supabase.auth.currentUserOrNull()?.id ?: prefs.userId.first()
            if (!uid.isNullOrBlank()) {
                runCatching {
                    supabase.from("users").update({ set("fcm_token", token) }) {
                        filter { eq("id", uid) }
                    }
                }
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val channelKey = message.data["channel"].orEmpty()
        val channelId = when (channelKey) {
            "prayer" -> NotificationChannels.PRAYER
            "devotional" -> NotificationChannels.DEVOTIONAL
            "message" -> NotificationChannels.MESSAGES
            "community" -> NotificationChannels.COMMUNITY
            else -> NotificationChannels.COMMUNITY
        }

        val enabled = runBlocking {
            when (channelId) {
                NotificationChannels.PRAYER -> prefs.notifPrayerEnabled.first()
                NotificationChannels.DEVOTIONAL -> prefs.notifDevoEnabled.first()
                NotificationChannels.MESSAGES -> prefs.notifMessagesEnabled.first()
                else -> prefs.notifCommunityEnabled.first()
            }
        }
        if (!enabled) return

        val destination = message.data["destination"] ?: channelKey
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("destination", destination)
            message.data["id"]?.let { putExtra("id", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = runCatching {
            android.graphics.BitmapFactory.decodeResource(
                resources, R.drawable.royals_logo_official
            )
        }.getOrNull()
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(message.notification?.title ?: message.data["title"] ?: "Royals")
            .setContentText(message.notification?.body ?: message.data["body"].orEmpty())
            .setSmallIcon(R.drawable.ic_grace_notification)
            .apply { largeIcon?.let { setLargeIcon(it) } }
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
