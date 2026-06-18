package com.grace.app.presentation.screens.devotional

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.presentation.components.CompletionRing
import com.grace.app.presentation.components.GraceButton
import com.grace.app.presentation.components.MenuButtonRow
import com.grace.app.presentation.theme.GraceCardAlt
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GraceRose

private val stepLabels = listOf("Scripture", "Reflection", "Prayer", "Journal")

private enum class DevoTab { DEVOTIONAL, MEDITATION }

@Composable
fun DevotionalScreen(
    onBackToHome: () -> Unit,
    onOpenMenu: () -> Unit,
    viewModel: DevotionalViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf(DevoTab.DEVOTIONAL) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
    ) {
        MenuButtonRow(onOpenMenu)
        DevoTabSwitcher(active = activeTab, onChange = { activeTab = it })
        Box(modifier = Modifier.fillMaxSize()) {
            when (activeTab) {
                DevoTab.DEVOTIONAL -> DevotionalTabBody(
                    state = state,
                    viewModel = viewModel,
                    onBackToHome = onBackToHome
                )
                DevoTab.MEDITATION -> WeeklyMeditationTab()
            }
        }
    }
}

@Composable
private fun DevoTabSwitcher(active: DevoTab, onChange: (DevoTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TabPill(
            label = "📖 Today's Devotional",
            selected = active == DevoTab.DEVOTIONAL,
            modifier = Modifier.weight(1f),
            onClick = { onChange(DevoTab.DEVOTIONAL) }
        )
        TabPill(
            label = "📅 This Week's Meditation",
            selected = active == DevoTab.MEDITATION,
            modifier = Modifier.weight(1f),
            onClick = { onChange(DevoTab.MEDITATION) }
        )
    }
}

@Composable
private fun TabPill(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) GraceGold else GraceCardBg)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) GraceDeepBlue else GraceCream,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun DevotionalTabBody(
    state: DevotionalUiState,
    viewModel: DevotionalViewModel,
    onBackToHome: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && state.devotional == null ->
                CircularProgressIndicator(
                    color = GraceGold,
                    modifier = Modifier.align(Alignment.Center)
                )

            state.devotional == null ->
                EmptyState(
                    message = state.error
                        ?: "No devotional available yet. Check back soon.",
                    onRetry = { viewModel.onEvent(DevotionalEvent.RetryLoad) }
                )

            state.isDone && !state.isReReading ->
                CompletionState(
                    streak = state.streakCount,
                    onBackToHome = onBackToHome,
                    onReadAgain = { viewModel.onEvent(DevotionalEvent.ReadAgain) }
                )

            else -> DevotionalContent(state, viewModel)
        }
    }
}

@Composable
private fun EmptyState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📖", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(message, color = GraceCreamDim, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        GraceButton(text = "Try Again", onClick = onRetry)
    }
}

@Composable
private fun DevotionalContent(
    state: DevotionalUiState,
    viewModel: DevotionalViewModel
) {
    val devo = state.devotional ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "TODAY'S DEVOTIONAL",
                    color = GraceGold,
                    fontSize = 10.sp,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(devo.title, color = GraceCream, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            CompletionRing(
                progress = state.progress,
                isDone = state.isDone,
                onTap = { viewModel.onEvent(DevotionalEvent.NextStep) },
                size = 84.dp
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stepLabels.forEachIndexed { index, label ->
                StepPill(
                    label = label,
                    state = when {
                        index < state.currentStep -> PillState.DONE
                        index == state.currentStep -> PillState.CURRENT
                        else -> PillState.FUTURE
                    },
                    onClick = { viewModel.onEvent(DevotionalEvent.GoToStep(index)) }
                )
            }
        }

        if (state.isOfflineCached) {
            Spacer(Modifier.height(10.dp))
            Text("📡 Offline — showing cached devotional", color = GraceGreen, fontSize = 12.sp)
        }

        Spacer(Modifier.height(20.dp))
        when (state.currentStep) {
            0 -> ScriptureStep(devo.verseText, devo.verseRef) {
                viewModel.onEvent(DevotionalEvent.NextStep)
            }
            1 -> TextStep(
                label = "TODAY'S REFLECTION",
                body = devo.reflection,
                buttonText = "Continue to Prayer →"
            ) { viewModel.onEvent(DevotionalEvent.NextStep) }
            2 -> TextStep(
                label = "PRAYER STARTER",
                body = devo.prayerStarter,
                buttonText = "Continue to Journal →",
                helper = "Close your eyes and pray in your own words. " +
                    "This is just a starting point."
            ) { viewModel.onEvent(DevotionalEvent.NextStep) }
            else -> JournalStep(state, viewModel, devo.journalPrompt)
        }

        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text("⚠ ${state.error}", color = GraceRose)
        }
        Spacer(Modifier.height(32.dp))
    }
}

