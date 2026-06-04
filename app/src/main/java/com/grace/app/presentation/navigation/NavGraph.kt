package com.grace.app.presentation.navigation

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.grace.app.presentation.components.AppDrawer
import kotlinx.coroutines.launch
import com.grace.app.presentation.screens.admin.AdminComplianceReportScreen
import com.grace.app.presentation.screens.admin.AdminScreen
import com.grace.app.presentation.screens.admin.AdminUserDetailScreen
import com.grace.app.presentation.screens.admin.BulkEmailScreen
import com.grace.app.presentation.screens.auth.ClaimRecordScreen
import com.grace.app.presentation.screens.auth.LoginScreen
import com.grace.app.presentation.screens.auth.ProfileSetupScreen
import com.grace.app.presentation.screens.auth.SignUpScreen
import com.grace.app.presentation.screens.community.CommunityHubScreen
import com.grace.app.presentation.screens.devotional.DevotionalScreen
import com.grace.app.presentation.screens.events.EventCheckInScreen
import com.grace.app.presentation.screens.events.EventFormScreen
import com.grace.app.presentation.screens.events.EventQrScreen
import com.grace.app.presentation.screens.events.EventRosterScreen
import com.grace.app.presentation.screens.events.EventsScreen
import com.grace.app.presentation.screens.feed.FeedScreen
import com.grace.app.presentation.screens.games.FillInBlankScreen
import com.grace.app.presentation.screens.games.GamesHomeScreen
import com.grace.app.presentation.screens.games.LeaderboardScreen
import com.grace.app.presentation.screens.games.MemoryMatchScreen
import com.grace.app.presentation.screens.games.TimelineSortScreen
import com.grace.app.presentation.screens.games.VerseScrambleScreen
import com.grace.app.presentation.screens.games.WhoAmIScreen
import com.grace.app.presentation.screens.games.TriviaScreen
import com.grace.app.presentation.screens.games.admin.EditPassageScreen
import com.grace.app.presentation.screens.games.admin.EditQuestionScreen
import com.grace.app.presentation.screens.games.admin.ManageContentScreen
import com.grace.app.presentation.screens.home.HomeScreen
import com.grace.app.presentation.screens.journal.MyJournalScreen
import com.grace.app.presentation.screens.leader.LeaderScreen
import com.grace.app.presentation.screens.leader.AddProxyMemberScreen
import com.grace.app.presentation.screens.leader.LogReflectionOnBehalfScreen
import com.grace.app.presentation.screens.leader.MemberDetailScreen
import com.grace.app.presentation.screens.leader.MemberReflectionsScreen
import com.grace.app.presentation.screens.leader.MemberReportScreen
import com.grace.app.presentation.screens.leader.MyMembersScreen
import com.grace.app.presentation.screens.leader.PostPrayerOnBehalfScreen
import com.grace.app.presentation.screens.lifegroup.LifeGroupScreen
import com.grace.app.presentation.screens.mycontent.MyContentScreen
import com.grace.app.presentation.screens.prayer.PrayerWallScreen
import com.grace.app.presentation.screens.progress.MyAttendanceScreen
import com.grace.app.presentation.screens.progress.MyProgressScreen
import com.grace.app.presentation.screens.profile.EditProfileScreen
import com.grace.app.presentation.screens.settings.SettingsScreen

