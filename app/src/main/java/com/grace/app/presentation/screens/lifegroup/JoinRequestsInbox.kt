package com.grace.app.presentation.screens.lifegroup

import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.IncomingJoinRequest
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GraceRose

@Composable
fun JoinRequestsInbox(
    groupId: String,
    viewModel: JoinRequestsInboxViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var toast by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    if (toast != null) {
        LaunchedEffect(toast) {
            kotlinx.coroutines.delay(2500)
            toast = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            when (fx) {
                is JoinInboxEffect.Toast -> toast = fx.message to fx.isError
            }
        }
    }

    val pending = state.requests.filter { it.groupId == groupId }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "JOIN REQUESTS",
                color = GraceCreamDim, fontSize = 10.sp,
                letterSpacing = 3.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            if (pending.isNotEmpty()) {
                Text(
                    pending.size.toString(),
                    color = GraceGold, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(GraceGold.copy(alpha = 0.18f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.size(8.dp))

        if (toast != null) {
            Text(
                (if (toast!!.second) "⚠ " else "✓ ") + toast!!.first,
                color = if (toast!!.second) GraceRose else GraceGreen,
                fontSize = 12.sp
            )
            Spacer(Modifier.size(6.dp))
        }

        if (state.isLoading) {
            Text("Loading…", color = GraceCreamDim, fontSize = 12.sp)
        } else if (pending.isEmpty()) {
            Text(
                "No pending requests.",
                color = GraceCreamDim, fontSize = 12.sp
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pending.forEach { r ->
                    RequestRow(
                        request = r,
                        isWorking = state.workingId == r.id,
                        onApprove = { viewModel.onEvent(JoinInboxEvent.Approve(r.id)) },
                        onReject = { viewModel.onEvent(JoinInboxEvent.StartReject(r)) }
                    )
                }
            }
        }

        state.rejecting?.let { target ->
            var note by remember(target.id) { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { viewModel.onEvent(JoinInboxEvent.CancelReject) },
                title = { Text("Decline ${target.userName}'s request?") },
                text = {
                    Column {
                        Text(
                            "They'll get a soft notification and can request again " +
                                "in 7 days. You can leave an optional note for them.",
                            color = GraceCreamDim, fontSize = 12.sp
                        )
                        Spacer(Modifier.size(10.dp))
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it.take(280) },
                            placeholder = {
                                Text(
                                    "Optional note (max 280 chars)",
                                    color = GraceCreamDim, fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.onEvent(JoinInboxEvent.ConfirmReject(note))
                    }) { Text("Decline", color = GraceRose) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        viewModel.onEvent(JoinInboxEvent.CancelReject)
                    }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
private fun RequestRow(
    request: IncomingJoinRequest,
    isWorking: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(36.dp)
                        .background(GraceMuted.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        request.userName.firstOrNull()?.uppercase() ?: "?",
                        color = GraceCream, fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            request.userName, color = GraceCream, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (request.userIsCompassion) {
                            Spacer(Modifier.size(6.dp))
                            Text(
                                "CP", color = GraceGold, fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        GraceGold.copy(alpha = 0.18f),
                                        RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        buildString {
                            if (!request.userCurrentGroup.isNullOrBlank()) {
                                append("Currently in ${request.userCurrentGroup}")
                            } else {
                                append("Not in a cell yet")
                            }
                        },
                        color = GraceCreamDim, fontSize = 11.sp
                    )
                }
            }
            Spacer(Modifier.size(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionBtn(
                    label = "Reject",
                    color = GraceRose,
                    enabled = !isWorking,
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                )
                ActionBtn(
                    label = if (isWorking) "…" else "Approve",
                    color = GraceGreen,
                    enabled = !isWorking,
                    onClick = onApprove,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ActionBtn(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color.copy(alpha = if (enabled) 0.18f else 0.06f),
                RoundedCornerShape(50)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = color.copy(alpha = if (enabled) 1f else 0.4f),
            fontSize = 12.sp, fontWeight = FontWeight.Bold
        )
    }
}
