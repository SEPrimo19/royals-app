package com.grace.app.presentation.screens.games

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.GameStats
import com.grace.app.domain.model.LeaderboardEntry
import com.grace.app.presentation.theme.GraceBlue
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GracePurple
import com.grace.app.presentation.theme.GraceRose
import java.time.Duration
import java.time.LocalDateTime

@Composable
fun GamesHomeScreen(
    onBack: () -> Unit,
    onStartDailyChallenge: (com.grace.app.domain.model.GameDifficulty) -> Unit,
    onStartPractice: () -> Unit,
    onStartDailyVerse: () -> Unit,
    onStartWhoAmI: () -> Unit,
    onStartMemoryMatch: () -> Unit,
    onStartVerseScramble: () -> Unit,
    onStartTimelineSort: () -> Unit,
    onViewLeaderboard: () -> Unit,
    viewModel: GamesHomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←", color = GraceCream, fontSize = 22.sp,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
            )
            Column {
                Text("Bible Games 🎮", color = GraceCream, fontSize = 24.sp)
                Text(
                    "Grow in the Word. Play with your group.",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (state.isLoading && state.stats.totalPoints == 0L) {
            Box(Modifier.fillMaxWidth().height(220.dp), Alignment.Center) {
                CircularProgressIndicator(color = GraceGold)
            }
        } else {
            StatsHero(state.stats)
            Spacer(Modifier.height(14.dp))
            Text(
                "DAILY CHALLENGES · 10 QUESTIONS EACH",
                color = GraceCreamDim, fontSize = 10.sp,
                letterSpacing = 2.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            DifficultyCard(
                difficulty = com.grace.app.domain.model.GameDifficulty.EASY,
                stats = state.stats,
                onPlay = { onStartDailyChallenge(
                    com.grace.app.domain.model.GameDifficulty.EASY
                ) }
            )
            Spacer(Modifier.height(8.dp))
            DifficultyCard(
                difficulty = com.grace.app.domain.model.GameDifficulty.MEDIUM,
                stats = state.stats,
                onPlay = { onStartDailyChallenge(
                    com.grace.app.domain.model.GameDifficulty.MEDIUM
                ) }
            )
            Spacer(Modifier.height(8.dp))
            DifficultyCard(
                difficulty = com.grace.app.domain.model.GameDifficulty.HARD,
                stats = state.stats,
                onPlay = { onStartDailyChallenge(
                    com.grace.app.domain.model.GameDifficulty.HARD
                ) }
            )
            Spacer(Modifier.height(8.dp))
            DailyVerseCard(stats = state.stats, onPlay = onStartDailyVerse)
            Spacer(Modifier.height(14.dp))
            PracticeCard(onClick = onStartPractice)
            Spacer(Modifier.height(8.dp))
            WhoAmICard(onClick = onStartWhoAmI)
            Spacer(Modifier.height(8.dp))
            MemoryCardsCard(onClick = onStartMemoryMatch)
            Spacer(Modifier.height(8.dp))
            VerseScrambleCard(onClick = onStartVerseScramble)
            Spacer(Modifier.height(8.dp))
            TimelineSortCard(onClick = onStartTimelineSort)
            Spacer(Modifier.height(20.dp))
            LeaderboardPreview(state.leaderboardPreview, onViewAll = onViewLeaderboard)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StatsHero(stats: GameStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatTile(
                emoji = "🔥",
                value = stats.currentStreak.toString(),
                label = "Day streak",
                accent = GraceGold,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider()
            StatTile(
                emoji = "⭐",
                value = stats.totalPoints.toString(),
                label = "This month",
                accent = GracePurple,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider()
            StatTile(
                emoji = "🏆",
                value = stats.longestStreak.toString(),
                label = "Best streak",
                accent = GraceGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatTile(
    emoji: String, value: String, label: String,
    accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(
            label.uppercase(), color = GraceCreamDim, fontSize = 8.sp,
            letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .size(width = 1.dp, height = 44.dp)
            .background(GraceCreamDim.copy(alpha = 0.2f))
    )
}

@Composable
private fun DifficultyCard(
    difficulty: com.grace.app.domain.model.GameDifficulty,
    stats: GameStats,
    onPlay: () -> Unit
) {
    val available = stats.canPlayDaily(difficulty)
    val (label, emoji, accent) = when (difficulty) {
        com.grace.app.domain.model.GameDifficulty.EASY ->
            Triple("EASY · 10 pts each", "🟢", GraceGreen)
        com.grace.app.domain.model.GameDifficulty.MEDIUM ->
            Triple("MEDIUM · 20 pts each", "🟡", GraceGold)
        com.grace.app.domain.model.GameDifficulty.HARD ->
            Triple("HARD · 30 pts each", "🔴", GracePurple)
    }
    val resolvedAccent = if (available) accent else GraceCreamDim
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = available) { onPlay() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 26.sp)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label, color = resolvedAccent, fontSize = 10.sp,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (available) "Tap to play"
                    else "Locked — comes back ${countdownLabel(stats.lastFor(difficulty))}",
                    color = GraceCream, fontSize = 12.sp
                )
            }
            Text(
                if (available) "Play →" else "🔒",
                color = resolvedAccent, fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DailyVerseCard(stats: GameStats, onPlay: () -> Unit) {
    val available = stats.canPlayFitb()
    val accent = if (available) GraceBlue else GraceCreamDim
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = available) { onPlay() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📖", fontSize = 26.sp)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "DAILY VERSE · 25 PTS",
                    color = accent, fontSize = 10.sp,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (available) "Fill in the blank · reflect on Scripture"
                    else "Locked — comes back ${countdownLabel(stats.lastFitbAt)}",
                    color = GraceCream, fontSize = 12.sp
                )
            }
            Text(
                if (available) "Play →" else "🔒",
                color = accent, fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PracticeCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📖", fontSize = 28.sp)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "PRACTICE · UNLIMITED", color = GraceBlue, fontSize = 10.sp,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "❤️x5 lives · ⏱ 15s per question. Out of lives ends the run.",
                    color = GraceCream, fontSize = 12.sp
                )
            }
            Text("→", color = GraceBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun WhoAmICard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🤔", fontSize = 28.sp)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "WHO AM I? · NEW", color = GracePurple, fontSize = 10.sp,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "🔍 4 clues · up to 40 pts. Guess the Bible character.",
                    color = GraceCream, fontSize = 12.sp
                )
            }
            Text("→", color = GracePurple, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MemoryCardsCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🃏", fontSize = 28.sp)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "MEMORY CARDS · NEW", color = GraceRose, fontSize = 10.sp,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Match reference ↔ verse. 6 pairs · up to 90 pts (perfect-clear bonus).",
                    color = GraceCream, fontSize = 12.sp
                )
            }
            Text("→", color = GraceRose, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun VerseScrambleCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🧩", fontSize = 28.sp)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "VERSE SCRAMBLE · NEW", color = GraceGreen, fontSize = 10.sp,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap words in order. 5 verses · up to 200 pts.",
                    color = GraceCream, fontSize = 12.sp
                )
            }
            Text("→", color = GraceGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TimelineSortCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📜", fontSize = 28.sp)
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "TIMELINE SORT · NEW", color = GraceBlue, fontSize = 10.sp,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Sort Bible events earliest→latest. 3 puzzles · up to 180 pts.",
                    color = GraceCream, fontSize = 12.sp
                )
            }
            Text("→", color = GraceBlue, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LeaderboardPreview(rows: List<LeaderboardEntry>, onViewAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "THIS WEEK · YOUR CELL GROUP", color = GraceCreamDim,
            fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            "See all →",
            color = GraceGold, fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onViewAll() }
        )
    }
    Spacer(Modifier.height(8.dp))
    if (rows.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onViewAll() },
            colors = CardDefaults.cardColors(containerColor = GraceCardBg),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏁", fontSize = 24.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "No scores yet this week.",
                    color = GraceCream, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Play to join the board — or tap to see the Monthly · Global board.",
                    color = GraceCreamDim, fontSize = 11.sp
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEachIndexed { idx, row ->
                LeaderboardRow(rank = idx + 1, row = row)
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, row: LeaderboardEntry) {
    val medal = when (rank) { 1 -> "🥇"; 2 -> "🥈"; 3 -> "🥉"; else -> "  $rank" }
    val nameColor = if (row.isMe) GraceGold else GraceCream
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(medal, fontSize = 16.sp)
            Spacer(Modifier.size(10.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GracePurple.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    row.userName.firstOrNull()?.uppercase() ?: "?",
                    color = GraceCream, fontSize = 13.sp
                )
            }
            Spacer(Modifier.size(10.dp))
            Text(
                row.userName + if (row.isMe) " (you)" else "",
                color = nameColor, fontSize = 14.sp,
                fontWeight = if (row.isMe) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${row.points} pts",
                color = GraceGold, fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun countdownLabel(lastDaily: LocalDateTime?): String {
    if (lastDaily == null) return "in a moment"
    val next = GameStats.nextDailyUnlock(lastDaily)
    val now = LocalDateTime.now()
    if (!next.isAfter(now)) return "now"
    val mins = Duration.between(now, next).toMinutes()
    val h = mins / 60
    val m = mins % 60
    return if (h > 0) "in ${h}h ${m}m" else "in ${m}m"
}