/**
 * Root navigation. [startGraph] is decided by MainActivity from the persisted
 * session. After auth completes we jump to MAIN_GRAPH and clear the whole auth
 * back stack so Back can't return to Login.
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startGraph: String
) {
    // App-level drawer state. Only swipeable on main-graph destinations so
    // the user can't accidentally open the side menu on Login / SignUp.
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onMainGraph = currentRoute != null &&
        currentRoute != Screen.Login.route &&
        currentRoute != Screen.SignUp.route &&
        currentRoute != Screen.ProfileSetup.route

    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }
    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Allow swipe-to-open / swipe-to-close on main-graph screens;
        // disable entirely on auth screens so the drawer can't sneak in.
        gesturesEnabled = onMainGraph,
        drawerContent = {
            AppDrawer(
                onCloseDrawer = closeDrawer,
                onOpenEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onOpenLifeGroup = { navController.navigate(Screen.LifeGroup.route) },
                onOpenMyContent = { navController.navigate(Screen.MyContent.route) },
                onOpenMyAttendance = { navController.navigate(Screen.MyAttendance.route) },
                onOpenMyProgress = { navController.navigate(Screen.MyProgress.route) },
                onOpenMyJournal = { navController.navigate(Screen.MyJournal.route) },
                onOpenMyMembers = { navController.navigate(Screen.MyMembers.route) },
                onOpenManageContent = { navController.navigate(Screen.ManageContent.route) },
                onOpenAdmin = { navController.navigate(Screen.Admin.route) },
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
    ) {
        NavGraphContent(navController, startGraph, openDrawer)
    }
}

@Composable
private fun NavGraphContent(
    navController: NavHostController,
    startGraph: String,
    onOpenMenu: () -> Unit
) {
    NavHost(navController = navController, startDestination = startGraph) {

        navigation(
            startDestination = Screen.Login.route,
            route = Screen.AUTH_GRAPH
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateHome = { navController.toMainGraph() },
                    onNavigateSignUp = { navController.navigate(Screen.SignUp.route) }
                )
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onNavigateProfileSetup = {
                        navController.navigate(Screen.ProfileSetup.route)
                    },
                    onNavigateLogin = {
                        navController.popBackStack(Screen.Login.route, inclusive = false)
                    }
                )
            }
            composable(Screen.ProfileSetup.route) {
                ProfileSetupScreen(
                    onNavigateHome = { navController.toMainGraph() }
                )
            }
        }

        navigation(
            // Phase P.5 — ClaimRecord is the start destination, NOT Home.
            // MainActivity's userId observer rebuilds the NavHost on auth
            // state change, which races against any toMainGraph() navigate
            // call and lands the user on whatever startDestination is set
            // here. Putting ClaimRecord first ensures the claim check
            // ALWAYS runs on first MAIN_GRAPH entry — fresh signup or
            // cold-start with a persisted session. The screen auto-
            // redirects to Home in <500ms when there's no claimable proxy.
            startDestination = Screen.ClaimRecord.route,
            route = Screen.MAIN_GRAPH
        ) {
            // Phase P.5 — start destination for MAIN_GRAPH. Self-detects
            // whether a proxy record matches the signed-in user's email and
            // either shows the "We found your record" prompt OR redirects
            // to Home in <500ms (one RPC round-trip) when there's no match.
            // Runs on EVERY MAIN_GRAPH entry — fresh signups + cold starts
            // alike — so existing users see a brief loading flash but no
            // wrong UX, and proxy claim NEVER gets bypassed by the
            // MainActivity auth-state race that would otherwise short-
            // circuit the SignUp → ProfileSetup → ClaimRecord chain.
            composable(Screen.ClaimRecord.route) {
                ClaimRecordScreen(
                    onDone = {
                        navController.navigate(Screen.Home.route) {
                            // popUpTo ClaimRecord inclusive so Back from Home
                            // doesn't bounce back here (would loop).
                            popUpTo(Screen.ClaimRecord.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    // Tab destinations go through the shared helper so the back
                    // stack matches the bottom bar exactly.
                    onOpenDevotional = { navController.navigateToTab(Screen.Devotional.route) },
                    onOpenPrayer = { navController.navigateToTab(Screen.Prayer.route) },
                    onOpenFeed = { navController.navigateToTab(Screen.Feed.route) },
                    onOpenLeaders = { navController.navigateToTab(Screen.Leaders.route) },
                    // Top-left burger button opens the app-level drawer (My
                    // Content / Reminders / Admin / Settings). Settings is
                    // no longer reachable from the Home top bar directly —
                    // it lives inside the drawer's footer.
                    onOpenMenu = onOpenMenu,
                    onOpenEvents = { navController.navigate(Screen.Events.route) },
                    onOpenCommunity = {
                        navController.navigate(Screen.CommunityHub.route)
                    },
                    onOpenGames = { navController.navigate(Screen.GamesHome.route) }
                )
            }
            composable(Screen.GamesHome.route) {
                GamesHomeScreen(
                    onBack = { navController.popBackStack() },
                    onStartDailyChallenge = { difficulty ->
                        val mode = "daily-" + difficulty.dbValue
                        navController.navigate(Screen.Trivia(mode).createRoute())
                    },
                    onStartPractice = {
                        navController.navigate(Screen.Trivia("practice").createRoute())
                    },
                    onStartDailyVerse = {
                        navController.navigate(Screen.DailyVerse.route)
                    },
                    onStartWhoAmI = {
                        navController.navigate(Screen.WhoAmI.route)
                    },
                    onStartMemoryMatch = {
                        navController.navigate(Screen.MemoryMatch.route)
                    },
                    onStartVerseScramble = {
                        navController.navigate(Screen.VerseScramble.route)
                    },
                    onStartTimelineSort = {
                        navController.navigate(Screen.TimelineSort.route)
                    },
                    onViewLeaderboard = {
                        navController.navigate(Screen.Leaderboard.route)
                    }
                )
            }
            composable(Screen.DailyVerse.route) {
                FillInBlankScreen(onExit = { navController.popBackStack() })
            }
            composable(Screen.WhoAmI.route) {
                WhoAmIScreen(onExit = { navController.popBackStack() })
            }
            composable(Screen.MemoryMatch.route) {
                MemoryMatchScreen(onExit = { navController.popBackStack() })
            }
            composable(Screen.VerseScramble.route) {
                VerseScrambleScreen(onExit = { navController.popBackStack() })
            }
            composable(Screen.TimelineSort.route) {
                TimelineSortScreen(onExit = { navController.popBackStack() })
            }
            composable(Screen.Leaderboard.route) {
                LeaderboardScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.ManageContent.route) {
                ManageContentScreen(
                    onBack = { navController.popBackStack() },
                    onNewQuestion = {
                        navController.navigate(
                            Screen.EditQuestion(Screen.EditQuestion.NEW).createRoute()
                        )
                    },
                    onEditQuestion = { id ->
                        navController.navigate(Screen.EditQuestion(id).createRoute())
                    },
                    onNewPassage = {
                        navController.navigate(
                            Screen.EditPassage(Screen.EditPassage.NEW).createRoute()
                        )
                    },
                    onEditPassage = { id ->
                        navController.navigate(Screen.EditPassage(id).createRoute())
                    }
                )
            }
            composable(
                route = "edit_question/{questionId}",
                arguments = listOf(navArgument("questionId") { type = NavType.StringType })
            ) {
                EditQuestionScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = "edit_passage/{passageId}",
                arguments = listOf(navArgument("passageId") { type = NavType.StringType })
            ) {
                EditPassageScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = "trivia/{mode}",
                arguments = listOf(navArgument("mode") { type = NavType.StringType })
            ) {
                TriviaScreen(onExit = { navController.popBackStack() })
            }
            composable(Screen.Settings.route) {
                // Slimmed Settings now hosts only account-management concerns
                // (profile, password, notifications, privacy, sign out). All
                // navigation shortcuts (My Content, reminders, admin, etc.)
                // moved into the app-level drawer (AppDrawer.kt).
                SettingsScreen(
                    onNavigateLogin = {
                        navController.navigate(Screen.AUTH_GRAPH) {
                            popUpTo(Screen.MAIN_GRAPH) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                    onOpenEditProfile = {
                        navController.navigate(Screen.EditProfile.route)
                    }
                )
            }
            composable(Screen.MyJournal.route) {
                MyJournalScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.CommunityHub.route) {
                CommunityHubScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLifeGroup = { navController.navigate(Screen.LifeGroup.route) },
                    onOpenMyContent = { navController.navigate(Screen.MyContent.route) },
                    onOpenMyAttendance = {
                        navController.navigate(Screen.MyAttendance.route)
                    },
                    onOpenMyProgress = {
                        navController.navigate(Screen.MyProgress.route)
                    },
                    onOpenMyJournal = {
                        navController.navigate(Screen.MyJournal.route)
                    }
                )
            }
            // Leader-only inbox + member detail screens. RLS on the backend
            // limits the data even if a non-leader navigates here directly.
            composable(Screen.MyMembers.route) {
                MyMembersScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMember = { memberId ->
                        navController.navigate(Screen.MemberDetail(memberId).createRoute())
                    },
                    onAddProxyMember = {
                        // From My Members the form is generic (no eventId) —
                        // just registers, doesn't mark attendance.
                        navController.navigate(Screen.AddProxyMember().createRoute())
                    }
                )
            }
            composable(
                route = "add_proxy_member?eventId={eventId}",
                arguments = listOf(navArgument("eventId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) {
                AddProxyMemberScreen(
                    onBack = { navController.popBackStack() },
                    onMemberAdded = {
                        // Pop the form screen — newly added member appears on
                        // the next MyMembers / EventRoster refresh.
                        navController.popBackStack()
                    }
                )
            }
            composable(
                route = "member_detail/{memberId}",
                arguments = listOf(navArgument("memberId") { type = NavType.StringType })
            ) {
                MemberDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenReflections = { id ->
                        navController.navigate(Screen.MemberReflections(id).createRoute())
                    },
                    onPostPrayerOnBehalf = { id ->
                        navController.navigate(Screen.PostPrayerOnBehalf(id).createRoute())
                    },
                    onLogReflectionOnBehalf = { id ->
                        navController.navigate(Screen.LogReflectionOnBehalf(id).createRoute())
                    },
                    onGenerateReport = { id ->
                        navController.navigate(Screen.MemberReport(id).createRoute())
                    }
                )
            }
            composable(
                route = "member_reflections/{memberId}",
                arguments = listOf(navArgument("memberId") { type = NavType.StringType })
            ) {
                MemberReflectionsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "post_prayer_on_behalf/{memberId}",
                arguments = listOf(navArgument("memberId") { type = NavType.StringType })
            ) {
                PostPrayerOnBehalfScreen(
                    onBack = { navController.popBackStack() },
                    onPosted = { navController.popBackStack() }
                )
            }
            composable(
                route = "log_reflection_on_behalf/{memberId}",
                arguments = listOf(navArgument("memberId") { type = NavType.StringType })
            ) {
                LogReflectionOnBehalfScreen(
                    onBack = { navController.popBackStack() },
                    onLogged = { navController.popBackStack() }
                )
            }
            composable(
                route = "member_report/{memberId}",
                arguments = listOf(navArgument("memberId") { type = NavType.StringType })
            ) {
                MemberReportScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.MyContent.route) {
                MyContentScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.MyAttendance.route) {
                MyAttendanceScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.MyProgress.route) {
                MyProgressScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAttendance = {
                        navController.navigate(Screen.MyAttendance.route)
                    }
                )
            }
            // Settings → My Content wiring is in the Settings composable above.
            composable(Screen.Events.route) {
                EventsScreen(
                    onBack = { navController.popBackStack() },
                    onShowQr = { eventId ->
                        navController.navigate(Screen.EventQr(eventId).createRoute())
                    },
                    onShowRoster = { eventId ->
                        navController.navigate(Screen.EventRoster(eventId).createRoute())
                    },
                    onCreateEvent = {
                        navController.navigate(
                            Screen.EventForm(Screen.EventForm.NEW).createRoute()
                        )
                    },
                    onEditEvent = { eventId ->
                        navController.navigate(Screen.EventForm(eventId).createRoute())
                    },
                    onScannedEvent = { eventId ->
                        navController.navigate(
                            Screen.EventCheckIn(eventId).createRoute()
                        )
                    },
                    onEmailEvent = { subject, message ->
                        navController.navigate(
                            Screen.BulkEmail(subject = subject, message = message)
                                .createRoute()
                        )
                    }
                )
            }
            composable(
                route = "event_form/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                EventFormScreen(onDone = { navController.popBackStack() })
            }
            composable(
                route = "event_qr/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                EventQrScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "event_checkin/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                EventCheckInScreen(onDone = {
                    // After check-in, drop the confirmation page and land on Events.
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                    navController.navigate(Screen.Events.route)
                })
            }
            composable(
                route = "event_roster/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                // Resolve the eventId from the nav arg so the inline + Add
                // path can forward it to AddProxyMember (P.2.6 flow).
                val eventId = backStackEntry.arguments?.getString("eventId").orEmpty()
                EventRosterScreen(
                    onBack = { navController.popBackStack() },
                    onAddProxyMember = {
                        navController.navigate(
                            Screen.AddProxyMember(eventId = eventId).createRoute()
                        )
                    }
                )
            }
            composable(Screen.Admin.route) {
                AdminScreen(
                    onBack = { navController.popBackStack() },
                    onSendAnnouncement = {
                        navController.navigate(Screen.BulkEmail().createRoute())
                    },
                    onOpenUser = { userId ->
                        navController.navigate(
                            Screen.AdminUserDetail(userId).createRoute()
                        )
                    },
                    onOpenComplianceReport = {
                        navController.navigate(Screen.AdminComplianceReport.route)
                    }
                )
            }
            composable(Screen.AdminComplianceReport.route) {
                AdminComplianceReportScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "admin_user_detail/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) {
                AdminUserDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "bulk_email?subject={subject}&message={message}",
                arguments = listOf(
                    navArgument("subject") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("message") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) {
                BulkEmailScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.LifeGroup.route) {
                LifeGroupScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.EditProfile.route) {
                EditProfileScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Devotional.route) {
                DevotionalScreen(
                    onBackToHome = { navController.navigateToTab(Screen.Home.route) },
                    onOpenMenu = onOpenMenu
                )
            }
            composable(Screen.Prayer.route) {
                PrayerWallScreen(onOpenMenu = onOpenMenu)
            }
            composable(Screen.Feed.route) {
                FeedScreen(onOpenMenu = onOpenMenu)
            }
            composable(Screen.Leaders.route) {
                LeaderScreen(onOpenMenu = onOpenMenu)
            }
        }
    }
}

private fun NavHostController.toMainGraph() {
    // Route through ClaimRecord on every fresh signin so the user gets the
    // "we found your record" prompt if their email matches a leader-
    // registered proxy row. ClaimRecord redirects to Home immediately
    // when there's no match — invisible for the common case.
    navigate(Screen.ClaimRecord.route) {
        popUpTo(Screen.AUTH_GRAPH) { inclusive = true }
        launchSingleTop = true
    }
}
