package com.grace.app.presentation.screens.discipleship

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.ActivityCategory
import com.grace.app.domain.model.DiscipleshipActivity
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GraceRose

@Composable
fun DiscipleshipLibraryScreen(
    onBack: () -> Unit,
    onOpenAuthor: (String?) -> Unit,
    viewModel: DiscipleshipLibraryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var toast by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    if (toast != null) {
        LaunchedEffect(toast) {
            kotlinx.coroutines.delay(2500)
            toast = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            when (fx) {
                is LibraryEffect.Toast -> toast = fx.message to fx.isError
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←", color = GraceCream, fontSize = 22.sp,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Discipleship 🌱", color = GraceCream, fontSize = 24.sp)
                Text(
                    "Daily practices to grow your faith",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
            if (state.canManage) {
                Text(
                    "+ New",
                    color = GraceGold, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(
                            GraceGold.copy(alpha = 0.18f),
                            RoundedCornerShape(50)
                        )
                        .clickable { onOpenAuthor(null) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item("all") {
                Chip(
                    label = "All",
                    selected = state.filter == null,
                    onClick = { viewModel.onEvent(LibraryEvent.FilterChanged(null)) }
                )
            }
            items(ActivityCategory.entries.toList(), key = { it.slug }) { c ->
                Chip(
                    label = "${c.emoji} ${c.label}",
                    selected = state.filter == c,
                    onClick = { viewModel.onEvent(LibraryEvent.FilterChanged(c)) }
                )
            }
        }

        if (toast != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                (if (toast!!.second) "⚠ " else "✓ ") + toast!!.first,
                color = if (toast!!.second) GraceRose else GraceGreen,
                fontSize = 11.sp
            )
        }
        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text("⚠ ${state.error}", color = GraceRose, fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))
        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }
            state.visible.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.filter == null) "No activities yet."
                        else "No activities in this category yet.",
                        color = GraceCreamDim
                    )
                }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.visible, key = { it.id }) { a ->
                    ActivityRow(
                        activity = a,
                        isDoneToday = a.id in state.completedTodayIds,
                        canManage = state.canManage,
                        onMarkDone = {
                            viewModel.onEvent(LibraryEvent.MarkDone(a.id))
                        },
                        onEdit = { onOpenAuthor(a.id) },
                        onDelete = {
                            viewModel.onEvent(LibraryEvent.PromptDelete(a))
                        }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    state.pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(LibraryEvent.CancelDelete) },
            title = { Text("Remove activity?") },
            text = {
                Text(
                    "\"${target.title}\" will no longer appear in the picker " +
                        "or library. Members' past completions are preserved.",
                    color = GraceCreamDim, fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(LibraryEvent.ConfirmDelete) }) {
                    Text("Remove", color = GraceRose)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(LibraryEvent.CancelDelete) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) GraceDeepBlue else GraceCream,
        fontSize = 11.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        modifier = Modifier
            .background(
                if (selected) GraceGold else GraceCardBg,
                RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun ActivityRow(
    activity: DiscipleshipActivity,
    isDoneToday: Boolean,
    canManage: Boolean,
    onMarkDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(activity.category.emoji, fontSize = 20.sp)
                Spacer(Modifier.padding(start = 8.dp))
                Text(
                    activity.category.label.uppercase(),
                    color = GraceCreamDim, fontSize = 9.sp,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    activity.durationTag.label,
                    color = GraceCreamDim, fontSize = 10.sp,
                    modifier = Modifier
                        .background(
                            GraceMuted.copy(alpha = 0.4f),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.padding(top = 6.dp))
            Text(
                activity.title, color = GraceCream, fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.padding(top = 4.dp))
            Text(
                activity.description, color = GraceCreamDim, fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Spacer(Modifier.padding(top = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isDoneToday) {
                    Text(
                        "✓ Done today",
                        color = GraceGreen, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                GraceGreen.copy(alpha = 0.30f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                } else {
                    Text(
                        "Mark done ✓",
                        color = GraceGreen, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                GraceGreen.copy(alpha = 0.18f),
                                RoundedCornerShape(50)
                            )
                            .clickable { onMarkDone() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                if (canManage) {
                    Text(
                        "Edit",
                        color = GraceGold, fontSize = 11.sp,
                        modifier = Modifier
                            .background(
                                GraceGold.copy(alpha = 0.12f),
                                RoundedCornerShape(50)
                            )
                            .clickable { onEdit() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    Text(
                        "Remove",
                        color = GraceRose, fontSize = 11.sp,
                        modifier = Modifier
                            .background(
                                GraceRose.copy(alpha = 0.12f),
                                RoundedCornerShape(50)
                            )
                            .clickable { onDelete() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
