package com.grace.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.presentation.theme.GraceCardAlt
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GraceRose

@Composable
fun DailyChallengeCard(
    onOpenLibrary: () -> Unit = {},
    viewModel: DailyChallengeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var toast by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    if (toast != null) {
        LaunchedEffect(toast) {
            kotlinx.coroutines.delay(2500)
            toast = null
        }
    }

    LaunchedEffect(Unit) { viewModel.onEvent(DailyChallengeEvent.Refresh) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            when (fx) {
                is DailyChallengeEffect.Toast -> toast = fx.message to fx.isError
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenLibrary() },
        colors = CardDefaults.cardColors(containerColor = GraceCardAlt),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "TODAY'S CHALLENGE · DISCIPLESHIP",
                    color = GraceGold, fontSize = 10.sp,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (state.streak > 0) {
                    Text(
                        "🌱 ${state.streak}",
                        color = GraceGreen, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                GraceGreen.copy(alpha = 0.18f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            when {
                state.isLoading -> {
                    Box(
                        Modifier.fillMaxWidth().height(72.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = GraceGold) }
                }
                state.activity == null -> {
                    Text(
                        "No activities yet.",
                        color = GraceCream, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Your leader will add activities here. Until then, " +
                            "keep up your devotional and prayer rhythms.",
                        color = GraceCreamDim, fontSize = 12.sp
                    )
                }
                else -> {
                    val a = state.activity!!
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(a.category.emoji, fontSize = 22.sp)
                        Spacer(Modifier.padding(start = 8.dp))
                        Text(
                            a.category.label.uppercase(),
                            color = GraceCreamDim, fontSize = 9.sp,
                            letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            a.durationTag.label,
                            color = GraceCreamDim, fontSize = 10.sp,
                            modifier = Modifier
                                .background(
                                    GraceMuted.copy(alpha = 0.4f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        a.title, color = GraceCream, fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        a.description, color = GraceCreamDim, fontSize = 13.sp,
                        lineHeight = 20.sp
                    )

                    Spacer(Modifier.height(12.dp))
                    if (state.cellCount > 0) {
                        Text(
                            "🌱 ${state.cellCount} in your cell did their challenge today",
                            color = GraceGreen, fontSize = 11.sp,
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    if (toast != null) {
                        Text(
                            (if (toast!!.second) "⚠ " else "✓ ") + toast!!.first,
                            color = if (toast!!.second) GraceRose else GraceGreen,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.isCompletedToday) {
                            Text(
                                "✓ Done today",
                                color = GraceGreen, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        GraceGreen.copy(alpha = 0.18f),
                                        RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                            Spacer(Modifier.weight(1f))
                        } else {
                            Text(
                                "Mark done ✓",
                                color = GraceDeepBlue, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(GraceGold, RoundedCornerShape(50))
                                    .clickable(enabled = !state.isWorking) {
                                        viewModel.onEvent(DailyChallengeEvent.OpenComposer)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                            Text(
                                "Pick another →",
                                color = GraceCreamDim, fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(GraceCardBg, RoundedCornerShape(50))
                                    .clickable(enabled = !state.isWorking) {
                                        viewModel.onEvent(DailyChallengeEvent.Swap)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.composerOpen) {
        ReflectionDialog(
            onDismiss = { viewModel.onEvent(DailyChallengeEvent.CloseComposer) },
            onConfirm = { viewModel.onEvent(DailyChallengeEvent.MarkDone(it)) }
        )
    }
}

@Composable
private fun ReflectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reflection by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark today's challenge done") },
        text = {
            Column {
                Text(
                    "Optional: leave a short reflection (private to you).",
                    color = GraceCreamDim, fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = reflection,
                    onValueChange = { reflection = it.take(280) },
                    placeholder = {
                        Text("What stuck with you?", color = GraceCreamDim, fontSize = 13.sp)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reflection) }) {
                Text("Mark done", color = GraceGold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
