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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.BibleEvent
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
fun TimelineSortScreen(
    onExit: () -> Unit,
    viewModel: TimelineSortViewModel = hiltViewModel()
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
            wrongTaps = state.wrongTapsThisPuzzle,
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
            state.currentPuzzle != null ->
                PuzzleBody(
                    state = state,
                    onEventTap = viewModel::onEventTapped,
                    onPlacedTap = viewModel::onPlacedTapped,
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
            Text("Timeline Sort 📜", color = GraceCream, fontSize = 22.sp,
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
private fun PuzzleBody(
    state: TimelineSortUiState,
    onEventTap: (BibleEvent) -> Unit,
    onPlacedTap: (BibleEvent) -> Unit,
    onNext: () -> Unit
) {
    val total = state.correctOrder.size

    Text(
        "EARLIEST → LATEST",
        color = GraceCreamDim, fontSize = 10.sp,
        letterSpacing = 2.sp, fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))
    SlotsCard(state = state, total = total, onPlacedTap = onPlacedTap)

    Spacer(Modifier.height(16.dp))

    if (!state.puzzleComplete) {
        Text(
            "TAP AN EVENT TO PLACE IT",
            color = GraceCreamDim, fontSize = 10.sp,
            letterSpacing = 2.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        EventPool(
            events = state.poolForPuzzle,
            wrongFlashEventId = state.wrongFlashEventId,
            onTap = onEventTap
        )
    }

    if (state.puzzleComplete) {
        Spacer(Modifier.height(16.dp))
        CompletionBanner(state = state)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(
                if (state.currentIndex == state.puzzles.lastIndex)
                    "Finish round ✓" else "Next puzzle →",
                color = GraceDeepBlue, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SlotsCard(
    state: TimelineSortUiState,
    total: Int,
    onPlacedTap: (BibleEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0 until total) {
                val placed = state.placed.getOrNull(i)
                if (placed != null) {
                    PlacedSlot(
                        index = i + 1,
                        event = placed,
                        isPuzzleComplete = state.puzzleComplete,
                        onTap = { onPlacedTap(placed) }
                    )
                } else {
                    EmptySlot(index = i + 1)
                }
            }
        }
    }
}

@Composable
private fun PlacedSlot(
    index: Int,
    event: BibleEvent,
    isPuzzleComplete: Boolean,
    onTap: () -> Unit
) {
    val accent = if (isPuzzleComplete) GraceGreen else GraceGold
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(accent.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
            .border(1.dp, accent, RoundedCornerShape(10.dp))
            .clickable(enabled = !isPuzzleComplete) { onTap() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$index.", color = accent, fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            event.title, color = accent, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EmptySlot(index: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GraceCardAlt.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .border(1.dp, GraceMuted.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$index.", color = GraceCreamDim, fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            "—", color = GraceCreamDim, fontSize = 13.sp
        )
    }
}

@Composable
private fun EventPool(
    events: List<BibleEvent>,
    wrongFlashEventId: String?,
    onTap: (BibleEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            events.forEach { ev ->
                PoolChip(
                    event = ev,
                    isFlashing = wrongFlashEventId == ev.id,
                    onTap = { onTap(ev) }
                )
            }
            if (events.isEmpty()) {
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
private fun PoolChip(event: BibleEvent, isFlashing: Boolean, onTap: () -> Unit) {
    val tint by animateFloatAsState(
        targetValue = if (isFlashing) 1f else 0f,
        animationSpec = tween(250),
        label = "tl_wrong_flash"
    )
    val bg = if (tint > 0.5f) GraceRose.copy(alpha = 0.35f) else GraceCardAlt
    val border = if (tint > 0.5f) GraceRose else GraceGold.copy(alpha = 0.55f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(10.dp))
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable { onTap() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            event.title, color = GraceCream,
            fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompletionBanner(state: TimelineSortUiState) {
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
                    "Puzzle complete · +${state.pointsThisPuzzle} pts" +
                        if (perfect) " (incl. +$TIMELINE_PERFECT_BONUS perfect bonus)" else "",
                    color = GraceGreen, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FinishedCard(
    state: TimelineSortUiState,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val maxPossible = TIMELINE_PUZZLES_PER_ROUND *
        (TIMELINE_PUZZLE_POINTS + TIMELINE_PERFECT_BONUS)
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
            Text(if (perfectRound) "🏆" else "📜", fontSize = 48.sp)
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
                "${state.puzzlesCompleted} of ${state.puzzles.size} puzzles · up to $maxPossible",
                color = GraceCreamDim, fontSize = 12.sp
            )

            Spacer(Modifier.height(18.dp))
            Text(
                "THIS ROUND'S TIMELINE",
                color = GraceCreamDim, fontSize = 10.sp,
                letterSpacing = 2.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                state.puzzles.forEachIndexed { puzzleIdx, puzzle ->
                    val sorted = puzzle.sortedBy { it.chronologicalOrder }
                    Text(
                        "Puzzle ${puzzleIdx + 1}",
                        color = GraceGold, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    sorted.forEachIndexed { i, ev ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "${i + 1}.", color = GraceCreamDim,
                                fontSize = 11.sp, fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.size(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    ev.title, color = GraceCream, fontSize = 12.sp
                                )
                                if (!ev.approxYearText.isNullOrBlank()) {
                                    Text(
                                        ev.approxYearText,
                                        color = GraceCreamDim, fontSize = 10.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
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
