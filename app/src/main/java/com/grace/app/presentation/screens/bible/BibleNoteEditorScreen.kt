package com.grace.app.presentation.screens.bible

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.grace.app.presentation.components.biblepicker.BibleVersePicker
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceRose

@Composable
fun BibleNoteEditorScreen(
    onBack: () -> Unit,
    viewModel: BibleNoteEditorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            when (fx) {
                is BibleNoteEditorEffect.Closed -> onBack()
                is BibleNoteEditorEffect.ShowError -> toast = fx.message
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
            Text(
                if (state.isSession) "Study note" else state.title,
                color = GraceCream, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "🗑",
                fontSize = 18.sp,
                modifier = Modifier.clickable { viewModel.delete() }.padding(start = 8.dp)
            )
        }

        if (toast != null) {
            Spacer(Modifier.height(10.dp))
            Text("⚠ $toast", color = GraceRose, fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))
        when {
            state.isLoading -> Box(
                Modifier.fillMaxWidth().weight(1f), Alignment.Center
            ) { CircularProgressIndicator(color = GraceGold) }

            state.notFound -> Box(
                Modifier.fillMaxWidth().weight(1f), Alignment.Center
            ) { Text("This note no longer exists.", color = GraceCreamDim, fontSize = 13.sp) }

            else -> {
                if (state.isSession) {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = { viewModel.onTitleChanged(it) },
                        singleLine = true,
                        placeholder = { Text("Title (e.g. sermon date / topic)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = noteColors()
                    )
                    Spacer(Modifier.height(10.dp))
                }
                OutlinedTextField(
                    value = state.content,
                    onValueChange = { viewModel.onContentChanged(it) },
                    placeholder = { Text("Write your notes…") },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = noteColors()
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "📖 Add a verse",
                    color = GraceDeepBlue, fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GraceGold, RoundedCornerShape(12.dp))
                        .clickable { showPicker = true }
                        .padding(vertical = 14.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Notes save automatically.",
                    color = GraceCreamDim, fontSize = 11.sp
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showPicker) {
        BibleVersePicker(
            onDismiss = { showPicker = false },
            onInsert = { ref, text ->
                viewModel.addVerse(ref, text)
            }
        )
    }
}

@Composable
private fun noteColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GraceGold,
    unfocusedBorderColor = GraceCreamDim,
    focusedTextColor = GraceCream,
    unfocusedTextColor = GraceCream,
    cursorColor = GraceGold
)
