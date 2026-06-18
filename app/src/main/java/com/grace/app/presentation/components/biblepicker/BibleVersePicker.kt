package com.grace.app.presentation.components.biblepicker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.presentation.components.GraceButton
import com.grace.app.presentation.theme.GraceCardAlt
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleVersePicker(
    onDismiss: () -> Unit,
    onInsert: (reference: String, text: String) -> Unit,
    viewModel: BiblePickerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GraceCardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.selectedBook != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = GraceGold,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { viewModel.back() }
                    )
                    Spacer(Modifier.size(10.dp))
                }
                Text(
                    text = headerText(state),
                    color = GraceCream,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))

            when {
                state.isLoading -> Box(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = GraceGold) }

                state.error != null -> Box(
                    Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) { Text("⚠ ${state.error}", color = GraceCreamDim, fontSize = 13.sp) }

                state.selectedBook == null ->
                    BookList(state, Modifier.weight(1f)) { viewModel.selectBook(it) }

                state.selectedChapter == null ->
                    ChapterGrid(state, Modifier.weight(1f)) { viewModel.selectChapter(it) }

                else -> {
                    VerseList(state, Modifier.weight(1f)) { viewModel.toggleVerse(it) }
                    InsertBar(
                        reference = viewModel.currentReference(),
                        enabled = state.canInsert
                    ) {
                        viewModel.buildSelection()?.let { (ref, text) ->
                            onInsert(ref, text)
                            onDismiss()
                        }
                    }
                }
            }
        }
    }
}

private fun headerText(state: BiblePickerUiState): String = when {
    state.selectedBook == null -> "📖 Add a Bible verse (KJV)"
    state.selectedChapter == null -> "${state.selectedBook.name} — choose a chapter"
    else -> "${state.selectedBook.name} ${state.selectedChapter}"
}

@Composable
private fun BookList(
    state: BiblePickerUiState,
    modifier: Modifier,
    onPick: (com.grace.app.domain.model.BibleBook) -> Unit
) {
    val ot = state.books.filter { it.testament == "OT" }
    val nt = state.books.filter { it.testament == "NT" }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item { SectionHeader("OLD TESTAMENT") }
        items(ot, key = { it.order }) { BookRow(it, onPick) }
        item { SectionHeader("NEW TESTAMENT") }
        items(nt, key = { it.order }) { BookRow(it, onPick) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        color = GraceGold,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun BookRow(
    book: com.grace.app.domain.model.BibleBook,
    onPick: (com.grace.app.domain.model.BibleBook) -> Unit
) {
    Text(
        book.name,
        color = GraceCream,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPick(book) }
            .padding(vertical = 12.dp)
    )
}

@Composable
private fun ChapterGrid(
    state: BiblePickerUiState,
    modifier: Modifier,
    onPick: (Int) -> Unit
) {
    val count = state.selectedBook?.chapterCount ?: 0
    LazyVerticalGrid(
        columns = GridCells.Adaptive(56.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items((1..count).toList()) { ch ->
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(GraceCardAlt, RoundedCornerShape(12.dp))
                    .clickable { onPick(ch) },
                contentAlignment = Alignment.Center
            ) {
                Text("$ch", color = GraceCream, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VerseList(
    state: BiblePickerUiState,
    modifier: Modifier,
    onToggle: (Int) -> Unit
) {
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(state.verses, key = { it.verse }) { v ->
            val selected = v.verse in state.selectedVerses
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (selected) GraceGold.copy(alpha = 0.18f) else GraceCardAlt,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onToggle(v.verse) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    "${v.verse}",
                    color = if (selected) GraceGold else GraceCreamDim,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.size(width = 24.dp, height = 20.dp)
                )
                Text(
                    v.text,
                    color = GraceCream,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun InsertBar(reference: String?, enabled: Boolean, onInsert: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Text(
        reference ?: "Tap verses to select",
        color = if (reference != null) GraceGreen else GraceCreamDim,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(8.dp))
    GraceButton(
        text = "Insert verse",
        onClick = onInsert,
        enabled = enabled,
        containerColor = GraceGold,
        contentColor = GraceDeepBlue
    )
}
