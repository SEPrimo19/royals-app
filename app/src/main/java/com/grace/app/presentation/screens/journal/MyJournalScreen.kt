package com.grace.app.presentation.screens.journal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.grace.app.domain.model.JournalEntry
import com.grace.app.presentation.theme.GraceCardAlt
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGoldDim
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceRose
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MyJournalScreen(
    onBack: () -> Unit,
    viewModel: MyJournalViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
    ) {
        Header(onBack = onBack)
        PrivacyBanner()

        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = GraceGold) }

            state.entries.isEmpty() -> EmptyState()

            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.entries, key = { it.devoId }) { entry ->
                    JournalRow(
                        entry = entry,
                        isExpanded = entry.devoId in state.expandedDevoIds,
                        onToggle = {
                            viewModel.onEvent(MyJournalEvent.ToggleExpanded(entry.devoId))
                        }
                    )
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "←",
            color = GraceCream, fontSize = 22.sp,
            modifier = Modifier
                .clickable { onBack() }
                .padding(end = 12.dp)
        )
        Column {
            Text("My Journal", color = GraceCream, fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold)
            Text("A private record of your time with God.",
                color = GraceCreamDim, fontSize = 12.sp,
                fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun PrivacyBanner() {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(GraceGreen.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔒", fontSize = 14.sp)
        Spacer(Modifier.size(8.dp))
        Text(
            "Encrypted on this device. Only you can read these entries.",
            color = GraceGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("📖", fontSize = 44.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "No journal entries yet.",
                color = GraceCream, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Complete today's devotional to write your first entry.",
                color = GraceCreamDim, fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun JournalRow(
    entry: JournalEntry,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GraceCardBg)
            .clickable { onToggle() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatDate(entry.completedAt),
                    color = GraceGoldDim, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(entry.devoTitle, color = GraceCream,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(entry.verseRef, color = GraceGold, fontSize = 12.sp,
                    fontStyle = FontStyle.Italic)
            }
            Text(if (isExpanded) "▾" else "▸",
                color = GraceCreamDim, fontSize = 18.sp)
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                // Verse text
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(GraceCardAlt)
                        .padding(12.dp)
                ) {
                    Text(entry.verseText,
                        color = GraceCream, fontSize = 14.sp,
                        fontStyle = FontStyle.Italic)
                }

                if (entry.journalPrompt.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    SectionLabel("PROMPT")
                    Text(entry.journalPrompt,
                        color = GraceCreamDim, fontSize = 13.sp,
                        fontStyle = FontStyle.Italic)
                }

                Spacer(Modifier.height(10.dp))
                SectionLabel("MY REFLECTION")
                when {
                    !entry.isReadable -> Text(
                        "This entry can't be read on this device. " +
                            "Journal encryption keys stay on the device where " +
                            "they were written, so entries don't survive a " +
                            "reinstall.",
                        color = GraceRose, fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )
                    entry.entry.isBlank() -> Text(
                        "(no reflection written)",
                        color = GraceCreamDim, fontSize = 13.sp,
                        fontStyle = FontStyle.Italic
                    )
                    else -> Text(
                        entry.entry,
                        color = GraceCream, fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = GraceGoldDim, fontSize = 10.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Spacer(Modifier.height(4.dp))
}

private val dateFormatter = DateTimeFormatter
    .ofPattern("EEEE · MMM d, yyyy", Locale.getDefault())

private fun formatDate(date: LocalDate): String =
    date.format(dateFormatter).uppercase(Locale.getDefault())
