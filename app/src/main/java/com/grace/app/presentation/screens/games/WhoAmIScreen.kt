package com.grace.app.presentation.screens.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
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
import androidx.compose.foundation.layout.width
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
import com.grace.app.domain.model.BibleCharacter
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
fun WhoAmIScreen(
    onExit: () -> Unit,
    viewModel: WhoAmIViewModel = hiltViewModel()
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
            correctCount = state.correctCount,
            livesRemaining = state.livesRemaining,
            onExit = onExit
        )
        if (!state.isFinished && state.currentCharacter != null) {
            Spacer(Modifier.height(10.dp))
            LifelinesBar(
                lifelines = state.lifelines,
                showJoshua = false,
                joshuaActiveThisQ = false,
                danielUsedThisQ = state.eliminatedIndices.isNotEmpty(),
                lifelineError = state.lifelineError,
                onUseJoshua = {   },
                onUseDaniel = viewModel::useDaniel,
                onDismissError = viewModel::dismissLifelineError
            )
        }
        Spacer(Modifier.height(16.dp))

        when {
            state.isLoading ->
                Box(Modifier.fillMaxWidth().height(280.dp), Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }
            state.error != null ->
                ErrorCard(state.error!!)
            state.isFinished ->
                FinishedCard(
                    totalPoints = state.totalPoints,
                    correctCount = state.correctCount,
                    charactersPlayed = state.charactersPlayed,
                    onPlayAgain = { viewModel.restart() },
                    onExit = onExit
                )
            state.currentCharacter != null ->
                RoundBody(state, viewModel::revealNextClue, viewModel::selectOption, viewModel::next)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Header(
    progress: String,
    totalPoints: Int,
    correctCount: Int,
    livesRemaining: Int,
    onExit: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "←", color = GraceCream, fontSize = 22.sp,
            modifier = Modifier.clickable { onExit() }.padding(end = 12.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text("Who Am I? 🤔", color = GraceCream, fontSize = 22.sp,
                fontWeight = FontWeight.Bold)
            Text(
                "$progress · $correctCount correct · $totalPoints pts",
                color = GraceCreamDim, fontSize = 11.sp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until MAX_LIVES_WAI) {
                val active = i < livesRemaining
                Text(
                    if (active) "❤️" else "🤍",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun FinishedCard(
    totalPoints: Int,
    correctCount: Int,
    charactersPlayed: Int,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🤔", fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text("Out of lives!", color = GraceCream, fontSize = 22.sp,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Text(
                "$totalPoints points earned",
                color = GraceGold, fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "$correctCount of $charactersPlayed characters correct",
                color = GraceCreamDim, fontSize = 13.sp
            )
            Spacer(Modifier.height(22.dp))
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

@Composable
private fun RoundBody(
    state: WhoAmIUiState,
    onPeelClue: () -> Unit,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit
) {
    val ch = state.currentCharacter ?: return
    val opts = state.options ?: return
    val attemptsLeft = (4 - state.wrongAttempts).coerceAtLeast(0)

    DifficultyChip(ch.difficulty)
    Spacer(Modifier.height(10.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "CLUE ${state.cluesRevealed} OF 4",
            color = GraceGold, fontSize = 10.sp,
            letterSpacing = 2.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.weight(1f))
        if (!state.hasAnswered) {
            AttemptsLeftBadge(attemptsLeft)
        }
    }
    Spacer(Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until state.cluesRevealed) {
            ClueCard(index = i + 1, text = ch.clues[i])
        }
    }

    Spacer(Modifier.height(14.dp))

    if (!state.hasAnswered && state.cluesRevealed < 4) {
        OutlinedButton(
            onClick = onPeelClue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "🔍 Next Clue (use one to lower score)",
                color = GraceCream, fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(14.dp))
    }

    Text(
        "WHO AM I?", color = GraceCreamDim, fontSize = 10.sp,
        letterSpacing = 2.sp, fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))
    opts.options.forEachIndexed { idx, label ->
        OptionRow(
            label = label,
            index = idx,
            state = state,
            correctIndex = opts.correctIndex,
            onClick = { onSelect(idx) }
        )
        Spacer(Modifier.height(10.dp))
    }

    AnimatedVisibility(
        visible = state.hasAnswered,
        enter = fadeIn(),
        exit = ExitTransition.None
    ) {
        Column {
            Spacer(Modifier.height(8.dp))
            RevealCard(character = ch, state = state)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next Character →", color = GraceDeepBlue,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ClueCard(index: Int, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardAlt),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                "#$index", color = GraceGold, fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text,
                color = GraceCream, fontSize = 15.sp, lineHeight = 22.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun OptionRow(
    label: String,
    index: Int,
    state: WhoAmIUiState,
    correctIndex: Int,
    onClick: () -> Unit
) {
    val isCorrect = index == correctIndex
    val isSelected = state.selectedOption == index
    val answered = state.hasAnswered
    val isEliminated = index in state.eliminatedIndices

    val bg = when {
        answered && isCorrect -> GraceGreen.copy(alpha = 0.25f)
        answered && isSelected -> GraceRose.copy(alpha = 0.25f)
        else -> GraceCardBg
    }
    val textColor = when {
        answered && isCorrect -> GraceGreen
        answered && isSelected -> GraceRose
        else -> GraceCream
    }
    val borderColor = when {
        answered && isCorrect -> GraceGreen
        answered && isSelected -> GraceRose
        else -> GraceMuted.copy(alpha = 0.4f)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isEliminated) 0.3f else 1f)
            .background(bg, RoundedCornerShape(14.dp))
            .clickable(enabled = !answered && !isEliminated) { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(borderColor.copy(alpha = 0.25f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                ('A' + index).toString(),
                color = textColor, fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            label, color = textColor, fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (answered) {
            Text(
                if (isCorrect) "✓" else if (isSelected) "✗" else "",
                color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RevealCard(character: BibleCharacter, state: WhoAmIUiState) {
    val (headerEmoji, headerText, headerColor) = when {
        state.isCorrect -> Triple(
            "🎉", "Correct! +${state.pointsEarned} pts (${state.cluesRevealed} clue${
                if (state.cluesRevealed == 1) "" else "s"
            } used)", GraceGreen
        )
        else -> Triple("✗", "Out of attempts — 0 pts", GraceRose)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(headerEmoji, color = headerColor, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(
                    headerText, color = headerColor,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "ANSWER", color = GraceCreamDim, fontSize = 10.sp,
                letterSpacing = 2.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                character.name, color = GraceGreen,
                fontSize = 17.sp, fontWeight = FontWeight.Bold
            )
            if (!character.explanation.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    character.explanation, color = GraceCream,
                    fontSize = 13.sp, lineHeight = 19.sp
                )
            }
            if (!character.sourceRef.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "📖 ${character.sourceRef}",
                    color = GraceGold, fontSize = 11.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun AttemptsLeftBadge(attemptsLeft: Int) {
    val color = when {
        attemptsLeft >= 3 -> GraceGreen
        attemptsLeft == 2 -> GraceGold
        else -> GraceRose
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            "💗 $attemptsLeft attempt${if (attemptsLeft == 1) "" else "s"} left",
            color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("⚠", color = GraceRose, fontSize = 36.sp)
            Spacer(Modifier.height(8.dp))
            Text(message, color = GraceCream, fontSize = 14.sp,
                fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DifficultyChip(d: com.grace.app.domain.model.GameDifficulty) {
    val (label, accent) = when (d) {
        com.grace.app.domain.model.GameDifficulty.EASY -> "EASY · up to 40 pts" to GraceGreen
        com.grace.app.domain.model.GameDifficulty.MEDIUM -> "MEDIUM · up to 40 pts" to GraceGold
        com.grace.app.domain.model.GameDifficulty.HARD -> "HARD · up to 40 pts" to GraceRose
    }
    Text(
        label,
        color = accent, fontSize = 10.sp,
        letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(accent.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

