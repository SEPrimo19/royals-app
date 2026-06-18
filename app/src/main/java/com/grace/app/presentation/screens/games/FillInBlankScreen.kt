package com.grace.app.presentation.screens.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.grace.app.presentation.theme.GracePurple
import com.grace.app.presentation.theme.GraceRose

@Composable
fun FillInBlankScreen(
    onExit: () -> Unit,
    viewModel: FillInBlankViewModel = hiltViewModel()
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←", color = GraceCream, fontSize = 22.sp,
                modifier = Modifier.clickable { onExit() }.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Daily Verse 📖", color = GraceCream, fontSize = 22.sp,
                    fontWeight = FontWeight.Bold)
                Text(
                    "Fill in the blank · 25 points",
                    color = GraceCreamDim, fontSize = 11.sp
                )
            }
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
                FinishedFitbCard(state, onExit)
            state.passage != null ->
                PassageBody(state, viewModel::selectOption, viewModel::finish)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun PassageBody(
    state: FillInBlankUiState,
    onSelect: (String) -> Unit,
    onFinish: () -> Unit
) {
    val passage = state.passage ?: return
    Text(
        "📖 ${passage.reference}",
        color = GraceGold, fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(
                GraceGold.copy(alpha = 0.18f),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
    Spacer(Modifier.height(14.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            if (state.hasAnswered) passage.text
            else state.verseWithBlank ?: passage.text,
            color = GraceCream, fontSize = 18.sp,
            lineHeight = 28.sp, fontStyle = FontStyle.Italic,
            modifier = Modifier.padding(20.dp)
        )
    }
    Spacer(Modifier.height(16.dp))

    if (!state.hasAnswered) {
        Text(
            "Pick the missing word",
            color = GraceCreamDim, fontSize = 11.sp,
            letterSpacing = 2.sp, fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
    }

    state.options.forEach { opt ->
        OptionRow(
            option = opt,
            state = state,
            onClick = { onSelect(opt) }
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GraceCardBg),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (state.correct) "✓ ${state.pointsEarned} points"
                        else "✗ The blank was ‘${state.passage?.blankWord}’.",
                        color = if (state.correct) GraceGreen else GraceRose,
                        fontSize = 14.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Reflect on this verse. Let it sit with you today.",
                        color = GraceCreamDim, fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Finish ✓", color = GraceDeepBlue,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun OptionRow(
    option: String,
    state: FillInBlankUiState,
    onClick: () -> Unit
) {
    val passage = state.passage ?: return
    val answered = state.hasAnswered
    val isCorrect = option.equals(passage.blankWord, ignoreCase = true)
    val selected = state.selectedOption == option

    val bg = when {
        !answered -> GraceCardBg
        isCorrect -> GraceGreen.copy(alpha = 0.25f)
        selected && !isCorrect -> GraceRose.copy(alpha = 0.25f)
        else -> GraceCardBg
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
            .clickable(enabled = !answered) { onClick() }
            .background(bg, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            option,
            color = text, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
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
private fun FinishedFitbCard(state: FillInBlankUiState, onExit: () -> Unit) {
    val emoji = if (state.correct) "✨" else "📖"
    val tone = if (state.correct) "Great recall!" else "Reflect and try tomorrow"
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
                "${state.pointsEarned} points earned",
                color = GraceGold, fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            state.streakAfter?.let { streak ->
                Spacer(Modifier.height(14.dp))
                Text(
                    "🔥 $streak day streak",
                    color = GracePurple, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
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

@Suppress("unused") private val keepImport: Color = Color.Transparent
