package com.grace.app.presentation.screens.mycontent

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.MeditationTheme
import com.grace.app.domain.model.Post
import com.grace.app.domain.model.Prayer
import com.grace.app.domain.model.PrayerStatus
import com.grace.app.presentation.theme.GraceBlue
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceOrange
import com.grace.app.presentation.theme.GracePurple
import com.grace.app.presentation.theme.GraceRose
import java.time.format.DateTimeFormatter

@Composable
fun MyContentScreen(
    onBack: () -> Unit,
    viewModel: MyContentViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var toast by remember { mutableStateOf<String?>(null) }
    var pendingEditPrayer by remember { mutableStateOf<Prayer?>(null) }
    var pendingDeletePrayerId by remember { mutableStateOf<String?>(null) }
    var pendingEditPost by remember { mutableStateOf<Post?>(null) }
    var pendingDeletePostId by remember { mutableStateOf<String?>(null) }

    if (toast != null) {
        LaunchedEffect(toast) {
            kotlinx.coroutines.delay(2500)
            toast = null
        }
    }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            toast = when (fx) {
                is MyContentEffect.ShowError -> "⚠ ${fx.message}"
                is MyContentEffect.ShowSuccess -> "✓ ${fx.message}"
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
            Column {
                Text("My Content 📝", color = GraceCream, fontSize = 24.sp)
                Text("Manage what you've shared", color = GraceCreamDim, fontSize = 12.sp)
            }
        }

        if (toast != null) {
            Spacer(Modifier.height(10.dp))
            Text(toast!!, color = if (toast!!.startsWith("✓")) GraceGreen else GraceRose)
        }

        Spacer(Modifier.height(14.dp))
        TabRow(active = state.activeTab) { tab ->
            viewModel.onEvent(MyContentEvent.TabChanged(tab))
        }

        Spacer(Modifier.height(12.dp))
        when {
            state.isLoading && state.prayers.isEmpty() && state.posts.isEmpty()
                && state.reflections.isEmpty() ->
                Box(Modifier.fillMaxWidth().height(160.dp), Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }

            state.activeTab == MyContentTab.PRAYERS && state.prayers.isEmpty() ->
                EmptyState("🙏", "You haven't shared a prayer yet.")

            state.activeTab == MyContentTab.POSTS && state.posts.isEmpty() ->
                EmptyState("🌿", "You haven't shared a post yet.")

            state.activeTab == MyContentTab.REFLECTIONS && state.reflections.isEmpty() ->
                EmptyState(
                    "📅",
                    "No meditation reflections yet. Open Devo → This Week's Meditation."
                )

            state.activeTab == MyContentTab.PRAYERS ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.prayers, key = { it.id }) { prayer ->
                        MyPrayerCard(
                            prayer = prayer,
                            onEdit = { pendingEditPrayer = prayer },
                            onDelete = { pendingDeletePrayerId = prayer.id },
                            onMarkAnswered = {
                                viewModel.onEvent(
                                    MyContentEvent.MarkPrayerAnswered(prayer.id)
                                )
                            }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }

            state.activeTab == MyContentTab.POSTS ->
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.posts, key = { it.id }) { post ->
                        MyPostCard(
                            post = post,
                            onEdit = { pendingEditPost = post },
                            onDelete = { pendingDeletePostId = post.id }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.reflections, key = { it.submission.id }) { item ->
                    MyReflectionCard(item)
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    pendingEditPrayer?.let { p ->
        EditDialog(
            title = "Edit prayer",
            initial = p.content,
            onDismiss = { pendingEditPrayer = null },
            onSave = { newText ->
                pendingEditPrayer = null
                viewModel.onEvent(MyContentEvent.UpdatePrayer(p.id, newText))
            }
        )
    }
    pendingEditPost?.let { p ->
        EditDialog(
            title = "Edit post",
            initial = p.content,
            onDismiss = { pendingEditPost = null },
            onSave = { newText ->
                pendingEditPost = null
                viewModel.onEvent(MyContentEvent.UpdatePost(p.id, newText))
            }
        )
    }
    pendingDeletePrayerId?.let { id ->
        ConfirmDelete(
            kind = "prayer",
            onDismiss = { pendingDeletePrayerId = null },
            onConfirm = {
                pendingDeletePrayerId = null
                viewModel.onEvent(MyContentEvent.DeletePrayer(id))
            }
        )
    }
    pendingDeletePostId?.let { id ->
        ConfirmDelete(
            kind = "post",
            onDismiss = { pendingDeletePostId = null },
            onConfirm = {
                pendingDeletePostId = null
                viewModel.onEvent(MyContentEvent.DeletePost(id))
            }
        )
    }
}

@Composable
private fun TabRow(active: MyContentTab, onSelect: (MyContentTab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TabPill("Prayers", active == MyContentTab.PRAYERS) {
            onSelect(MyContentTab.PRAYERS)
        }
        TabPill("Posts", active == MyContentTab.POSTS) {
            onSelect(MyContentTab.POSTS)
        }
        TabPill("Reflections", active == MyContentTab.REFLECTIONS) {
            onSelect(MyContentTab.REFLECTIONS)
        }
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) GraceDeepBlue else GraceCreamDim,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .background(
                if (selected) GraceGold else GraceCardBg,
                RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

private val reflectionDateFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
private fun MyReflectionCard(item: MyReflectionItem) {
    val meditation = item.meditation
    val themeColor = meditation?.theme?.let { themeColor(it) } ?: GraceCreamDim
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (meditation != null) {
                    Text(
                        meditation.theme.label,
                        color = themeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                themeColor.copy(alpha = 0.18f),
                                RoundedCornerShape(50)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.padding(horizontal = 6.dp))
                    Text(
                        "Week ${meditation.weekNumber}",
                        color = GraceCreamDim, fontSize = 11.sp
                    )
                } else {
                    Text(
                        "Past meditation",
                        color = GraceCreamDim, fontSize = 11.sp
                    )
                }
                Spacer(Modifier.padding(start = 0.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        item.submission.submittedAt.format(reflectionDateFmt),
                        color = GraceCreamDim,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    )
                }
            }
            if (meditation != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    meditation.title,
                    color = GraceCream, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${meditation.scriptureRef} — ${meditation.reflectionPrompt}",
                    color = GraceCreamDim,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    maxLines = 2
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                item.submission.reflectionText,
                color = GraceCream,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            if (item.submission.updatedAt != item.submission.submittedAt) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Edited",
                    color = GraceCreamDim, fontSize = 10.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun themeColor(theme: MeditationTheme) = when (theme) {
    MeditationTheme.JESUS -> GraceGold
    MeditationTheme.EDUCATION -> GraceBlue
    MeditationTheme.FAMILY -> GraceGreen
    MeditationTheme.FRIENDS -> GracePurple
    MeditationTheme.CHURCH -> GraceRose
    MeditationTheme.RELATIONSHIPS -> GraceOrange
}

@Composable
private fun EmptyState(emoji: String, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 44.sp)
        Spacer(Modifier.height(10.dp))
        Text(message, color = GraceCreamDim)
    }
}

@Composable
private fun MyPrayerCard(
    prayer: Prayer,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkAnswered: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isAnswered = prayer.status == PrayerStatus.ANSWERED
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAnswered) GraceGreen.copy(alpha = 0.12f) else GraceCardBg
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            prayer.category.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            color = GraceGold, fontSize = 11.sp,
                            modifier = Modifier
                                .background(
                                    GraceGold.copy(alpha = 0.18f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        if (prayer.isAnonymous) {
                            Spacer(Modifier.height(0.dp))
                            Text(
                                "  🕊️ Posted anonymously",
                                color = GraceCreamDim, fontSize = 10.sp
                            )
                        }
                        if (isAnswered) {
                            Spacer(Modifier.height(0.dp))
                            Text(
                                "  ✨ ANSWERED",
                                color = GraceGreen, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Box {
                    Text(
                        "⋮", color = GraceCreamDim, fontSize = 22.sp,
                        modifier = Modifier
                            .clickable { menuOpen = true }
                            .padding(horizontal = 6.dp)
                    )
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        if (!isAnswered) {
                            DropdownMenuItem(
                                text = { Text("Mark as answered") },
                                onClick = { menuOpen = false; onMarkAnswered() }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { menuOpen = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = GraceRose) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                prayer.content, color = GraceCream, fontSize = 15.sp,
                fontStyle = FontStyle.Italic
            )
            Spacer(Modifier.height(6.dp))
            Text("🙏 ${prayer.prayCount}", color = GraceGold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun MyPostCard(post: Post, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    post.verseRef?.let { ref ->
                        Text(
                            ref, color = GraceGold, fontSize = 11.sp,
                            modifier = Modifier
                                .background(
                                    GraceGold.copy(alpha = 0.15f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Box {
                    Text(
                        "⋮", color = GraceCreamDim, fontSize = 22.sp,
                        modifier = Modifier
                            .clickable { menuOpen = true }
                            .padding(horizontal = 6.dp)
                    )
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { menuOpen = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = GraceRose) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }
            Text(post.content, color = GraceCream, fontSize = 14.sp)
        }
    }
}

@Composable
private fun EditDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.trim().length >= 3,
                onClick = { onSave(text) }
            ) { Text("Save", color = GraceGold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ConfirmDelete(
    kind: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this $kind?") },
        text = { Text("This permanently removes it for everyone. Can't be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Delete", color = GraceRose) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
