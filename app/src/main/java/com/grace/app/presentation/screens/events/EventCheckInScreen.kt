package com.grace.app.presentation.screens.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.AttendanceStatus
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceOrange
import com.grace.app.presentation.theme.GraceRose
import java.time.format.DateTimeFormatter

@Composable
fun EventCheckInScreen(
    onDone: () -> Unit,
    viewModel: EventCheckInViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFmt = androidx.compose.runtime.remember {
        DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            state.isLoading ->
                CircularProgressIndicator(color = GraceGold)

            state.isDone -> {
                val isLate = state.resultStatus == AttendanceStatus.LATE
                Text(
                    if (isLate) "⏱" else "✓",
                    fontSize = 64.sp,
                    color = if (isLate) GraceOrange else GraceGreen
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isLate) "Checked in — a little late"
                    else "You're checked in!",
                    color = GraceCream, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (isLate && state.resultLateMinutes > 0) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Marked late by ${formatLate(state.resultLateMinutes)}.",
                        color = GraceOrange, fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                state.event?.let { e ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        e.title,
                        color = GraceGold, fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(containerColor = GraceGold)
                ) { Text("Back to Events", color = GraceDeepBlue) }
            }

            state.event == null -> {
                Text("⚠", fontSize = 56.sp, color = GraceRose)
                Spacer(Modifier.height(12.dp))
                Text(
                    state.error ?: "Event not found.",
                    color = GraceCream, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                TextButton(onClick = onDone) {
                    Text("Back to Events", color = GraceGold)
                }
            }

            else -> {
                Text("Confirm check-in", color = GraceCream, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GraceCardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(state.event!!.title, color = GraceCream, fontSize = 18.sp,
                            fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "📆 ${state.event!!.eventDate.format(dateFmt)}",
                            color = GraceGold, fontSize = 12.sp
                        )
                        if (!state.event!!.location.isNullOrBlank()) {
                            Text(
                                "📍 ${state.event!!.location}",
                                color = GraceCreamDim, fontSize = 12.sp
                            )
                        }
                    }
                }
                if (state.error != null) {
                    Spacer(Modifier.height(12.dp))
                    Text("⚠ ${state.error}", color = GraceRose,
                        textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.confirm() },
                    enabled = !state.isCheckingIn,
                    colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isCheckingIn) CircularProgressIndicator(
                        color = GraceDeepBlue, modifier = Modifier.size(18.dp)
                    ) else Text(
                        "I'm here ✓",
                        color = GraceDeepBlue, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDone) {
                    Text("Cancel", color = GraceCreamDim)
                }
            }
        }
    }

    if (state.isDone) {
        Box(modifier = Modifier.fillMaxWidth().height(0.dp)) {
            // No-op; placeholder for future Lottie confetti animation.
        }
    }
}

// "23 min" up to an hour; "1h 12m" past that. Server gives whole minutes.
private fun formatLate(minutes: Int): String {
    if (minutes < 60) return "$minutes min"
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) "${hours}h" else "${hours}h ${remainder}m"
}
