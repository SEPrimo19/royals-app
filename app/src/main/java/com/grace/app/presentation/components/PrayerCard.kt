package com.grace.app.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grace.app.domain.model.Prayer
import com.grace.app.domain.model.PrayerCategory
import com.grace.app.domain.model.PrayerStatus
import com.grace.app.presentation.theme.GraceBlue
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GraceOrange
import com.grace.app.presentation.theme.GracePurple
import com.grace.app.presentation.theme.GraceRose
import java.time.Duration
import java.time.LocalDateTime

@androidx.compose.runtime.Composable
fun categoryColor(category: PrayerCategory): Color = when (category) {
    PrayerCategory.FAMILY -> GraceBlue
    PrayerCategory.SCHOOL -> GracePurple
    PrayerCategory.FAITH -> GraceGold
    PrayerCategory.HEALTH -> GraceGreen
    PrayerCategory.PERSONAL -> GraceRose
    PrayerCategory.NATIONS -> GraceOrange
}

private fun timeAgo(time: LocalDateTime): String {
    val d = Duration.between(time, LocalDateTime.now())
    return when {
        d.toMinutes() < 1 -> "just now"
        d.toMinutes() < 60 -> "${d.toMinutes()}m ago"
        d.toHours() < 24 -> "${d.toHours()}h ago"
        else -> "${d.toDays()}d ago"
    }
}

@Composable
fun PrayerCard(
    prayer: Prayer,
    realtimePrayCount: Int,
    hasUserPrayed: Boolean,
    isOwnPrayer: Boolean,
    onPrayTap: () -> Unit,
    onMarkAnswered: () -> Unit
) {
    val answered = prayer.status == PrayerStatus.ANSWERED
    val catColor = categoryColor(prayer.category)
    val pulse by animateFloatAsState(
        targetValue = if (hasUserPrayed) 1.06f else 1f,
        label = "pray_pulse"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (answered) GraceGreen.copy(alpha = 0.12f) else GraceCardBg
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (prayer.isAnonymous) GraceMuted else catColor.copy(alpha = 0.3f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (prayer.isAnonymous) "🕊️"
                        else (prayer.userName?.firstOrNull()?.uppercase() ?: "?"),
                        color = GraceCream
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (prayer.isAnonymous) "A Youth in Prayer"
                        else (prayer.userName ?: "A Youth in Prayer"),
                        color = GraceCream,
                        fontSize = 14.sp
                    )
                    if (prayer.postedByProxy != null) {
                        Text(
                            "via ${prayer.proxyLeaderName ?: "a leader"}",
                            color = GraceCreamDim,
                            fontSize = 10.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    Text(timeAgo(prayer.createdAt), color = GraceCreamDim, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .background(catColor.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        prayer.category.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = catColor,
                        fontSize = 10.sp
                    )
                }
            }

            if (answered) {
                Spacer(Modifier.size(8.dp))
                Text("ANSWERED ✨", color = GraceGreen, fontSize = 11.sp)
            }

            Spacer(Modifier.size(12.dp))
            Text(
                prayer.content,
                color = GraceCreamDim,
                fontSize = 16.sp,
                lineHeight = 26.sp,
                fontStyle = FontStyle.Italic
            )

            Spacer(Modifier.size(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .scale(pulse)
                        .background(
                            if (hasUserPrayed) GraceBlue.copy(alpha = 0.2f) else GraceCardBg,
                            RoundedCornerShape(50)
                        )
                        .border(
                            width = if (hasUserPrayed) 1.dp else 0.dp,
                            color = if (hasUserPrayed) GraceBlue else Color.Transparent,
                            shape = RoundedCornerShape(50)
                        )
                        .clickable { onPrayTap() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "🙏 ${realtimePrayCount}",
                        color = if (hasUserPrayed) GraceBlue else GraceCreamDim,
                        fontSize = 14.sp
                    )
                }
                if (isOwnPrayer && !answered) {
                    TextButton(onClick = onMarkAnswered) {
                        Text("Mark as Answered 🙏", color = GraceGreen, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
