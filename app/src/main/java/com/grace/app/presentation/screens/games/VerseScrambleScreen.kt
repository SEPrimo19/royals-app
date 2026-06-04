package com.grace.app.presentation.screens.games

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
fun VerseScrambleScreen(
    onExit: () -> Unit,
    viewModel: VerseScrambleViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Header(
            progress = state.progressLabel,
            totalPoints = state.totalPoints,
            wrongTaps = state.wrongTapsThisVerse,
            isPerfectSoFar = state.isPerfectSoFar,
            onExit = onExit
        )
        Spacer(Modifier.height(14.dp))

        when {
            state.isLoading ->
                Box(Modifier.fillMaxWidth().height(280.dp), Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }
            state.error != null ->
                Text("⚠ ${state.error}", color = GraceRose)
            state.isRoundFinished ->
                FinishedCard(
                    state = state,
                    onPlayAgain = { viewModel.newRound() },
                    onExit = onExit
                )
            state.currentVerse != null ->
                RoundBody(
                    state = state,
                    onChipTap = viewModel::onChipTapped,
                    onPlacedTap = viewModel::onPlacedChipTapped,
                    onNext = viewModel::next
                )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Header(
    progress: String,
    totalPoints: Int,
    wrongTaps: Int,
    isPerfectSoFar: Boolean,
    onExit: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "←", color = GraceCream, fontSize = 22.sp,
            modifier = Modifier.clickable { onExit() }.padding(end = 12.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text("Verse Scramble 🧩", color = GraceCream, fontSize = 22.sp,
                fontWeight = FontWeight.Bold)
            Text(
                "$progress · $wrongTaps wrong taps · $totalPoints pts",
                color = GraceCreamDim, fontSize = 11.sp
            )
        }
        if (isPerfectSoFar) {
            Box(
                modifier = Modifier
                    .background(GraceGold.copy(alpha = 0.18f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("✨ PERFECT", color = GraceGold, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            }
        }
    }
}

@Composable
private fun RoundBody(
    state: VerseScrambleUiState,
    onChipTap: (WordChip) -> Unit,
    onPlacedTap: (WordChip) -> Unit,
    onNext: () -> Unit
) {
    val verse = state.currentVerse ?: return

    // Reference chip
    Text(
        "📖 ${verse.reference}",
        color = GraceGold, fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(GraceGold.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
    Spacer(Modifier.height(14.dp))

    // SLOTS — the assembled verse in placement order. Renders one word
    // chip per filled slot + a placeholder underscore card per remaining
    // slot, so the user sees their progress.
    Text(
        "ASSEMBLE THE VERSE",
        color = GraceCreamDim, fontSize = 10.sp,
        letterSpacing = 2.sp, fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))
    SlotsCard(state = state, onPlacedTap = onPlacedTap)

    Spacer(Modifier.height(18.dp))

    // CHIP POOL — render remaining words for tap-to-place. We don't show
    // the pool once the verse is complete.
    if (!state.verseComplete) {
        Text(
            "TAP A WORD TO ADD IT",
            color = GraceCreamDim, fontSize = 10.sp,
            letterSpacing = 2.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        ChipPool(
            chips = state.pool,
            wrongFlashChipIndex = state.wrongFlashChipIndex,
            onTap = onChipTap
        )
    }

    // Bottom CTA once the verse is complete.
    if (state.verseComplete) {
        Spacer(Modifier.height(16.dp))
        CompletionBanner(state = state)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                if (state.currentIndex == state.round.lastIndex)
                    "Finish round ✓" else "Next verse →",
                color = GraceDeepBlue, fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SlotsCard(
    state: VerseScrambleUiState,
    onPlacedTap: (WordChip) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        // FlowRow wraps onto multiple lines when the verse has many words.
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val totalSlots = state.correctWords.size
            for (i in 0 until totalSlots) {
                val placed = state.placed.getOrNull(i)
                if (placed != null) {
                    PlacedChip(
                        text = placed.text,
                        isVerseComplete = state.verseComplete,
                        onTap = { onPlacedTap(placed) }
                    )
                } else {
                    EmptySlot()
                }
            }
        }
    }
}

@Composable
private fun PlacedChip(text: String, isVerseComplete: Boolean, onTap: () -> Unit) {
    val accent = if (isVerseComplete) GraceGreen else GraceGold
    Box(
        modifier = Modifier
            .background(accent.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            .border(1.dp, accent, RoundedCornerShape(10.dp))
            .clickable(enabled = !isVerseComplete) { onTap() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text, color = accent, fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptySlot() {
    Box(
        modifier = Modifier
            .background(GraceCardAlt.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .border(1.dp, GraceMuted.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text("___", color = GraceCreamDim, fontSize = 13.sp,
            fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipPool(
    chips: List<WordChip>,
    wrongFlashChipIndex: Int?,
    onTap: (WordChip) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chips.forEach { chip ->
                PoolChip(
                    text = chip.text,
                    isFlashing = wrongFlashChipIndex == chip.originalIndex,
                    onTap = { onTap(chip) }
                )
            }
            if (chips.isEmpty()) {
                Text(
                    "All placed — confirm with Next →",
                    color = GraceCreamDim, fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun PoolChip(text: String, isFlashing: Boolean, onTap: () -> Unit) {
    // Background flashes rose on a wrong tap, then animates back to neutral.
    val targetTint by animateFloatAsState(
        targetValue = if (isFlashing) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "wrong_flash"
    )
    val bg = if (targetTint > 0.5f)
        GraceRose.copy(alpha = 0.35f)
    else
        GraceCardAlt
    val border = if (targetTint > 0.5f) GraceRose
                 else GraceGold.copy(alpha = 0.55f)
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(10.dp))
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable { onTap() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text, color = GraceCream, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CompletionBanner(state: VerseScrambleUiState) {
    val perfect = state.isPerfectSoFar
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✓", color = GraceGreen, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(8.dp))
                Text(
                    "Verse complete · +${state.pointsThisVerse} pts" +
                        if (perfect) " (incl. +$VERSE_PERFECT_BONUS perfect bonus)" else "",
                    color = GraceGreen, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FinishedCard(
    state: VerseScrambleUiState,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val maxPossible = VERSES_PER_ROUND * (VERSE_POINTS + VERSE_PERFECT_BONUS)
    val perfectRound = state.totalPoints >= maxPossible
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (perfectRound) "🏆" else "🧩", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                if (perfectRound) "Perfect round!" else "Round complete!",
                color = GraceCream, fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "${state.totalPoints} points earned",
                color = GraceGold, fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${state.versesCompleted} of ${state.round.size} verses · up to $maxPossible",
                color = GraceCreamDim, fontSize = 12.sp
            )

            Spacer(Modifier.height(18.dp))
            Text(
                "THIS ROUND'S VERSES",
                color = GraceCreamDim, fontSize = 10.sp,
                letterSpacing = 2.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()) {
                state.round.forEach { v ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            v.reference, color = GraceGold,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.alpha(0.95f)
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            v.text, color = GraceCream,
                            fontSize = 12.sp, fontStyle = FontStyle.Italic,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Play again", color = GraceDeepBlue,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Text("Back to Games", color = GraceCream)
            }
        }
    }
}
