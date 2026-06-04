package com.grace.app.presentation.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceGold

private data class NavTab(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    NavTab(Screen.Home, "Home", Icons.Filled.Home),
    NavTab(Screen.Devotional, "Devo", Icons.Filled.MenuBook),
    NavTab(Screen.Prayer, "Prayer", Icons.Filled.VolunteerActivism),
    NavTab(Screen.Feed, "Feed", Icons.Filled.Spa),
    NavTab(Screen.Leaders, "Leaders", Icons.Filled.Forum)
)

@Composable
fun BottomNavBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar(containerColor = GraceCardBg) {
        tabs.forEach { tab ->
            // Hierarchy-aware so selection is correct across the nested graph.
            val selected = currentDestination?.hierarchy?.any {
                it.route == tab.screen.route
            } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) navController.navigateToTab(tab.screen.route)
                },
                icon = {
                    Icon(
                        tab.icon,
                        contentDescription = tab.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = GraceGold,
                    selectedTextColor = GraceGold,
                    indicatorColor = GraceCardBg,
                    unselectedIconColor = GraceCreamDim,
                    unselectedTextColor = GraceCreamDim
                )
            )
        }
    }
}

// Reserved for the gold active-dot indicator refinement in Prompt 8 polish.
@Suppress("unused")
private val activeDotShape = CircleShape
