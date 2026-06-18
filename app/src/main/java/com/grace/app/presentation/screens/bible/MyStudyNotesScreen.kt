package com.grace.app.presentation.screens.bible

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GraceRose

@Composable
fun MyStudyNotesScreen(
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    viewModel: MyStudyNotesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<StudyNoteListItem?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            when (fx) {
                is MyStudyNotesEffect.OpenNote -> onOpenNote(fx.id)
                is MyStudyNotesEffect.ShowError -> toast = fx.message
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.load() }

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
            Text(
                "My Study Notes 📝", color = GraceCream, fontSize = 24.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                "+ New",
                color = GraceDeepBlue,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(GraceGold, RoundedCornerShape(12.dp))
                    .clickable { showCreate = true }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        if (toast != null) {
            Spacer(Modifier.height(10.dp))
            Text("⚠ $toast", color = GraceRose, fontSize = 12.sp)
        }

        Spacer(Modifier.height(16.dp))
        when {
            state.isLoading -> Box(
                Modifier.fillMaxWidth().height(220.dp), Alignment.Center
            ) { CircularProgressIndicator(color = GraceGold) }

            state.error != null -> Text("⚠ ${state.error}", color = GraceRose)

            state.notes.isEmpty() -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📝", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text("No notes yet", color = GraceCream, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap + New for a sermon or Bible-study note, or open the Bible " +
                        "and use 📝 to take chapter notes.",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.notes, key = { it.id }) { note ->
                    NoteRow(
                        note = note,
                        onOpen = { onOpenNote(note.id) },
                        onDelete = { pendingDelete = note }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showCreate) {
        NewNoteDialog(
            onDismiss = { showCreate = false },
            onCreate = { title ->
                showCreate = false
                viewModel.createNote(title)
            }
        )
    }

    pendingDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete note?") },
            text = { Text("\"${note.title}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(note.id)
                    pendingDelete = null
                }) { Text("Delete", color = GraceRose) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NoteRow(
    note: StudyNoteListItem,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (note.isSession) "STUDY" else "CHAPTER",
                        color = GraceGold, fontSize = 8.sp,
                        letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(GraceMuted.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(Modifier.height(0.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(note.title, color = GraceCream, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (note.preview.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(note.preview, color = GraceCreamDim, fontSize = 12.sp, maxLines = 2)
                }
            }
            Text(
                "🗑",
                fontSize = 16.sp,
                modifier = Modifier.clickable { onDelete() }.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun NewNoteDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New study note") },
        text = {
            Column {
                Text(
                    "Give it a title — e.g. a sermon date or topic.",
                    color = GraceCreamDim, fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    singleLine = true,
                    placeholder = { Text("Sunday Sermon — June 15") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(title) }) { Text("Create", color = GraceGold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
