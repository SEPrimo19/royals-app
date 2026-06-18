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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import com.grace.app.domain.model.User
import com.grace.app.presentation.components.MenuButtonRow
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GracePurple

@Composable
fun LeaderScreen(
    onOpenMenu: () -> Unit,
    viewModel: LeaderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
    ) {
        MenuButtonRow(onOpenMenu)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("My Leaders 🤝", color = GraceCream, fontSize = 26.sp)
        Text("Real mentorship. Real people.", color = GraceCreamDim, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))

        CheckInCard(state, viewModel)
        Spacer(Modifier.height(16.dp))

        if (state.error != null) {
            Text("⚠ ${state.error}", color = com.grace.app.presentation.theme.GraceRose)
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.allLeaders, key = { it.id }) { leader ->
                LeaderRow(leader)
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
        }
    }
}

@Composable
private fun CheckInCard(state: LeaderUiState, viewModel: LeaderViewModel) {
    val isEditing = state.checkInDone
    Card(
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📋 WEEKLY CHECK-IN", color = GracePurple, fontSize = 11.sp)
            if (isEditing) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "✓ Submitted this week — edit anytime and tap Update.",
                    color = GraceGreen, fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .background(
                                if (i <= state.checkInStep) GracePurple else GraceCreamDim,
                                RoundedCornerShape(50)
                            )
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                viewModel.questions[state.checkInStep],
                color = GraceCream,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = state.checkInAnswers[state.checkInStep],
                onValueChange = {
                    viewModel.onEvent(
                        LeaderEvent.CheckInAnswerChanged(state.checkInStep, it)
                    )
                },
                modifier = Modifier.fillMaxWidth().height(90.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GracePurple,
                    unfocusedBorderColor = GraceCreamDim,
                    focusedTextColor = GraceCream,
                    unfocusedTextColor = GraceCream,
                    cursorColor = GracePurple
                )
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = { viewModel.onEvent(LeaderEvent.PreviousCheckInStep) },
                    enabled = state.checkInStep > 0
                ) { Text("Back", color = GraceCreamDim) }

                if (state.checkInStep < 2) {
                    TextButton(
                        onClick = { viewModel.onEvent(LeaderEvent.NextCheckInStep) },
                        enabled = state.checkInAnswers[state.checkInStep].isNotBlank()
                    ) { Text("Next", color = GracePurple) }
                } else {
                    val allAnswered = state.checkInAnswers.all { it.isNotBlank() }
                    TextButton(
                        onClick = { viewModel.onEvent(LeaderEvent.SubmitCheckIn) },
                        enabled = !state.isSubmittingCheckIn && allAnswered
                    ) {
                        Text(
                            if (isEditing) "Update ✓" else "Submit ✓",
                            color = GraceGreen
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderRow(leader: User) {
    val context = LocalContext.current
    val msg = leader.messengerUrl
    val canMessage = leader.messengerPublic && !msg.isNullOrBlank()
    Card(
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(GracePurple.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(leader.name.firstOrNull()?.uppercase() ?: "?", color = GraceCream)
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(leader.name, color = GraceCream, fontSize = 15.sp)
                Text(
                    leader.role.name.lowercase().replace('_', ' ')
                        .replaceFirstChar { it.uppercase() },
                    color = GraceCreamDim,
                    fontSize = 11.sp
                )
            }
            if (canMessage) {
                Text(
                    "💬 Messenger",
                    color = GraceDeepBlue, fontSize = 12.sp,
                    modifier = Modifier
                        .background(GracePurple, RoundedCornerShape(10.dp))
                        .clickable {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, msg!!.toUri())
                                )
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            } else {
                Text(
                    "No Messenger",
                    color = GraceCreamDim, fontSize = 11.sp,
                    modifier = Modifier
                        .background(
                            GraceCardBg,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
