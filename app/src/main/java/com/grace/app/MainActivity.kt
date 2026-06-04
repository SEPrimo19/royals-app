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

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // Deep-link target from a tapped notification ("prayer"/"devotional"/...).
    private val deepLink = MutableStateFlow<String?>(null)
    // Event-checkin deep link (grace://event-checkin/{id}) — captured separately
    // because it carries an id, not just a destination keyword.
    private val pendingCheckInEventId = MutableStateFlow<String?>(null)
    // Flips to true once refreshSessionIfNeeded() has finished. Until then
    // we keep the spinner up if DataStore says we have a user, so the
    // first query doesn't race ahead of the JWT restore.
    private val sessionReady = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deepLink.value = intent?.getStringExtra("destination")
        readCheckInIntent(intent)
        // Forward any OAuth callback URL (grace://login-callback?...) to
        // Supabase so it can extract the tokens and set the session.
        supabase.handleDeeplinks(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Best-effort silent refresh of an expired session on cold start.
        // Then re-sync the profile row so role changes made server-side
        // (admin promotions, etc.) reach the UI without requiring sign-out.
        // sessionReady flips once this is done so MAIN_GRAPH only renders
        // after the JWT is back in the Supabase client.
        lifecycleScope.launch {
            authRepository.refreshSessionIfNeeded()
            authRepository.syncProfileFromServer()
            // Restore completed-devotional rows so the Devo screen shows
            // 100% / ✓ for previously-completed days right after a fresh
            // install. Without this, Room is empty and the completion
            // ring drops to 0% even though the server has the record.
            devotionalRepository.syncMyDevoProgress()
            sessionReady.value = true
            // Push the CURRENT FCM token to Supabase on every cold start.
            // GraceFcmService.onNewToken only fires when the token actually
            // changes — after an app reinstall, Firebase issues a fresh
            // token but if the previous one is still in users.fcm_token,
            // every push fan-out attempt against that stale token returns
            // FCM "NotRegistered" (HTTP 404) and the user silently never
            // sees notifications. Proactively re-uploading the live token
            // here closes that gap.
            refreshFcmTokenIfSignedIn()
        }
        // Watch the Supabase session — every transition to Authenticated
        // (email/password sign-in, Google native, browser OAuth callback)
        // mirrors the profile into DataStore so prefs.userId flips and the
        // NavHost re-keys to MAIN_GRAPH. Without this, the Google flow could
        // race and leave the user staring at the Login screen after success.
        lifecycleScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                if (status is SessionStatus.Authenticated) {
                    authRepository.syncProfileFromServer()
                    devotionalRepository.syncMyDevoProgress()
                    // Also push the FCM token NOW — the cold-start call in
                    // onCreate ran before sign-in (when uid was null and the
                    // helper noop'd), so without this the freshly-signed-in
                    // user would have NULL fcm_token in Supabase and miss
                    // their first batch of pushes.
                    refreshFcmTokenIfSignedIn()
                }
            }
        }
        // OFFLINE-FIRST: when connectivity comes back AFTER a cold-start
        // that had no network, retry the session refresh so the user
        // transparently transitions from "offline view with stale auth"
        // to "online with active session". Without this hook, the user
        // would have to manually close + reopen the app for the SDK to
        // try again. drop(1) skips the initial state emission so we
        // only react to a real false→true transition.
        lifecycleScope.launch {
            networkMonitor.networkState
                .drop(1)
                .collect { online ->
                    if (online && supabase.auth.currentSessionOrNull() == null) {
                        authRepository.refreshSessionIfNeeded()
                    }
                }
        }

        setContent {
            // Observe the user's text-size + theme preferences so a change in
            // Settings takes effect immediately app-wide.
            val fontScale by prefs.fontScale.collectAsState(initial = 1.0f)
            val themeMode by prefs.themeMode.collectAsState(
                // Initial value before DataStore's first emit — must match
                // the default in UserPreferences.DEFAULT_THEME_MODE so the
                // first frame doesn't flash a different palette.
                initial = com.grace.app.data.datastore.ThemeMode.LIGHT
            )
            // SYSTEM mode mirrors the OS's dark-theme setting; the others
            // force a palette regardless of the OS.
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
                    networkMonitor, prefs, deepLink,
                    pendingCheckInEventId, sessionReady
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink.value = intent.getStringExtra("destination")
        readCheckInIntent(intent)
        // Also process auth deep-link redirects that arrive after the app is
        // already running (the Custom Tab was open over our activity).
        supabase.handleDeeplinks(intent)
        // After Google OAuth completes, mirror the new profile into DataStore.
        lifecycleScope.launch { authRepository.syncProfileFromServer() }
    }

    /** Pulls the event id out of a grace://event-checkin/{id} VIEW intent. */
    private fun readCheckInIntent(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "grace" && data.host == "event-checkin") {
            val id = data.lastPathSegment ?: return
            pendingCheckInEventId.value = id
        }
    }

    /**
     * Fetches the live FCM token from Firebase and mirrors it to Supabase if
     * the user is signed in. Fire-and-forget — callback-based to avoid
     * pulling in a separate kotlinx-coroutines-play-services dependency.
     * Wrapped in runCatching because FirebaseMessaging can throw on devices
     * with broken Google Play Services or missing configuration; a token
     * refresh failure must never bring down the activity.
     */
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
                .addOnFailureListener { /* swallow — diagnostic only */ }
        }
    }
}

