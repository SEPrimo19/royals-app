package com.grace.app.presentation.screens.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
fun MemoryMatchScreen(
    onExit: () -> Unit,
    viewModel: MemoryMatchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Header(
            matchedPairs = state.matchedPairCount,
            mismatches = state.mismatchCount,
            points = state.pointsEarned,
            isPerfectSoFar = state.isPerfectSoFar,
            onExit = onExit
        )
        Spacer(Modifier.height(14.dp))

        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }
            state.error != null ->
                Text("⚠ ${state.error}", color = GraceRose)
            state.boardComplete ->
                CompletedCard(state = state,
                    onPlayAgain = { viewModel.newBoard() },
                    onExit = onExit)
            else ->
                Grid(state = state, onCardTapped = viewModel::onCardTapped)
        }
    }
}

@Composable
private fun Header(
    matchedPairs: Int,
    mismatches: Int,
    points: Int,
    isPerfectSoFar: Boolean,
    onExit: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "←", color = GraceCream, fontSize = 22.sp,
            modifier = Modifier.clickable { onExit() }.padding(end = 12.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text("Memory Cards 🃏", color = GraceCream, fontSize = 22.sp,
                fontWeight = FontWeight.Bold)
            Text(
                "$matchedPairs / $MEMORY_PAIRS_PER_BOARD matched · $mismatches mismatches · $points pts",
                color = GraceCreamDim, fontSize = 11.sp
            )
        }
        if (isPerfectSoFar && matchedPairs > 0) {
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
private fun Grid(
    state: MemoryMatchUiState,
    onCardTapped: (Int) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        itemsIndexed(
            items = state.cards,
            key = { idx, card -> "${idx}_${card.pairId}_${card.isReference}" }
        ) { idx, card ->
            val isFaceUp = idx in state.faceUpIndices || idx in state.matchedIndices
            val isMatched = idx in state.matchedIndices
            FlipCard(
                card = card,
                isFaceUp = isFaceUp,
                isMatched = isMatched,
                onTap = { onCardTapped(idx) }
            )
        }
    }
}

@Composable
private fun FlipCard(
    card: MemoryCard,
    isFaceUp: Boolean,
    isMatched: Boolean,
    onTap: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFaceUp) 180f else 0f,
        animationSpec = tween(durationMillis = 380),
        label = "card_flip"
    )
    val showFront = rotation > 90f

    val bg = when {
        isMatched -> GraceGreen.copy(alpha = 0.22f)
        showFront -> GraceCardBg
        else -> GraceCardAlt
    }
    val border = when {
        isMatched -> GraceGreen
        showFront -> GraceGold.copy(alpha = 0.6f)
        else -> GraceMuted.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.78f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable(enabled = !isFaceUp && !isMatched) { onTap() },
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (showFront) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (card.isReference) {
                        Text(
                            card.text,
                            color = if (isMatched) GraceGreen else GraceGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    } else {
                        Text(
                            card.text,
                            color = if (isMatched) GraceGreen else GraceCream,
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📖", fontSize = 24.sp)
                    Text(
                        "ROYALS", color = GraceCreamDim, fontSize = 9.sp,
                        letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(border.copy(alpha = 0.0f))
            )
        }
    }
}

@Composable
private fun CompletedCard(
    state: MemoryMatchUiState,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val perfect = state.isPerfectSoFar
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(if (perfect) "✨" else "🃏", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                if (perfect) "Perfect clear!" else "Board complete!",
                color = GraceCream, fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "${state.pointsEarned} points earned",
                color = GraceGold, fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${state.mismatchCount} mismatch${if (state.mismatchCount == 1) "" else "es"}" +
                    if (perfect) " · +$MEMORY_PERFECT_BONUS bonus 🎉" else "",
                color = GraceCreamDim, fontSize = 12.sp
            )

            Spacer(Modifier.height(18.dp))
            Text(
                "PAIRS THIS ROUND", color = GraceCreamDim, fontSize = 10.sp,
                letterSpacing = 2.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()) {
                state.pairs.forEach { p ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            p.reference, color = GraceGold,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 6.dp))
                        Text(
                            p.verseSnippet, color = GraceCreamDim,
                            fontSize = 12.sp, fontStyle = FontStyle.Italic
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

