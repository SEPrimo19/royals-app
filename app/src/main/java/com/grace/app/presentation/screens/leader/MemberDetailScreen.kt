package com.grace.app.presentation.screens.leader

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import com.grace.app.domain.model.CheckIn
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GracePurple
import com.grace.app.presentation.theme.GraceRose
import java.time.format.DateTimeFormatter

private val checkInQuestionLabels = mapOf(
    "q1" to "How's your faith walk this week? (1 = struggling, 5 = thriving)",
    "q2" to "What's your biggest struggle right now?",
    "q3" to "What can you specifically pray for this week?"
)

@Composable
fun MemberDetailScreen(
    onBack: () -> Unit,
    onOpenReflections: (memberId: String) -> Unit,
    onPostPrayerOnBehalf: (memberId: String) -> Unit = {},
    onLogReflectionOnBehalf: (memberId: String) -> Unit = {},
    onGenerateReport: (memberId: String) -> Unit = {},
    viewModel: MemberDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")

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
            Text("Member", color = GraceCream, fontSize = 22.sp)
        }
        Spacer(Modifier.height(16.dp))

        when {
            state.isLoading ->
                Box(Modifier.fillMaxWidth().height(220.dp), Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }
            state.mentee == null ->
                Text("⚠ Couldn't load this member.", color = GraceRose)
            else -> {
                val m = state.mentee!!
                HeaderCard(
                    name = m.user.name.ifBlank { m.user.email },
                    email = m.user.email,
                    initial = m.user.name.firstOrNull()?.uppercase() ?: "?"
                )
                Spacer(Modifier.height(14.dp))
                val msg = m.user.messengerUrl
                val canMessage = m.user.messengerPublic && !msg.isNullOrBlank()
                if (canMessage) {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, msg!!.toUri())
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "💬  Open Messenger",
                            color = GraceDeepBlue, fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        "This member hasn't shared a Messenger link.",
                        color = GraceCreamDim, fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                CheckInCard(state.latestCheckIn, dateFmt)
                Spacer(Modifier.height(14.dp))
                ReflectionsEntryCard(
                    count = state.reflections.size,
                    isLoading = state.isLoadingReflections,
                    onTap = { onOpenReflections(m.user.id) }
                )
                Spacer(Modifier.height(14.dp))
                ProxyActionsRow(
                    onPostPrayer = { onPostPrayerOnBehalf(m.user.id) },
                    onLogReflection = { onLogReflectionOnBehalf(m.user.id) }
                )
                Spacer(Modifier.height(14.dp))
                ReportEntryCard(
                    isCompassion = m.user.isCompassion,
                    onTap = { onGenerateReport(m.user.id) }
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun ReportEntryCard(
    isCompassion: Boolean,
    onTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "📄  COMPLIANCE REPORT",
                    color = GraceGold, fontSize = 10.sp,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (isCompassion)
                        "Generate the monthly Compassion PDF for this member"
                    else
                        "Generate a progress report for this member",
                    color = GraceCream, fontSize = 14.sp
                )
            }
            Text("›", color = GraceGold, fontSize = 22.sp,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProxyActionsRow(
    onPostPrayer: () -> Unit,
    onLogReflection: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ProxyActionCard(
            emoji = "🙏",
            title = "Post Prayer",
            subtitle = "On their behalf",
            accent = com.grace.app.presentation.theme.GraceBlue,
            modifier = Modifier.weight(1f),
            onTap = onPostPrayer
        )
        ProxyActionCard(
            emoji = "📝",
            title = "Log Reflection",
            subtitle = "From paper journal",
            accent = com.grace.app.presentation.theme.GraceOrange,
            modifier = Modifier.weight(1f),
            onTap = onLogReflection
        )
    }
}

@Composable
private fun ProxyActionCard(
    emoji: String,
    title: String,
    subtitle: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onTap() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(title, color = accent, fontSize = 13.sp,
                fontWeight = FontWeight.Bold)
            Text(subtitle, color = GraceCreamDim, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ReflectionsEntryCard(
    count: Int,
    isLoading: Boolean,
    onTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "📅  WEEKLY MEDITATION REFLECTIONS",
                    color = GraceGold, fontSize = 10.sp,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    when {
                        isLoading -> "Loading…"
                        count == 0 -> "No reflections yet"
                        count == 1 -> "1 reflection — tap to view"
                        else -> "$count reflections — tap to view"
                    },
                    color = GraceCream, fontSize = 14.sp
                )
            }
            Text("›", color = GraceGold, fontSize = 22.sp,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun HeaderCard(name: String, email: String, initial: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(GracePurple.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, color = GraceCream, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = GraceCream, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
                Text(email, color = GraceCreamDim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun CheckInCard(checkIn: CheckIn?, fmt: DateTimeFormatter) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "📋  LATEST WEEKLY CHECK-IN",
                    color = GracePurple, fontSize = 10.sp,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (checkIn != null) {
                    Text(
                        checkIn.submittedAt.format(fmt),
                        color = GraceGreen, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (checkIn == null) {
                Text(
                    "This member hasn't submitted a check-in yet.",
                    color = GraceCreamDim, fontSize = 13.sp
                )
            } else {
                checkInQuestionLabels.forEach { (key, question) ->
                    val answer = checkIn.answers[key].orEmpty()
                    Spacer(Modifier.height(6.dp))
                    Text(question, color = GraceCreamDim, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (answer.isBlank()) "—" else answer,
                        color = GraceCream, fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
