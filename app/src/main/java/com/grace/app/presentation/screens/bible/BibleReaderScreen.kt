package com.grace.app.presentation.screens.bible

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.BibleBook
import com.grace.app.presentation.theme.DisplayFont
import com.grace.app.presentation.theme.GraceCardAlt
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.ScriptureFont

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BibleReaderScreen(
    onBack: () -> Unit,
    viewModel: BibleReaderViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var selectorOpen by remember { mutableStateOf(false) }
    var searchOpen by remember { mutableStateOf(false) }
    var shareContent by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(state.verses, state.scrollTargetVerse) {
        val target = state.scrollTargetVerse
        if (target != null) {
            val idx = state.verses.indexOfFirst { it.verse == target }
            listState.scrollToItem(if (idx >= 0) idx + 1 else 0)
        } else {
            listState.scrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = GraceCream,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBack() }
            )
            Spacer(Modifier.width(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectorOpen = true }
            ) {
                Text(
                    state.title,
                    color = GraceCream,
                    fontFamily = DisplayFont,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Change book or chapter",
                    tint = GraceGold,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                "📝",
                fontSize = 18.sp,
                modifier = Modifier
                    .then(
                        if (state.notesPaneOpen) Modifier.background(
                            GraceGold.copy(alpha = 0.2f), RoundedCornerShape(8.dp)
                        ) else Modifier
                    )
                    .clickable { viewModel.toggleNotesPane() }
                    .padding(4.dp)
            )
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Filled.Search,
                contentDescription = "Search the Bible",
                tint = GraceCream,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { searchOpen = true }
            )
            Spacer(Modifier.width(10.dp))
            Text("KJV", color = GraceCreamDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        if (state.notesPaneOpen) {
            Column(
                modifier = Modifier
                    .weight(0.7f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 2.dp, bottom = 8.dp)
            ) {
                Text(
                    "MY NOTES · ${state.title}",
                    color = GraceGold, fontSize = 10.sp,
                    letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = state.noteContent,
                    onValueChange = { viewModel.onNoteChanged(it) },
                    placeholder = {
                        Text(
                            "Write your notes — tap a verse below to add it here.",
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GraceGold,
                        unfocusedBorderColor = GraceCreamDim,
                        focusedTextColor = GraceCream,
                        unfocusedTextColor = GraceCream,
                        cursorColor = GraceGold
                    )
                )
            }
            Box(
                Modifier.fillMaxWidth().height(1.dp)
                    .background(GraceCreamDim.copy(alpha = 0.15f))
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }
                state.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("⚠ ${state.error}", color = GraceCreamDim, fontSize = 13.sp)
                }
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(Modifier.height(4.dp)) }
                    items(state.verses, key = { it.verse }) { v ->
                        val isSelected = state.inSelectionMode && v.verse in state.imageSelection
                        val isHighlight = v.verse in state.highlights
                        val isSearchHit = v.verse == state.scrollTargetVerse
                        val bg = when {
                            isSelected -> GraceGold.copy(alpha = 0.38f)
                            isHighlight -> GraceGold.copy(alpha = 0.22f)
                            isSearchHit -> GraceGold.copy(alpha = 0.12f)
                            else -> null
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (bg != null) Modifier.background(bg, RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                                .combinedClickable(
                                    onClick = { viewModel.onVerseTap(v) },
                                    onLongClick = { viewModel.onVerseLongPress(v) }
                                )
                                .padding(horizontal = if (bg != null) 6.dp else 0.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "${v.verse}",
                                color = GraceGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .width(22.dp)
                                    .padding(end = 8.dp, top = 4.dp)
                            )
                            Text(
                                v.text,
                                color = GraceCream,
                                fontFamily = ScriptureFont,
                                fontSize = 18.sp,
                                lineHeight = 28.sp
                            )
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }

        if (state.inSelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "✕",
                    color = GraceCream, fontSize = 18.sp,
                    modifier = Modifier.clickable { viewModel.exitSelection() }
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    "${state.imageSelection.size} verse" +
                        (if (state.imageSelection.size == 1) "" else "s") + " selected",
                    color = GraceCreamDim, fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "🖼 Create image",
                    color = GraceDeepBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    modifier = Modifier
                        .background(GraceGold, RoundedCornerShape(50))
                        .clickable {
                            shareContent = viewModel.buildSelectionForImage()
                            viewModel.exitSelection()
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "‹ Prev",
                    color = if (state.hasPrev) GraceGold else GraceMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = state.hasPrev) { viewModel.prevChapter() }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Next ›",
                    color = if (state.hasNext) GraceGold else GraceMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = state.hasNext) { viewModel.nextChapter() }
                )
            }
        }
    }

    if (selectorOpen) {
        ChapterSelectorSheet(
            books = state.books,
            onDismiss = { selectorOpen = false },
            onPick = { bookOrder, chapter ->
                viewModel.jumpTo(bookOrder, chapter)
                selectorOpen = false
            }
        )
    }

    if (searchOpen) {
        SearchSheet(
            state = state,
            onQueryChange = viewModel::onSearchQueryChanged,
            onResultTap = {
                viewModel.openSearchResult(it)
                searchOpen = false
            },
            onDismiss = {
                searchOpen = false
                viewModel.clearSearch()
            }
        )
    }

    state.selectedVerse?.let { v ->
        val clipboard = LocalClipboardManager.current
        val reference = "${state.currentBook?.name ?: ""} ${state.chapter}:${v.verse}"
        VerseActionSheet(
            reference = reference,
            verse = v,
            isHighlighted = v.verse in state.highlights,
            onCopy = {
                clipboard.setText(AnnotatedString("\"${v.text}\"\n— $reference (KJV)"))
                viewModel.dismissVerseSheet()
            },
            onToggleHighlight = { viewModel.toggleHighlight(v.verse) },
            onAddToNote = { viewModel.addVerseToNote(v) },
            onShareImage = {
                shareContent = v.text to
                    "${state.currentBook?.name ?: ""} ${state.chapter}:${v.verse}"
                viewModel.dismissVerseSheet()
            },
            onDismiss = { viewModel.dismissVerseSheet() }
        )
    }

    shareContent?.let { (text, ref) ->
        VerseImageEditor(
            verseText = text,
            reference = ref,
            onClose = { shareContent = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerseActionSheet(
    reference: String,
    verse: com.grace.app.domain.model.BibleVerse,
    isHighlighted: Boolean,
    onCopy: () -> Unit,
    onToggleHighlight: () -> Unit,
    onAddToNote: () -> Unit,
    onShareImage: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = GraceCardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(reference, color = GraceGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                verse.text,
                color = GraceCream,
                fontFamily = ScriptureFont,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(18.dp))
            VerseAction("📋", "Copy", onCopy)
            VerseAction(
                if (isHighlighted) "✕" else "✨",
                if (isHighlighted) "Remove highlight" else "Highlight",
                onToggleHighlight
            )
            VerseAction("📝", "Add to my notes", onAddToNote)
            VerseAction("🖼", "Share as image", onShareImage)
        }
    }
}

@Composable
private fun VerseAction(emoji: String, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp)
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(14.dp))
        Text(label, color = GraceCream, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchSheet(
    state: BibleReaderUiState,
    onQueryChange: (String) -> Unit,
    onResultTap: (com.grace.app.domain.model.BibleSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
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
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Search the Bible (KJV)…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GraceGold,
                    unfocusedBorderColor = GraceCreamDim,
                    focusedTextColor = GraceCream,
                    unfocusedTextColor = GraceCream,
                    cursorColor = GraceGold
                )
            )
            Spacer(Modifier.height(10.dp))
            when {
                state.isSearching -> Box(
                    Modifier.fillMaxWidth().weight(1f), Alignment.Center
                ) { CircularProgressIndicator(color = GraceGold) }

                state.searchQuery.trim().length < 2 -> Box(
                    Modifier.fillMaxWidth().weight(1f), Alignment.Center
                ) { Text("Type a word or phrase to search.", color = GraceCreamDim, fontSize = 13.sp) }

                state.searchResults.isEmpty() -> Box(
                    Modifier.fillMaxWidth().weight(1f), Alignment.Center
                ) { Text("No verses found.", color = GraceCreamDim, fontSize = 13.sp) }

                else -> {
                    Text(
                        "${state.searchResults.size} result" +
                            if (state.searchResults.size == 1) "" else "s",
                        color = GraceCreamDim, fontSize = 10.sp,
                        letterSpacing = 2.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(
                            state.searchResults,
                            key = { "${it.bookOrder}-${it.chapter}-${it.verse}" }
                        ) { r ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GraceCardAlt, RoundedCornerShape(10.dp))
                                    .clickable { onResultTap(r) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    r.reference,
                                    color = GraceGold, fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    r.text, color = GraceCream,
                                    fontSize = 13.sp, lineHeight = 19.sp, maxLines = 3
                                )
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterSelectorSheet(
    books: List<BibleBook>,
    onDismiss: () -> Unit,
    onPick: (bookOrder: Int, chapter: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selBook by remember { mutableStateOf<BibleBook?>(null) }

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
                if (selBook != null) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to books",
                        tint = GraceGold,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { selBook = null }
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Text(
                    selBook?.let { "${it.name} — choose a chapter" } ?: "Choose a book",
                    color = GraceCream,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))

            val book = selBook
            if (book == null) {
                val ot = books.filter { it.testament == "OT" }
                val nt = books.filter { it.testament == "NT" }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    item { SheetSection("OLD TESTAMENT") }
                    items(ot, key = { it.order }) { b -> BookRow(b) { selBook = b } }
                    item { SheetSection("NEW TESTAMENT") }
                    items(nt, key = { it.order }) { b -> BookRow(b) { selBook = b } }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(56.dp),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items((1..book.chapterCount).toList()) { ch ->
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(GraceCardAlt, RoundedCornerShape(12.dp))
                                .clickable { onPick(book.order, ch) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$ch",
                                color = GraceCream,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetSection(text: String) {
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
private fun BookRow(book: BibleBook, onTap: () -> Unit) {
    Text(
        book.name,
        color = GraceCream,
        fontSize = 15.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(vertical = 12.dp)
    )
}
