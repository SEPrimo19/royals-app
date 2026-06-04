package com.grace.app.presentation.screens.games.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.grace.app.domain.model.BiblePassage
import com.grace.app.domain.model.BibleQuestion
import com.grace.app.presentation.theme.GraceCardAlt
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGoldDim
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GracePurple
import com.grace.app.presentation.theme.GraceRose

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageContentScreen(
    onBack: () -> Unit,
    onNewQuestion: () -> Unit,
    onEditQuestion: (String) -> Unit,
    onNewPassage: () -> Unit,
    onEditPassage: (String) -> Unit,
    viewModel: ManageContentViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { eff ->
            when (eff) {
                is ManageContentEffect.ShowError -> snackbar.showSnackbar(eff.message)
                is ManageContentEffect.ShowToast -> snackbar.showSnackbar(eff.message)
            }
        }
    }

    Scaffold(
        containerColor = GraceDeepBlue,
        topBar = {
            TopAppBar(
                title = {
                    Text("Manage Questions",
                        color = GraceCream, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                            tint = GraceCream)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GraceDeepBlue
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (state.tab) {
                        ManageTab.TRIVIA -> onNewQuestion()
                        ManageTab.VERSES -> onNewPassage()
                    }
                },
                containerColor = GraceGold,
                contentColor = GraceDeepBlue
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            TabRowSegmented(
                current = state.tab,
                onSelected = { viewModel.onEvent(ManageContentEvent.TabChanged(it)) }
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }
                return@Column
            }

            when (state.tab) {
                ManageTab.TRIVIA -> QuestionsList(
                    items = state.questions,
                    onToggle = { id, v ->
                        viewModel.onEvent(ManageContentEvent.ToggleQuestionActive(id, v))
                    },
                    onEdit = onEditQuestion
                )
                ManageTab.VERSES -> PassagesList(
                    items = state.passages,
                    onToggle = { id, v ->
                        viewModel.onEvent(ManageContentEvent.TogglePassageActive(id, v))
                    },
                    onEdit = onEditPassage
                )
            }
        }
    }
}

@Composable
private fun TabRowSegmented(
    current: ManageTab,
    onSelected: (ManageTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(GraceCardBg)
            .padding(4.dp)
    ) {
        SegPill("Trivia", current == ManageTab.TRIVIA, Modifier.weight(1f)) {
            onSelected(ManageTab.TRIVIA)
        }
        SegPill("Daily Verse", current == ManageTab.VERSES, Modifier.weight(1f)) {
            onSelected(ManageTab.VERSES)
        }
    }
}

@Composable
private fun SegPill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) GraceGold else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) GraceDeepBlue else GraceCreamDim,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun QuestionsList(
    items: List<BibleQuestion>,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (String) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState("No trivia yet.\nTap + to add the first question.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.id }) { q ->
            QuestionRow(q, onToggle, onEdit)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun QuestionRow(
    q: BibleQuestion,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GraceCardAlt)
            .border(
                width = 1.dp,
                color = if (q.isActive) Color.Transparent else GraceRose.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onEdit(q.id) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DifficultyChip(q.difficulty.name)
                Spacer(Modifier.width(6.dp))
                CategoryChip(q.category.name)
                if (!q.isActive) {
                    Spacer(Modifier.width(6.dp))
                    Text("HIDDEN",
                        color = GraceRose, fontSize = 9.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                q.question,
                color = GraceCream,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            q.sourceRef?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, color = GraceGoldDim, fontSize = 11.sp,
                    fontStyle = FontStyle.Italic)
            }
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = q.isActive,
            onCheckedChange = { onToggle(q.id, it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = GraceGold,
                checkedTrackColor = GraceGold.copy(alpha = 0.4f),
                uncheckedThumbColor = GraceMuted,
                uncheckedTrackColor = GraceMuted.copy(alpha = 0.4f)
            )
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.Edit, contentDescription = "Edit",
            tint = GraceCreamDim, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PassagesList(
    items: List<BiblePassage>,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (String) -> Unit
) {
    if (items.isEmpty()) {
        EmptyState("No verses yet.\nTap + to add the first Daily Verse.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(items, key = { it.id }) { p ->
            PassageRow(p, onToggle, onEdit)
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun PassageRow(
    p: BiblePassage,
    onToggle: (String, Boolean) -> Unit,
    onEdit: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GraceCardAlt)
            .border(
                width = 1.dp,
                color = if (p.isActive) Color.Transparent else GraceRose.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onEdit(p.id) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.reference,
                    color = GraceGold, fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp)
                Spacer(Modifier.width(8.dp))
                Text("blank: ${p.blankWord}",
                    color = GracePurple, fontSize = 11.sp,
                    fontWeight = FontWeight.Medium)
                if (!p.isActive) {
                    Spacer(Modifier.width(6.dp))
                    Text("HIDDEN",
                        color = GraceRose, fontSize = 9.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                p.text,
                color = GraceCream,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = p.isActive,
            onCheckedChange = { onToggle(p.id, it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = GraceGold,
                checkedTrackColor = GraceGold.copy(alpha = 0.4f),
                uncheckedThumbColor = GraceMuted,
                uncheckedTrackColor = GraceMuted.copy(alpha = 0.4f)
            )
        )
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.Edit, contentDescription = "Edit",
            tint = GraceCreamDim, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DifficultyChip(difficulty: String) {
    val color = when (difficulty) {
        "EASY" -> GraceGreen
        "MEDIUM" -> GraceGold
        "HARD" -> GraceRose
        else -> GraceMuted
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(difficulty, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryChip(category: String) {
    val label = when (category) {
        "OLD_TESTAMENT" -> "OT"
        "NEW_TESTAMENT" -> "NT"
        "CHARACTER" -> "Character"
        else -> category
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(GracePurple.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = GracePurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = GraceCreamDim,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}
