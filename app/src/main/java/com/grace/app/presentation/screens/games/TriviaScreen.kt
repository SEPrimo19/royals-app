package com.grace.app.presentation.screens.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.BibleQuestion
import com.grace.app.domain.model.GameDifficulty
import com.grace.app.presentation.theme.GraceBlue
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GraceOrange
import com.grace.app.presentation.theme.GracePurple
import com.grace.app.presentation.theme.GraceRose

@Composable
fun TriviaScreen(
    onExit: () -> Unit,
    viewModel: TriviaViewModel = hiltViewModel()
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
        val diff = state.difficulty
        Header(
            title = when {
                state.isDaily && diff != null ->
                    "Daily " + diff.name.lowercase()
                        .replaceFirstChar { it.uppercase() } + " 🎯"
                state.isDaily -> "Daily Challenge 🎯"
                else -> "Practice 📖"
            },
            progress = state.progressLabel,
            onExit = onExit
        )
        Spacer(Modifier.height(12.dp))
        if (state.isDaily && state.questions.isNotEmpty()) {
            val pct = (state.currentIndex + (if (state.hasAnswered) 1 else 0))
                .toFloat() / state.questions.size
            LinearProgressIndicator(
                progress = { pct.coerceIn(0f, 1f) },
                color = GraceGold,
                trackColor = GraceMuted,
                modifier = Modifier.fillMaxWidth().height(4.dp)
            )
        } else if (!state.isDaily && !state.isFinished) {
            // Practice: lives + countdown timer pinned above the question.
            PracticeHud(
                lives = state.livesRemaining,
                seconds = state.timerSeconds,
                answered = state.hasAnswered,
                timerFrozen = state.timerFrozen
            )
        }
        // Lifelines bar — shown while a question is active, hides on
        // round-finished. Joshua only in Practice (Daily has no timer);
        // Daniel works in both.
        if (!state.isFinished && state.currentQuestion != null) {
            Spacer(Modifier.height(10.dp))
            LifelinesBar(
                lifelines = state.lifelines,
                showJoshua = !state.isDaily,
                joshuaActiveThisQ = state.timerFrozen,
                danielUsedThisQ = state.eliminatedIndices.isNotEmpty(),
                lifelineError = state.lifelineError,
                onUseJoshua = viewModel::useJoshua,
                onUseDaniel = viewModel::useDaniel,
                onDismissError = viewModel::dismissLifelineError
            )
        }
        Spacer(Modifier.height(20.dp))

        when {
            state.isLoading ->
                Box(Modifier.fillMaxWidth().height(280.dp), Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }
            state.error != null ->
                Text("⚠ ${state.error}", color = GraceRose)
            state.isFinished ->
                FinishedCard(state, onExit)
            state.currentQuestion != null ->
                QuestionBody(state, viewModel::selectOption, viewModel::next)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Header(title: String, progress: String, onExit: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "←", color = GraceCream, fontSize = 22.sp,
            modifier = Modifier.clickable { onExit() }.padding(end = 12.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = GraceCream, fontSize = 20.sp,
                fontWeight = FontWeight.Bold)
            if (progress.isNotBlank()) {
                Text(progress, color = GraceCreamDim, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun QuestionBody(
    state: TriviaUiState,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit
) {
    val q = state.currentQuestion ?: return
    DifficultyChip(q.difficulty)
    Spacer(Modifier.height(12.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            q.question,
            color = GraceCream, fontSize = 18.sp, lineHeight = 26.sp,
            modifier = Modifier.padding(20.dp)
        )
    }
    Spacer(Modifier.height(16.dp))
    q.options.forEachIndexed { idx, label ->
        OptionRow(
            label = label,
            index = idx,
            state = state,
            onClick = { onSelect(idx) }
        )
        Spacer(Modifier.height(10.dp))
    }
    AnimatedVisibility(
        visible = state.hasAnswered,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Column {
            Spacer(Modifier.height(8.dp))
            // Always render the reveal card on answered — even questions
            // without an explanation get the correct/wrong header + (when
            // wrong) the explicit "Correct answer: X" reveal so the user
            // walks away knowing the right answer.
            AnswerRevealCard(
                question = q,
                selectedIndex = state.selectedOption,
                timedOut = state.timedOut
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.isLastQuestion) "Finish ✓" else "Next →",
                    color = GraceDeepBlue, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Post-answer reveal card. Three states, all driven by the same data:
 *   - Correct → green "✓ Correct!" header + explanation
 *   - Wrong   → rose "✗ Not quite" header + "The correct answer is: …" +
 *               explanation framed as "Here's why"
 *   - Timed out → orange "⏱ Time's up" header + correct answer + explanation
 *
 * We only have ONE `explanation` per question (schema is one big "why is
 * the correct answer right" field). Per-option rationale would require a
 * schema change — surfaced to the user; not in v1.
 */
@Composable
private fun AnswerRevealCard(
    question: BibleQuestion,
    selectedIndex: Int?,
    timedOut: Boolean
) {
    val correctIndex = question.correctIndex
    val correctText = question.options.getOrNull(correctIndex) ?: "—"
    val isCorrect = selectedIndex != null && selectedIndex == correctIndex
    val isWrong = selectedIndex != null && selectedIndex != correctIndex

    val (headerEmoji, headerText, headerColor) = when {
        isCorrect -> Triple("✓", "Correct!", GraceGreen)
        isWrong -> Triple("✗", "Not quite — that's not it.", GraceRose)
        timedOut -> Triple("⏱", "Time's up.", GraceOrange)
        else -> Triple("·", "", GraceCreamDim)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: correct/wrong/timed-out status.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(headerEmoji, color = headerColor, fontSize = 18.sp,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text(
                    headerText, color = headerColor,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold
                )
            }
            // For wrong / timed-out, surface the correct answer plainly
            // so the user always walks away knowing it.
            if (!isCorrect) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Correct answer",
                    color = GraceCreamDim, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    correctText, color = GraceGreen,
                    fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    lineHeight = 21.sp
                )
            }
            // The general explanation. Framed as "Why?" because it explains
            // the CORRECT answer specifically (schema only has one field).
            if (!question.explanation.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    if (isCorrect) "Why this is right"
                    else "Why the correct answer is right",
                    color = GraceCreamDim, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    question.explanation!!,
                    color = GraceCream, fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            }
            if (!question.sourceRef.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "📖 ${question.sourceRef}",
                    color = GraceGold, fontSize = 11.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun DifficultyChip(d: GameDifficulty) {
    val (label, accent) = when (d) {
        GameDifficulty.EASY -> "EASY · 10 pts" to GraceGreen
        GameDifficulty.MEDIUM -> "MEDIUM · 20 pts" to GraceGold
        GameDifficulty.HARD -> "HARD · 30 pts" to GraceRose
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

@Composable
private fun OptionRow(
    label: String,
    index: Int,
    state: TriviaUiState,
    onClick: () -> Unit
) {
    val q = state.currentQuestion ?: return
    val answered = state.hasAnswered
    val selected = state.selectedOption == index
    val isCorrect = index == q.correctIndex
    // Daniel Effect — 50/50 dims this option to ~30% and blocks taps.
    val isEliminated = index in state.eliminatedIndices

    // `timedOut` means no option was picked but the timer hit zero. We
    // still want to surface the correct answer in green so the player
    // learns the answer — there's just no selected-wrong-option to color.
    val bg = when {
        !answered -> GraceCardBg
        isCorrect -> GraceGreen.copy(alpha = 0.25f)
        selected && !isCorrect -> GraceRose.copy(alpha = 0.25f)
        else -> GraceCardBg
    }
    val border = when {
        !answered -> Color.Transparent
        isCorrect -> GraceGreen
        selected && !isCorrect -> GraceRose
        else -> Color.Transparent
    }
    val text = when {
        !answered -> GraceCream
        isCorrect -> GraceGreen
        selected && !isCorrect -> GraceRose
        else -> GraceCream
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isEliminated) 0.3f else 1f)
            .clickable(enabled = !answered && !isEliminated) { onClick() }
            .background(bg, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val letter = ('A' + index).toString()
        Text(
            letter,
            color = if (answered && isCorrect) GraceGreen
                else if (answered && selected) GraceRose
                else GracePurple,
            fontSize = 14.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.size(28.dp)
                .background(border.copy(alpha = 0.15f), RoundedCornerShape(50))
                .padding(vertical = 4.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.size(12.dp))
        Text(
            label,
            color = text, fontSize = 15.sp,
            modifier = Modifier.weight(1f),
            lineHeight = 20.sp
        )
        if (answered) {
            Text(
                if (isCorrect) "✓" else if (selected) "✗" else "",
                color = if (isCorrect) GraceGreen else GraceRose,
                fontSize = 18.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FinishedCard(state: TriviaUiState, onExit: () -> Unit) {
    val outOfLives = state.finishedReason == FinishReason.OUT_OF_LIVES
    // Practice and Daily share the summary card layout but with different
    // copy: Practice doesn't have "5 of 10" framing, just total answered.
    val attempted = if (state.isDaily) state.questions.size
        else state.currentIndex + (if (state.hasAnswered) 1 else 0)
    val percent = if (attempted == 0) 0 else (state.correctCount * 100 / attempted)
    val tone = when {
        outOfLives -> "Out of lives!"
        percent >= 80 -> "Excellent!"
        percent >= 60 -> "Well done"
        percent >= 40 -> "Keep growing"
        else -> "Try again"
    }
    val emoji = when {
        outOfLives -> "💔"
        percent >= 80 -> "🎉"
        percent >= 40 -> "🌱"
        else -> "📖"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 48.sp)
            Spacer(Modifier.height(8.dp))
            Text(tone, color = GraceCream, fontSize = 20.sp,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text(
                "${state.correctCount} / $attempted correct",
                color = GraceGold, fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${state.pointsEarned} points earned",
                color = GraceCreamDim, fontSize = 13.sp
            )
            if (state.isDaily) {
                state.streakAfter?.let { streak ->
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "🔥 $streak day streak",
                        color = GracePurple, fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Games", color = GraceDeepBlue,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PracticeHud(
    lives: Int,
    seconds: Int,
    answered: Boolean,
    timerFrozen: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Hearts row — filled for remaining, hollow for spent. 5 max.
        Row {
            repeat(5) { i ->
                Text(
                    if (i < lives) "❤️" else "🤍",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 2.dp)
                )
            }
        }
        if (timerFrozen) {
            // Joshua Effect — timer is paused for this question.
            Text(
                "🛡️ FROZEN",
                color = GraceBlue, fontSize = 12.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp
            )
        } else {
            // Countdown — turns rose under 5s.
            val timerColor = if (seconds <= 5 && !answered) GraceRose else GraceCream
            Text(
                "⏱ ${seconds}s",
                color = timerColor, fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Suppress("unused") private val keepImport: BibleQuestion? = null
