package com.grace.app.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.presentation.components.CompletionRing
import com.grace.app.presentation.theme.GraceBlue
import com.grace.app.presentation.theme.GraceCardAlt
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGoldDim
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GracePurple
import com.grace.app.presentation.theme.GraceRose
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    onOpenDevotional: () -> Unit,
    onOpenPrayer: () -> Unit,
    onOpenFeed: () -> Unit,
    onOpenLeaders: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenEvents: () -> Unit,
    onOpenCommunity: () -> Unit = {},
    onOpenGames: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val today = formattedToday()

    // Re-pull server-backed sections on each screen entry (the VM persists
    // across bottom-bar tabs, so init only fires once).
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.greeting,
                    color = GraceGold,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                )
                Text(
                    state.userName.ifBlank { "Friend" },
                    color = GraceCream,
                    fontSize = 30.sp
                )
                Text(today, color = GraceCreamDim, fontSize = 13.sp)
            }
            // Hide the streak badge entirely for users at 0 — an empty 🔥 reads
            // as a failure state, the opposite of the intent.
            if (state.streak > 0) {
                Box(
                    modifier = Modifier
                        .background(GraceGold.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔥 ${state.streak}", color = GraceGold, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold)
                        Text("DAY STREAK", color = GraceGoldDim, fontSize = 9.sp)
                    }
                }
            }
            IconButton(onClick = onOpenMenu) {
                Icon(Icons.Filled.Menu, "Menu", tint = GraceCreamDim)
            }
        }

        Spacer(Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = GraceCardAlt),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenDevotional() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TODAY'S DEVOTIONAL", color = GraceGold, fontSize = 10.sp,
                        letterSpacing = 3.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        state.todayDevotional?.title ?: "Tap to load today's devotional",
                        color = GraceCream,
                        fontSize = 20.sp
                    )
                    state.todayDevotional?.let { d ->
                        Spacer(Modifier.height(4.dp))
                        Text(
                            d.verseText.take(80).let { if (d.verseText.length > 80) "$it…" else it },
                            color = GraceCreamDim,
                            fontSize = 13.sp,
                            fontStyle = FontStyle.Italic
                        )
                        Text(d.verseRef, color = GraceGold, fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (state.devoDone) "✓ Completed today" else "Tap to read & complete",
                            color = if (state.devoDone)
                                com.grace.app.presentation.theme.GraceGreen
                            else GraceCreamDim,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                CompletionRing(
                    progress = if (state.devoDone) 1f else 0f,
                    isDone = state.devoDone,
                    onTap = onOpenDevotional,
                    size = 64.dp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        // Community Hub gets top billing below the devotional — it's the
        // user's personal-progress doorway (attendance, journal, life group)
        // and used to live buried inside Settings. Now it's one tap away.
        QuickAction(
            "🏘️", "Community",
            "Life Group · Content · Progress · Journal",
            GraceGold, Modifier.fillMaxWidth(), onOpenCommunity
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("🙏", "Prayer Wall",
                "${state.recentPrayers.size} recent", GraceBlue,
                Modifier.weight(1f), onOpenPrayer)
            QuickAction("🌿", "Life Feed", "Share what God's doing",
                GraceGreen, Modifier.weight(1f), onOpenFeed)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAction("🤝", "My Leader",
                state.myLeader?.name ?: "Connect", GracePurple,
                Modifier.weight(1f), onOpenLeaders)
            QuickAction("📅", "Events", "See what's happening", GraceRose,
                Modifier.weight(1f), onOpenEvents)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Bible Games full-width — flagship feature deserves visual
            // weight on Home. Solo row so it doesn't get lost. The
            // subtitle upgrades from a generic CTA to "🔥 N day streak"
            // once the user has actually played the daily round.
            val gameSubtitle = if (state.gameStreak > 0)
                "🔥 ${state.gameStreak} day streak · keep it alive"
            else
                "Daily challenge · grow in the Word"
            QuickAction(
                "🎮", "Bible Games",
                gameSubtitle, GraceGold,
                Modifier.weight(1f), onOpenGames
            )
        }

        state.spotlightPost?.let { post ->
            Spacer(Modifier.height(20.dp))
            Text("✦ COMMUNITY SPOTLIGHT", color = GraceGold, fontSize = 10.sp,
                letterSpacing = 3.sp)
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = GraceCardBg)) {
                Column(Modifier.padding(16.dp)) {
                    Text(post.userName.ifBlank { "A Youth" }, color = GraceCream,
                        fontSize = 14.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(post.content, color = GraceCreamDim, fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        if (state.offlineVerseText != null) {
            Text("📡 OFFLINE VERSE CACHE", color = GraceGoldDim, fontSize = 9.sp,
                letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                state.offlineVerseText!!,
                color = GraceCreamDim,
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic
            )
            state.offlineVerseRef?.let {
                Text(it, color = GraceGold, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun formattedToday(): String =
    androidx.compose.runtime.remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
    }

@Composable
private fun QuickAction(
    emoji: String,
    title: String,
    subtitle: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(108.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.weight(1f))
            Text(title, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = GraceCreamDim, fontSize = 11.sp, maxLines = 1)
        }
    }
}
