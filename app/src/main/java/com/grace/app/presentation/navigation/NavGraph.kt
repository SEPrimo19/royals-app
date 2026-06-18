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

@Composable
fun NavGraph(
    navController: NavHostController,
    startGraph: String
) {
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
        gesturesEnabled = onMainGraph,
        drawerContent = {
            AppDrawer(
                onCloseDrawer = closeDrawer,
                onOpenEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onOpenBible = { navController.navigate(Screen.Bible.route) },
                onOpenStudyNotes = { navController.navigate(Screen.StudyNotes.route) },
                onOpenLifeGroup = { navController.navigate(Screen.LifeGroup.route) },
                onOpenFindCell = { navController.navigate(Screen.FindCell.route) },
                onOpenDiscipleship = {
                    navController.navigate(Screen.DiscipleshipLibrary.route)
                },
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
            startDestination = Screen.ClaimRecord.route,
            route = Screen.MAIN_GRAPH
        ) {
            composable(Screen.ClaimRecord.route) {
                ClaimRecordScreen(
                    onDone = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.ClaimRecord.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Home.route) {
                HomeScreen(
                    onOpenDevotional = { navController.navigateToTab(Screen.Devotional.route) },
                    onOpenPrayer = { navController.navigateToTab(Screen.Prayer.route) },
                    onOpenFeed = { navController.navigateToTab(Screen.Feed.route) },
                    onOpenLeaders = { navController.navigateToTab(Screen.Leaders.route) },
                    onOpenMenu = onOpenMenu,
                    onOpenEvents = { navController.navigate(Screen.Events.route) },
                    onOpenCommunity = {
                        navController.navigate(Screen.CommunityHub.route)
                    },
                    onOpenGames = { navController.navigate(Screen.GamesHome.route) },
                    onOpenDiscipleship = {
                        navController.navigate(Screen.DiscipleshipLibrary.route)
                    },
                    onOpenBible = { navController.navigate(Screen.Bible.route) }
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
                    },
                    onOpenPrivacy = {
                        navController.navigate(Screen.Privacy.route)
                    }
                )
            }
            composable(Screen.Privacy.route) {
                com.grace.app.presentation.screens.privacy.PrivacyScreen(
                    onBack = { navController.popBackStack() }
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
            composable(Screen.MyMembers.route) {
                MyMembersScreen(
                    onBack = { navController.popBackStack() },
                    onOpenMember = { memberId ->
                        navController.navigate(Screen.MemberDetail(memberId).createRoute())
                    },
                    onAddProxyMember = {
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
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                    navController.navigate(Screen.Events.route)
                })
            }
            composable(
                route = "event_roster/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
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
            composable(Screen.FindCell.route) {
                com.grace.app.presentation.screens.lifegroup.FindCellScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.DiscipleshipLibrary.route) {
                com.grace.app.presentation.screens.discipleship
                    .DiscipleshipLibraryScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAuthor = { activityId ->
                            val id = activityId
                                ?: Screen.DiscipleshipAuthor.NEW
                            navController.navigate(
                                Screen.DiscipleshipAuthor(id).createRoute()
                            )
                        }
                    )
            }
            composable(
                Screen.DiscipleshipAuthor("new").route,
                arguments = listOf(
                    androidx.navigation.navArgument("activityId") {
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) {
                com.grace.app.presentation.screens.discipleship
                    .DiscipleshipAuthorScreen(
                        onBack = { navController.popBackStack() }
                    )
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
            composable(Screen.Bible.route) {
                com.grace.app.presentation.screens.bible.BibleReaderScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.StudyNotes.route) {
                com.grace.app.presentation.screens.bible.MyStudyNotesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenNote = { noteId ->
                        navController.navigate(Screen.BibleNoteEditor(noteId).createRoute())
                    }
                )
            }
            composable(
                route = "bible_note/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) {
                com.grace.app.presentation.screens.bible.BibleNoteEditorScreen(
                    onBack = { navController.popBackStack() }
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
    navigate(Screen.ClaimRecord.route) {
        popUpTo(Screen.AUTH_GRAPH) { inclusive = true }
        launchSingleTop = true
    }
}
