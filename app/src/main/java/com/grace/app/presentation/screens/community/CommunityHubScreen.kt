package com.grace.app.presentation.screens.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grace.app.presentation.theme.GraceBlue
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceOrange
import com.grace.app.presentation.theme.GracePurple

@Composable
fun CommunityHubScreen(
    onBack: () -> Unit,
    onOpenLifeGroup: () -> Unit,
    onOpenMyContent: () -> Unit,
    onOpenMyAttendance: () -> Unit,
    onOpenMyProgress: () -> Unit,
    onOpenMyJournal: () -> Unit
) {
    val items = listOf(
        HubItem("🏘️", "My Life Group",
            "Your cell + leader contact", GracePurple, onOpenLifeGroup),
        HubItem("📖", "My Content",
            "Devotionals I've completed", GraceBlue, onOpenMyContent),
        HubItem("✅", "My Attendance",
            "Where I've shown up", GraceGreen, onOpenMyAttendance),
        HubItem("📈", "My Progress",
            "Compassion-ready report", GraceGold, onOpenMyProgress),
        HubItem("📝", "My Journal",
            "Private — encrypted on device", GraceOrange, onOpenMyJournal)
    )

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
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Community", color = GraceCream, fontSize = 24.sp)
                Text(
                    "Your faith journey, all in one place.",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items, key = { it.title }) { item -> HubCard(item) }
        }
    }
}

private data class HubItem(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val accent: Color,
    val onTap: () -> Unit
)

@Composable
private fun HubCard(item: HubItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable { item.onTap() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(item.emoji, fontSize = 24.sp)
            Spacer(Modifier.weight(1f))
            Text(
                item.title, color = item.accent,
                fontSize = 14.sp, fontWeight = FontWeight.Bold
            )
            Text(
                item.subtitle, color = GraceCreamDim,
                fontSize = 11.sp, maxLines = 2
            )
        }
    }
}
