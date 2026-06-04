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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.Mentee
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
fun MyMembersScreen(
    onBack: () -> Unit,
    onOpenMember: (String) -> Unit,
    onAddProxyMember: () -> Unit,
    viewModel: MyMembersViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val withCheckIns = state.mentees.count { it.lastCheckInAt != null }

    // Re-fetch on every screen entry so a freshly-added proxy member shows up
    // immediately after the form pops back here.
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.refresh() }

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
            Column(modifier = Modifier.weight(1f)) {
                Text("My Members 🤝", color = GraceCream, fontSize = 24.sp)
                Text(
                    "${state.mentees.size} member" +
                        (if (state.mentees.size == 1) "" else "s") +
                        " · $withCheckIns checked in",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
            // + Add — opens the proxy-member registration form.
            Text(
                "+ Add",
                color = GraceGold, fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(
                        GraceGold.copy(alpha = 0.15f),
                        RoundedCornerShape(50)
                    )
                    .clickable { onAddProxyMember() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(16.dp))

        when {
            state.isLoading && state.mentees.isEmpty() ->
                Box(Modifier.fillMaxWidth().height(220.dp), Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }
            state.error != null && state.mentees.isEmpty() ->
                Text("⚠ ${state.error}", color = GraceRose)
            state.mentees.isEmpty() ->
                EmptyState()
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.mentees, key = { it.user.id }) { mentee ->
                        MenteeRow(mentee, onClick = { onOpenMember(mentee.user.id) })
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MenteeRow(mentee: Mentee, onClick: () -> Unit) {
    val initial = mentee.user.name.firstOrNull()?.uppercase() ?: "?"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(GracePurple.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, color = GraceCream, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        mentee.user.name.ifBlank { mentee.user.email },
                        color = GraceCream, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (mentee.user.isProxyOnly) {
                        Spacer(Modifier.size(6.dp))
                        // Subtle "no app" badge so leaders can scan the list
                        // and know which members they need to act for.
                        Text(
                            "NO APP",
                            color = GraceCreamDim,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    GraceCreamDim.copy(alpha = 0.18f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.size(2.dp))
                // Proxy members often have no email — fall back to a friendly
                // line about their cell / leader proxy status.
                val subtitle = if (mentee.user.email.isNotBlank()) {
                    mentee.user.email
                } else {
                    "Registered by leader — no app account"
                }
                Text(subtitle, color = GraceCreamDim, fontSize = 11.sp)
                if (mentee.lastCheckInAt != null) {
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "📋 Check-in ${relativeTime(mentee.lastCheckInAt)}",
                        color = GraceGreen, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text("›", color = GraceGold, fontSize = 22.sp)
        }
    }
}

@Composable
private fun EmptyState() {
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
            Text("No members yet", color = GraceCream, fontSize = 16.sp,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Members of your group will appear here once they're assigned to you.",
                color = GraceCreamDim, fontSize = 12.sp
            )
        }
    }
}

private fun relativeTime(t: LocalDateTime): String {
    val now = LocalDateTime.now()
    val mins = Duration.between(t, now).toMinutes()
    return when {
        mins < 1 -> "just now"
        mins < 60 -> "${mins}m ago"
        mins < 60 * 24 -> "${mins / 60}h ago"
        mins < 60 * 24 * 7 -> "${mins / (60 * 24)}d ago"
        else -> "${mins / (60 * 24 * 7)}w ago"
    }
}
