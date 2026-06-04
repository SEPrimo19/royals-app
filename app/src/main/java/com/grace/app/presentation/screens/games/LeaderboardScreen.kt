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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.LeaderboardEntry
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GracePurple
import com.grace.app.presentation.theme.GraceRose

@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←", color = GraceCream, fontSize = 22.sp,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
            )
            Column {
                Text("Leaderboard 🏆", color = GraceCream, fontSize = 24.sp)
                Text(
                    when (state.period) {
                        LeaderboardPeriod.WEEKLY_CELL ->
                            "Your cell group · This week · Daily only"
                        LeaderboardPeriod.MONTHLY_GLOBAL ->
                            "All players · This month · Daily + Practice"
                    },
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        // Period toggle — weekly cell vs monthly global.
        PeriodTabs(
            current = state.period,
            onSelect = { viewModel.selectPeriod(it) }
        )
        Spacer(Modifier.height(14.dp))

        when {
            state.isLoading ->
                Box(Modifier.fillMaxWidth().height(220.dp), Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }
            state.showNoGroupCard ->
                NoGroupCard()
            state.error != null && state.rows.isEmpty() ->
                Text("⚠ ${state.error}", color = GraceRose)
            state.rows.isEmpty() ->
                EmptyCard(state.period)
            else -> {
                Podium(state.podium, state.period)
                Spacer(Modifier.height(14.dp))
                if (state.rest.isNotEmpty()) {
                    Text(
                        when (state.period) {
                            LeaderboardPeriod.WEEKLY_CELL -> "REST OF THE WEEK"
                            LeaderboardPeriod.MONTHLY_GLOBAL -> "REST OF THE MONTH"
                        },
                        color = GraceCreamDim, fontSize = 10.sp,
                        letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.rest, key = { it.userId }) { row ->
                            RowItem(
                                rank = state.rows.indexOf(row) + 1,
                                row = row,
                                showGroupChip = state.period == LeaderboardPeriod.MONTHLY_GLOBAL
                            )
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
                if (!state.isUserInList) {
                    Spacer(Modifier.height(12.dp))
                    YouAreOffTheBoardHint(state.period)
                }
            }
        }
    }
}

@Composable
private fun PeriodTabs(
    current: LeaderboardPeriod,
    onSelect: (LeaderboardPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GraceCardBg, RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TabChip(
            label = "Weekly · Cell",
            selected = current == LeaderboardPeriod.WEEKLY_CELL,
            onClick = { onSelect(LeaderboardPeriod.WEEKLY_CELL) },
            modifier = Modifier.weight(1f)
        )
        TabChip(
            label = "Monthly · Global",
            selected = current == LeaderboardPeriod.MONTHLY_GLOBAL,
            onClick = { onSelect(LeaderboardPeriod.MONTHLY_GLOBAL) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                if (selected) GraceGold else Color.Transparent,
                RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) GraceDeepBlue else GraceCreamDim,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Podium(top: List<LeaderboardEntry>, period: LeaderboardPeriod) {
    val first = top.getOrNull(0)
    val second = top.getOrNull(1)
    val third = top.getOrNull(2)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (second != null) {
            PodiumColumn(
                rank = 2, row = second,
                medal = "🥈", accent = GraceCreamDim,
                tier = 120.dp, modifier = Modifier.weight(1f),
                showGroupChip = period == LeaderboardPeriod.MONTHLY_GLOBAL
            )
        } else Spacer(Modifier.weight(1f))
        if (first != null) {
            PodiumColumn(
                rank = 1, row = first,
                medal = "🥇", accent = GraceGold,
                tier = 150.dp, modifier = Modifier.weight(1f),
                showGroupChip = period == LeaderboardPeriod.MONTHLY_GLOBAL
            )
        } else Spacer(Modifier.weight(1f))
        if (third != null) {
            PodiumColumn(
                rank = 3, row = third,
                medal = "🥉", accent = GraceOrangeOrPurple,
                tier = 100.dp, modifier = Modifier.weight(1f),
                showGroupChip = period == LeaderboardPeriod.MONTHLY_GLOBAL
            )
        } else Spacer(Modifier.weight(1f))
    }
}

// Composable getter so the underlying GracePurple (now palette-driven)
// resolves at call time rather than at class-init time.
private val GraceOrangeOrPurple: Color
    @Composable @androidx.compose.runtime.ReadOnlyComposable
    get() = GracePurple

@Composable
private fun PodiumColumn(
    rank: Int,
    row: LeaderboardEntry,
    medal: String,
    accent: Color,
    tier: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    showGroupChip: Boolean = false
) {
    val nameColor = if (row.isMe) GraceGold else GraceCream
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(medal, fontSize = 30.sp)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                row.userName.firstOrNull()?.uppercase() ?: "?",
                color = GraceCream, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            row.userName + if (row.isMe) " (you)" else "",
            color = nameColor, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        if (showGroupChip && !row.groupName.isNullOrBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                row.groupName,
                color = GraceCreamDim, fontSize = 9.sp,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(6.dp))
        // Tier height varies by rank to give a podium feel.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(tier)
                .background(
                    accent.copy(alpha = 0.18f),
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text(
                    "${row.points}",
                    color = accent, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "PTS", color = GraceCreamDim, fontSize = 9.sp,
                    letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RowItem(rank: Int, row: LeaderboardEntry, showGroupChip: Boolean) {
    val nameColor = if (row.isMe) GraceGold else GraceCream
    val bg = if (row.isMe) GraceGold.copy(alpha = 0.12f) else GraceCardBg
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "#$rank",
                color = GraceCreamDim, fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.size(32.dp).padding(top = 6.dp)
            )
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    row.userName + if (row.isMe) " (you)" else "",
                    color = nameColor, fontSize = 14.sp,
                    fontWeight = if (row.isMe) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
                if (showGroupChip && !row.groupName.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                GraceMuted.copy(alpha = 0.4f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            row.groupName,
                            color = GraceCreamDim, fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
            Text(
                "${row.points} pts",
                color = GraceGold, fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NoGroupCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🤝", fontSize = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Join a cell group to compete weekly",
                color = GraceCream, fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Your weekly leaderboard runs against your cell group. " +
                    "Switch to Monthly · Global to see all players.",
                color = GraceCreamDim, fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun EmptyCard(period: LeaderboardPeriod) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🏁", fontSize = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                when (period) {
                    LeaderboardPeriod.WEEKLY_CELL -> "No scores yet this week"
                    LeaderboardPeriod.MONTHLY_GLOBAL -> "No scores yet this month"
                },
                color = GraceCream, fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when (period) {
                    LeaderboardPeriod.WEEKLY_CELL ->
                        "Play today's Daily Challenge to be the first on the board!"
                    LeaderboardPeriod.MONTHLY_GLOBAL ->
                        "Play Daily or Practice to be the first on the global board!"
                },
                color = GraceCreamDim, fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun YouAreOffTheBoardHint(period: LeaderboardPeriod) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            when (period) {
                LeaderboardPeriod.WEEKLY_CELL ->
                    "Not on the board yet this week — play any Daily to join."
                LeaderboardPeriod.MONTHLY_GLOBAL ->
                    "Not on the board yet this month — play Daily or Practice to join."
            },
            color = GraceCreamDim, fontSize = 12.sp,
            modifier = Modifier.padding(14.dp)
        )
    }
}