@Composable
private fun GraceApp(
    networkMonitor: NetworkMonitor,
    prefs: UserPreferencesRepo,
    deepLink: StateFlow<String?>,
    pendingCheckInEventId: StateFlow<String?>,
    sessionReady: StateFlow<Boolean>
) {
    val isOnline by networkMonitor.networkState.collectAsStateWithLifecycle()
    val userId by prefs.userId.collectAsState(initial = SESSION_LOADING)
    val pendingDeepLink by deepLink.collectAsState()
    val pendingCheckIn by pendingCheckInEventId.collectAsState()
    val isSessionReady by sessionReady.collectAsState()

    // If DataStore has a user id but the Supabase session restore hasn't
    // completed yet, hold the spinner — otherwise the first MAIN_GRAPH
    // network query goes out anonymous and silently returns nothing.
    val waitingForSessionRestore =
        userId != SESSION_LOADING && !userId.isNullOrBlank() && !isSessionReady

    Box(modifier = Modifier.fillMaxSize().background(GraceDeepBlue)) {
        when {
            userId == SESSION_LOADING || waitingForSessionRestore -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                val startGraph =
                    if (userId.isNullOrBlank()) Screen.AUTH_GRAPH else Screen.MAIN_GRAPH
                // Key on startGraph so signing in/out rebuilds the NavController
                // and NavHost. Without this, NavHost.startDestination is locked
                // on first composition — sign-out would leave an inconsistent
                // back stack and close the app.
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
                                else -> null
                            }
                            if (route != null) {
                                // Wait for the NavHost to finish wiring the
                                // graph; navigate() throws "Navigation graph
                                // has not been set" if called before the first
                                // frame composes the NavHost. This race
                                // surfaces specifically when the activity is
                                // launched cold from a tapped notification.
                                navController.currentBackStackEntryFlow.first()
                                // Pop up to Home so pressing Back from the
                                // deep-linked screen returns to a sensible
                                // place rather than diving into a stale stack.
                                // singleTop prevents duplicating the screen
                                // if the user re-taps the same notification.
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
                    // Event QR scan deep link — route into the check-in screen
                    // for the scanned eventId. Only fires once authenticated.
                    if (startGraph == Screen.MAIN_GRAPH && pendingCheckIn != null) {
                        LaunchedEffect(pendingCheckIn) {
                            // Same graph-ready guard as the notification
                            // deep link above — see comment there.
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

        AnimatedVisibility(
            visible = !isOnline,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Text(
                text = stringResource(R.string.offline_banner),
                color = GraceCream,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GraceOrange)
                    .padding(top = 36.dp, bottom = 10.dp)
            )
        }
    }
}

// Sentinel distinct from "no session" (null/blank) and "has session".
private const val SESSION_LOADING = "__loading__"
