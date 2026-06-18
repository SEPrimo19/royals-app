package com.grace.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import com.grace.app.domain.model.UserNote
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GraceRose

@Composable
fun NotesBar(viewModel: NotesBarViewModel = hiltViewModel()) {
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
                is NotesBarEffect.Toast -> toast = fx.message to fx.isError
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "NOTES · 24H",
                color = GraceCreamDim, fontSize = 9.sp,
                letterSpacing = 2.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.size(8.dp))

        if (toast != null) {
            Text(
                (if (toast!!.second) "⚠ " else "✓ ") + toast!!.first,
                color = if (toast!!.second) GraceRose else GraceGold,
                fontSize = 11.sp
            )
            Spacer(Modifier.size(6.dp))
        }

        val others = state.notes.filter { it.userId != state.myUserId }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "me") {
                MyNoteSlot(
                    myNote = state.myNote,
                    onTap = {
                        val existing = state.myNote
                        if (existing == null) {
                            viewModel.onEvent(NotesBarEvent.OpenComposer)
                        } else {
                            viewModel.onEvent(NotesBarEvent.ViewNote(existing))
                        }
                    }
                )
            }
            items(others, key = { it.userId }) { n ->
                FriendNoteSlot(
                    note = n,
                    onTap = { viewModel.onEvent(NotesBarEvent.ViewNote(n)) }
                )
            }
            if (state.isLoading && state.notes.isEmpty()) {
                item("loading") {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(GraceCardBg, RoundedCornerShape(16.dp))
                    )
                }
            }
        }
    }

    if (state.composerOpen) {
        ComposerDialog(
            initial = state.myNote?.content.orEmpty(),
            hasExisting = state.myNote != null,
            onDismiss = { viewModel.onEvent(NotesBarEvent.CloseComposer) },
            onPost = { viewModel.onEvent(NotesBarEvent.PostNote(it)) },
            onDelete = { viewModel.onEvent(NotesBarEvent.DeleteMyNote) }
        )
    }

    state.viewing?.let { note ->
        val isMine = note.userId == state.myUserId
        ViewerDialog(
            note = note,
            isMine = isMine,
            canModerate = state.isLeaderTier && !isMine,
            onDismiss = { viewModel.onEvent(NotesBarEvent.DismissViewer) },
            onHeart = { viewModel.onEvent(NotesBarEvent.ToggleHeart(note.userId)) },
            onHide = { viewModel.onEvent(NotesBarEvent.HideNote(note.userId)) },
            onReplace = {
                viewModel.onEvent(NotesBarEvent.DismissViewer)
                viewModel.onEvent(NotesBarEvent.OpenComposer)
            },
            onDelete = {
                viewModel.onEvent(NotesBarEvent.DismissViewer)
                viewModel.onEvent(NotesBarEvent.DeleteMyNote)
            }
        )
    }
}

private val SLOT_WIDTH = 84.dp
private val BUBBLE_AREA_HEIGHT = 68.dp
private val HEART_ROW_HEIGHT = 12.dp