private enum class PillState { DONE, CURRENT, FUTURE }

@Composable
private fun StepPill(label: String, state: PillState, onClick: () -> Unit) {
    val (bg, fg) = when (state) {
        PillState.DONE -> GraceGold.copy(alpha = 0.15f) to GraceGold
        PillState.CURRENT -> GraceGold to GraceDeepBlue
        PillState.FUTURE -> GraceCardBg to GraceMuted
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ScriptureStep(verseText: String, verseRef: String, onContinue: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GraceCardAlt),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("✦", color = GraceGold, fontSize = 32.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                verseText,
                color = GraceCream,
                fontSize = 22.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(verseRef, color = GraceGold, fontSize = 12.sp)
        }
    }
    Spacer(Modifier.height(20.dp))
    GraceButton(text = "Continue to Reflection →", onClick = onContinue)
}

@Composable
private fun TextStep(
    label: String,
    body: String,
    buttonText: String,
    helper: String? = null,
    onContinue: () -> Unit
) {
    Text(label, color = GraceGold, fontSize = 10.sp, letterSpacing = 3.sp)
    Spacer(Modifier.height(12.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp)
    ) {
        Text(
            body,
            color = GraceCreamDim,
            fontSize = 17.sp,
            lineHeight = 30.sp,
            modifier = Modifier.padding(20.dp)
        )
    }
    if (helper != null) {
        Spacer(Modifier.height(10.dp))
        Text(
            helper,
            color = GraceCreamDim,
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic
        )
    }
    Spacer(Modifier.height(20.dp))
    GraceButton(text = buttonText, onClick = onContinue)
}

@Composable
private fun JournalStep(
    state: DevotionalUiState,
    viewModel: DevotionalViewModel,
    prompt: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("MY JOURNAL", color = GraceGold, fontSize = 10.sp, letterSpacing = 3.sp)
        Spacer(Modifier.width(8.dp))
        Text("🔒 Private", color = GraceGreen, fontSize = 10.sp)
    }
    Spacer(Modifier.height(12.dp))
    Text(prompt, color = GraceCream, fontSize = 16.sp, fontStyle = FontStyle.Italic)
    Spacer(Modifier.height(12.dp))

    if (state.isDone) {
        Text(
            "✓ You've already completed this devotional. Your journal is private " +
                "to you and backed up to your account.",
            color = GraceGreen,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(20.dp))
        GraceButton(
            text = "Back to Summary",
            onClick = { viewModel.onEvent(DevotionalEvent.BackToSummary) },
            containerColor = GraceGreen,
            contentColor = GraceDeepBlue
        )
    } else {
        OutlinedTextField(
            value = state.journalText,
            onValueChange = { viewModel.onEvent(DevotionalEvent.JournalTextChanged(it)) },
            placeholder = {
                Text("Write your thoughts… this is private, just between you and God.")
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GraceGold,
                unfocusedBorderColor = GraceMuted,
                focusedContainerColor = GraceCardBg,
                unfocusedContainerColor = GraceCardBg,
                focusedTextColor = GraceCream,
                unfocusedTextColor = GraceCream,
                cursorColor = GraceGold
            )
        )
        Spacer(Modifier.height(20.dp))
        GraceButton(
            text = "Mark Devotional Complete ✓",
            onClick = { viewModel.onEvent(DevotionalEvent.MarkComplete) },
            enabled = state.journalText.isNotBlank(),
            loading = state.isMarkingComplete,
            containerColor = GraceGreen,
            contentColor = GraceDeepBlue
        )
    }
}

@Composable
private fun CompletionState(
    streak: Int,
    onBackToHome: () -> Unit,
    onReadAgain: () -> Unit
) {
    AnimatedContent(
        targetState = streak,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "completion"
    ) { currentStreak ->
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CompletionRing(progress = 1f, isDone = true, onTap = {}, size = 120.dp)
            Spacer(Modifier.height(24.dp))
            Text(
                "🎉 Devotional Complete!",
                color = GraceGreen,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Day $currentStreak streak — God sees your faithfulness.",
                color = GraceCreamDim,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            GraceButton(text = "Back to Home", onClick = onBackToHome)
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onReadAgain) {
                Text("Read Again", color = GraceGold)
            }
        }
    }
}
