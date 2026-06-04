package com.grace.app.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceRose
import kotlinx.coroutines.flow.collectLatest

/**
 * Shown on first signin if a proxy-only user row matches the new user's
 * email. They can confirm to inherit the history their cell leader has
 * been building up, or dismiss if it isn't them.
 *
 * [onDone] fires when the screen has nothing more to show — either the
 * user dismissed, the claim succeeded, or no proxy was found in the
 * first place. The host (NavGraph / MainActivity) routes to Home.
 */
@Composable
fun ClaimRecordScreen(
    onDone: () -> Unit,
    viewModel: ClaimRecordViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { fx ->
            when (fx) {
                ClaimRecordEffect.Done -> onDone()
                ClaimRecordEffect.Claimed -> {
                    // Short delay so the user sees the "✓ Claimed" toast
                    // before the screen pops.
                    kotlinx.coroutines.delay(1500)
                    onDone()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue),
        contentAlignment = Alignment.Center
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(color = GraceGold)
            state.proxy == null -> CircularProgressIndicator(color = GraceGold)
            state.claimSuccess -> SuccessCard(name = state.proxy?.name.orEmpty())
            else -> ClaimPromptCard(
                state = state,
                onConfirm = { viewModel.onEvent(ClaimRecordEvent.ConfirmClaim) },
                onDismiss = { viewModel.onEvent(ClaimRecordEvent.Dismiss) }
            )
        }
    }
}

@Composable
private fun ClaimPromptCard(
    state: ClaimRecordUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val proxy = state.proxy ?: return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("👋", fontSize = 44.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "We found your record",
                color = GraceCream, fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your cell leader registered you here before you had the app. " +
                    "If that's you, claim your record and your attendance + " +
                    "reflections will move into your new account.",
                color = GraceCreamDim, fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            if (proxy.name.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = GraceGold.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Registered name",
                            color = GraceCreamDim, fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            proxy.name,
                            color = GraceCream, fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (proxy.isCompassion &&
                            !proxy.compassionNumber.isNullOrBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Compassion ID: ${proxy.compassionNumber}",
                                color = GraceGold, fontSize = 12.sp,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "⚠ ${state.error}",
                    color = GraceRose, fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onConfirm,
                enabled = !state.isClaiming,
                colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    if (state.isClaiming) "Claiming…" else "Yes, that's me",
                    color = GraceDeepBlue, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDismiss,
                enabled = !state.isClaiming,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("No, this isn't me", color = GraceCreamDim)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "If unsure, tap \"this isn't me\" — your cell leader can sort " +
                    "it out and you can claim later.",
                color = GraceCreamDim, fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SuccessCard(name: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🎉", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Welcome back" + if (name.isNotBlank()) ", ${name.split(' ').first()}!" else "!",
                color = GraceGreen, fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your history is now in your account.",
                color = GraceCreamDim, fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
