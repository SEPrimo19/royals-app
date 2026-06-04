package com.grace.app.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Single source of truth for switching between bottom-tab destinations.
 *
 * WHY: Home/Devotional/Prayer/Feed/Leaders are reachable from BOTH the bottom
 * bar AND the Home quick-action cards. If those two entry points navigate
 * differently, a tab destination ends up as a child push in one path and a tab
 * root in another. The bottom bar's saveState/restoreState then keys state to
 * the wrong destination — tapping "Home" would resurrect the saved "Prayer"
 * state instead of going Home. Routing every tab switch through this one helper
 * keeps the back stack consistent regardless of where the tap came from.
 */
fun NavController.navigateToTab(route: String) {
    navigate(route) {
        // Keep the start destination at the root; save the leaving tab's state.
        popUpTo(graph.findStartDestination().id) { saveState = true }
        // Never stack the same tab twice.
        launchSingleTop = true
        // Restore this tab's previously saved state if we've been here before.
        restoreState = true
    }
}