@Composable
private fun MyNoteSlot(myNote: UserNote?, onTap: () -> Unit) {
    Column(
        modifier = Modifier
            .width(SLOT_WIDTH)
            .clickable { onTap() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BUBBLE_AREA_HEIGHT),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (myNote == null) {
                AddNotePill()
            } else {
                NoteBubble(text = myNote.content, accent = GraceGold)
            }
        }
        Spacer(Modifier.size(2.dp))
        BubbleTail(color = if (myNote == null) GraceMuted else GraceCardBg)
        Spacer(Modifier.size(4.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    if (myNote == null) GraceMuted.copy(alpha = 0.4f)
                    else GraceGold.copy(alpha = 0.30f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "You", color = GraceGold, fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.size(4.dp))
        Text(
            if (myNote == null) "Add note" else "You",
            color = GraceCream, fontSize = 10.sp,
            maxLines = 1, fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(HEART_ROW_HEIGHT))
    }
}

@Composable
private fun FriendNoteSlot(note: UserNote, onTap: () -> Unit) {
    Column(
        modifier = Modifier
            .width(SLOT_WIDTH)
            .clickable { onTap() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BUBBLE_AREA_HEIGHT),
            contentAlignment = Alignment.BottomCenter
        ) {
            NoteBubble(text = note.content, accent = GraceCream)
        }
        Spacer(Modifier.size(2.dp))
        BubbleTail(color = GraceCardBg)
        Spacer(Modifier.size(4.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(GraceMuted.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                note.userName.firstOrNull()?.uppercase() ?: "?",
                color = GraceCream, fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.size(4.dp))
        Text(
            shortName(note.userName),
            color = GraceCream, fontSize = 10.sp,
            maxLines = 1, fontWeight = FontWeight.SemiBold
        )
        Box(
            modifier = Modifier.height(HEART_ROW_HEIGHT),
            contentAlignment = Alignment.Center
        ) {
            if (note.heartCount > 0) {
                Text(
                    "🩷 ${note.heartCount}",
                    color = GraceCreamDim, fontSize = 9.sp
                )
            }
        }
    }
}

private fun shortName(full: String): String {
    val firstNameish = if (full.contains(',')) {
        full.substringAfter(',').trim().substringBefore(' ')
    } else {
        full.substringBefore(' ')
    }
    return firstNameish.trimEnd(',', '.', ';', ':').ifEmpty {
        full.substringBefore(',').trim().ifEmpty { full }
    }
}

@Composable
private fun NoteBubble(text: String, accent: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GraceCardBg, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            color = accent,
            fontSize = 10.sp,
            lineHeight = 13.sp,
            maxLines = 3,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AddNotePill() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(GraceMuted.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "+",
            color = GraceGold,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BubbleTail(color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.Canvas(
        modifier = Modifier.size(width = 10.dp, height = 6.dp)
    ) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, size.height)
            close()
        }
        drawPath(path, color = color)
    }
}

@Composable
private fun ComposerDialog(
    initial: String,
    hasExisting: Boolean,
    onDismiss: () -> Unit,
    onPost: (String) -> Unit,
    onDelete: () -> Unit
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasExisting) "Update your note" else "Add a note · 24h") },
        text = {
            Column {
                Text(
                    "Share a short thought, prayer, or what's on your mind. " +
                        "Visible to the whole church for 24 hours.",
                    color = GraceCreamDim, fontSize = 12.sp
                )
                Spacer(Modifier.size(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(200) },
                    placeholder = {
                        Text("Type your note…", color = GraceCreamDim, fontSize = 13.sp)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    "${text.length}/200",
                    color = GraceCreamDim, fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = text.trim().isNotEmpty(),
                onClick = { onPost(text) }
            ) { Text("Post", color = GraceGold) }
        },
        dismissButton = {
            if (hasExisting) {
                TextButton(onClick = onDelete) {
                    Text("Remove", color = GraceRose)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun ViewerDialog(
    note: UserNote,
    isMine: Boolean,
    canModerate: Boolean,
    onDismiss: () -> Unit,
    onHeart: () -> Unit,
    onHide: () -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(GraceDeepBlue)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                GraceMuted.copy(alpha = 0.6f), CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            note.userName.firstOrNull()?.uppercase() ?: "?",
                            color = GraceCream, fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Text(
                        note.userName,
                        color = GraceCream, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        relativeTime(note.createdAt),
                        color = GraceCreamDim, fontSize = 12.sp,
                        modifier = Modifier.padding(end = 10.dp)
                    )
                    Text(
                        "✕",
                        color = GraceCream, fontSize = 22.sp,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Spacer(Modifier.size(48.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(min = 140.dp, max = 280.dp)
                            .background(
                                GraceCardBg,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Text(
                            note.content,
                            color = GraceCream, fontSize = 16.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.size(4.dp))
                    BubbleTail(color = GraceCardBg)
                    Spacer(Modifier.size(10.dp))
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(
                                if (isMine) GraceGold.copy(alpha = 0.3f)
                                else GraceMuted.copy(alpha = 0.6f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            note.userName.firstOrNull()?.uppercase() ?: "?",
                            color = GraceCream,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(
                        note.userName,
                        color = GraceCream, fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        "Shared with Church",
                        color = GraceCreamDim, fontSize = 13.sp
                    )
                    Spacer(Modifier.size(2.dp))
                    Text(
                        "Expires in ${remainingLifetime(note.expiresAt)}",
                        color = GraceCreamDim, fontSize = 12.sp
                    )
                    if (note.heartCount > 0) {
                        Spacer(Modifier.size(8.dp))
                        Text(
                            "🩷 ${note.heartCount}",
                            color = GraceCreamDim, fontSize = 13.sp
                        )
                    }

                    Spacer(Modifier.size(28.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isMine) {
                            ViewerAction(
                                label = "Delete",
                                iconText = "🗑",
                                tint = GraceRose,
                                onClick = onDelete
                            )
                            ViewerAction(
                                label = "Replace note",
                                iconText = "+",
                                tint = GraceGold,
                                onClick = onReplace
                            )
                        } else {
                            ViewerAction(
                                label = if (note.hasMyHeart) "Hearted" else "Heart",
                                iconText = "🩷",
                                tint = if (note.hasMyHeart) GraceGold else GraceCream,
                                onClick = onHeart
                            )
                            if (canModerate) {
                                ViewerAction(
                                    label = "Hide",
                                    iconText = "⊘",
                                    tint = GraceRose,
                                    onClick = onHide
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewerAction(
    label: String,
    iconText: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    tint.copy(alpha = 0.18f), CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(iconText, fontSize = 20.sp, color = tint)
        }
        Spacer(Modifier.size(6.dp))
        Text(
            label,
            color = GraceCream, fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun relativeTime(t: java.time.LocalDateTime): String {
    val d = java.time.Duration.between(t, java.time.LocalDateTime.now())
    return when {
        d.toMinutes() < 1 -> "Just now"
        d.toMinutes() < 60 -> "${d.toMinutes()}m ago"
        d.toHours() < 24 -> "${d.toHours()}h ago"
        else -> "1d+ ago"
    }
}

private fun remainingLifetime(expiresAt: java.time.LocalDateTime): String {
    val d = java.time.Duration.between(java.time.LocalDateTime.now(), expiresAt)
    return when {
        d.isNegative || d.isZero -> "less than a minute"
        d.toHours() > 0 -> "${d.toHours()}h"
        d.toMinutes() > 0 -> "${d.toMinutes()}m"
        else -> "${d.seconds}s"
    }
}

