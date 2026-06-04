package com.grace.app.presentation.screens.games

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grace.app.domain.model.LifelinesState
import com.grace.app.presentation.theme.GraceBlue
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceRose

/**
 * Shared Lifelines row used by Trivia + Who Am I?. Two pill chips:
 *   🛡️ JOSHUA  (n)    — freeze active Practice timer
 *   🕯️ DANIEL  (n)    — 50/50, eliminate 2 wrong MCQ options
 *
 * [showJoshua] is false for screens with no timer (Daily Trivia, Who Am I?).
 * Each chip dims when its count is 0. Tap → callback. Errors surface in a
 * compact red text under the bar, dismissable.
 */
@Composable
fun LifelinesBar(
    lifelines: LifelinesState,
    showJoshua: Boolean,
    joshuaActiveThisQ: Boolean,
    danielUsedThisQ: Boolean,
    lifelineError: String?,
    onUseJoshua: () -> Unit,
    onUseDaniel: () -> Unit,
    onDismissError: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showJoshua) {
                LifelinePill(
                    emoji = "🛡️",
                    label = "Joshua",
                    count = lifelines.joshuaRemaining,
                    alreadyUsed = joshuaActiveThisQ,
                    accent = GraceBlue,
                    onClick = onUseJoshua,
                    modifier = Modifier.weight(1f)
                )
            }
            LifelinePill(
                emoji = "🕯️",
                label = "Daniel 50/50",
                count = lifelines.danielRemaining,
                alreadyUsed = danielUsedThisQ,
                accent = GraceGold,
                onClick = onUseDaniel,
                modifier = Modifier.weight(1f)
            )
        }
        if (lifelineError != null) {
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GraceRose.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .clickable { onDismissError() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    "⚠ $lifelineError · tap to dismiss",
                    color = GraceRose, fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun LifelinePill(
    emoji: String,
    label: String,
    count: Int,
    alreadyUsed: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // "alreadyUsed" disables the chip after it's been spent on the current
    // question (e.g. Joshua already frozen, Daniel already 50/50'd). The
    // server-side balance is the source of truth for the count, but per-Q
    // single-use is a client guard.
    val disabled = count <= 0 || alreadyUsed
    val pillAlpha = if (disabled) 0.35f else 1f
    Row(
        modifier = modifier
            .height(36.dp)
            .alpha(pillAlpha)
            .background(GraceCardBg, RoundedCornerShape(50))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(50))
            .clickable(enabled = !disabled) { onClick() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(emoji, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            label,
            color = accent, fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "×$count",
            color = GraceCreamDim, fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
