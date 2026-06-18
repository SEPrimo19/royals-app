package com.grace.app.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceMuted

@Composable
fun CompletionRing(
    progress: Float,
    isDone: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (isDone) 1f else progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "ring_progress"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (isDone) 1f else 0f,
        animationSpec = spring(),
        label = "check_scale"
    )
    val greenColor = GraceGreen
    val mutedColor = GraceMuted
    val ringColor = if (isDone) greenColor else GraceGold
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = !isDone
            ) { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = 6.dp.toPx()
            if (isDone) {
                drawCircle(color = greenColor.copy(alpha = 0.25f), radius = size.toPx() / 2f)
            }
            drawArc(
                color = mutedColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokePx + 1.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        if (isDone) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Devotional complete",
                tint = GraceGreen,
                modifier = Modifier
                    .size(size * 0.4f)
                    .scale(checkScale)
            )
        } else {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                color = GraceGold,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}
