package com.grace.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.LaunchedEffect
import com.google.firebase.messaging.FirebaseMessaging
import com.grace.app.data.datastore.UserPreferencesRepo
import com.grace.app.data.util.NetworkMonitor
import com.grace.app.domain.repository.AuthRepository
import com.grace.app.domain.repository.DevotionalRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.handleDeeplinks
import io.github.jan.supabase.postgrest.from
import com.grace.app.presentation.navigation.BottomNavBar
import com.grace.app.presentation.navigation.NavGraph
import com.grace.app.presentation.navigation.Screen
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceOrange
import com.grace.app.presentation.theme.GraceTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var networkMonitor: NetworkMonitor
    @Inject lateinit var prefs: UserPreferencesRepo
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var devotionalRepository: DevotionalRepository
    @Inject lateinit var supabase: SupabaseClient
    @Inject lateinit var profileRealtime:
        com.grace.app.data.util.ProfileRealtimeSubscriber

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val deepLink = MutableStateFlow<String?>(null)
    private val pendingCheckInEventId = MutableStateFlow<String?>(null)
    private val sessionReady = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLink.value = intent?.getStringExtra("destination")
        readCheckInIntent(intent)
        supabase.handleDeeplinks(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        lifecycleScope.launch {
            authRepository.refreshSessionIfNeeded()
            authRepository.syncProfileFromServer()
            devotionalRepository.syncMyDevoProgress()
            sessionReady.value = true
            refreshFcmTokenIfSignedIn()
            supabase.auth.currentUserOrNull()?.id?.let { profileRealtime.start(it) }
        }
        lifecycleScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    authRepository.syncProfileFromServer()
                    devotionalRepository.syncMyDevoProgress()
                    refreshFcmTokenIfSignedIn()
                    supabase.auth.currentUserOrNull()?.id
                        ?.let { profileRealtime.start(it) }
                } else {
                    profileRealtime.stop()
                }
            }
        }
        lifecycleScope.launch {
            networkMonitor.networkState
                .drop(1)
                .collect { online ->
                    if (online && supabase.auth.currentSessionOrNull() == null) {
                        authRepository.refreshSessionIfNeeded()
                    }
                }
        }

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                lifecycleScope.launch {
                    authRepository.syncProfileFromServer()
                }
            }
        })

        setContent {
            val fontScale by prefs.fontScale.collectAsState(initial = 1.0f)
            val themeMode by prefs.themeMode.collectAsState(
                initial = com.grace.app.data.datastore.ThemeMode.LIGHT
            )
            val systemInDark =
                androidx.compose.foundation.isSystemInDarkTheme()
            val palette = when (themeMode) {
                com.grace.app.data.datastore.ThemeMode.LIGHT ->
                    com.grace.app.presentation.theme.LightGracePalette
                com.grace.app.data.datastore.ThemeMode.DARK ->
                    com.grace.app.presentation.theme.DarkGracePalette
                com.grace.app.data.datastore.ThemeMode.SYSTEM ->
                    if (systemInDark)
                        com.grace.app.presentation.theme.DarkGracePalette
                    else
                        com.grace.app.presentation.theme.LightGracePalette
            }
            GraceTheme(fontScale = fontScale, palette = palette) {
                GraceApp(
                    prefs, deepLink,
                    pendingCheckInEventId, sessionReady
                )
            }
        }
    }

    override fun onDestroy() {
        profileRealtime.stop()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink.value = intent.getStringExtra("destination")
        readCheckInIntent(intent)
        supabase.handleDeeplinks(intent)
        lifecycleScope.launch { authRepository.syncProfileFromServer() }
    }

    private fun readCheckInIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "grace" && data.host == "event-checkin") {
            val id = data.lastPathSegment ?: return
            pendingCheckInEventId.value = id
        }
    }

    private fun refreshFcmTokenIfSignedIn() {
        runCatching {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (token.isNullOrBlank()) return@addOnSuccessListener
                    lifecycleScope.launch {
                        val uid = supabase.auth.currentUserOrNull()?.id
                            ?: prefs.userId.first()
                        if (uid.isNullOrBlank()) return@launch
                        prefs.setFcmToken(token)
                        runCatching {
                            supabase.from("users").update({
                                set("fcm_token", token)
                            }) {
                                filter { eq("id", uid) }
                            }
                        }
                    }
                }
                .addOnFailureListener {   }
        }
    }
}

@Composable
private fun GraceApp(
    prefs: UserPreferencesRepo,
    deepLink: StateFlow<String?>,
    pendingCheckInEventId: StateFlow<String?>,
    sessionReady: StateFlow<Boolean>
) {
    val userId by prefs.userId.collectAsState(initial = SESSION_LOADING)
    val pendingDeepLink by deepLink.collectAsState()
    val pendingCheckIn by pendingCheckInEventId.collectAsState()
    val isSessionReady by sessionReady.collectAsState()

    val stillBootstrapping = userId == SESSION_LOADING || !isSessionReady

    Box(modifier = Modifier.fillMaxSize().background(GraceDeepBlue)) {
        when {
            stillBootstrapping -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                val startGraph =
                    if (userId.isNullOrBlank()) Screen.AUTH_GRAPH else Screen.MAIN_GRAPH
                key(startGraph) {
                    val navController = rememberNavController()
                    if (startGraph == Screen.MAIN_GRAPH && pendingDeepLink != null) {
                        LaunchedEffect(pendingDeepLink) {
                            val route = when (pendingDeepLink) {
                                "prayer" -> Screen.Prayer.route
                                "devotional" -> Screen.Devotional.route
                                "message" -> Screen.Leaders.route
                                "community" -> Screen.Feed.route
                                "events" -> Screen.Events.route
                                "my_progress" -> Screen.MyProgress.route
                                "games" -> Screen.GamesHome.route
                                else -> null
                            }
                            if (route != null) {
                                navController.currentBackStackEntryFlow.first()
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                    if (startGraph == Screen.MAIN_GRAPH && pendingCheckIn != null) {
                        LaunchedEffect(pendingCheckIn) {
                            navController.currentBackStackEntryFlow.first()
                            navController.navigate(
                                Screen.EventCheckIn(pendingCheckIn!!).createRoute()
                            )
                        }
                    }
                    Scaffold(
                        bottomBar = {
                            if (startGraph == Screen.MAIN_GRAPH) BottomNavBar(navController)
                        }
                    ) { inner ->
                        Box(modifier = Modifier.fillMaxSize().padding(inner)) {
                            NavGraph(navController = navController, startGraph = startGraph)
                        }
                    }
                }
            }
        }
    }
}

private const val SESSION_LOADING = "__loading__"
