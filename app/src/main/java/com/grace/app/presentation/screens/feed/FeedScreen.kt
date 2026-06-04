package com.grace.app.presentation.screens.feed

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.presentation.components.GraceButton
import com.grace.app.presentation.components.GracePullToRefresh
import com.grace.app.presentation.components.MenuButtonRow
import com.grace.app.presentation.components.PostCard
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceRose

@Composable
fun FeedScreen(
    onOpenMenu: () -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Re-fetch + reconcile on every screen entry — the VM persists across tabs,
    // so init only fires once and we'd otherwise see stale counts / deleted posts.
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.refresh() }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            if (fx is FeedEffect.ShowError) viewModel.surfaceError(fx.message)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        viewModel.onEvent(FeedEvent.ImagePicked(uri?.toString()))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
    ) {
        // MenuButtonRow owns its 16dp-from-edge contract — must sit OUTSIDE
        // the horizontally-padded content column below.
        MenuButtonRow(onOpenMenu)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Life Feed 🌿", color = GraceCream, fontSize = 26.sp)
            Text(
                "+ Share",
                color = GraceDeepBlue,
                modifier = Modifier
                    .background(GraceGreen, RoundedCornerShape(12.dp))
                    .clickable { viewModel.onEvent(FeedEvent.ToggleCompose) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        AnimatedVisibility(visible = state.showCompose) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                OutlinedTextField(
                    value = state.draftText,
                    onValueChange = { viewModel.onEvent(FeedEvent.DraftTextChanged(it)) },
                    placeholder = { Text("What is God teaching you today?") },
                    modifier = Modifier.fillMaxWidth().height(110.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GraceGreen,
                        unfocusedBorderColor = GraceCreamDim,
                        focusedTextColor = GraceCream,
                        unfocusedTextColor = GraceCream,
                        cursorColor = GraceGreen
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.draftVerseRef ?: "",
                    onValueChange = { viewModel.onEvent(FeedEvent.VerseRefChanged(it)) },
                    placeholder = { Text("Tag a verse (optional, e.g. John 3:16)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GraceGreen,
                        unfocusedBorderColor = GraceCreamDim,
                        focusedTextColor = GraceCream,
                        unfocusedTextColor = GraceCream,
                        cursorColor = GraceGreen
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (state.draftImageUri == null) "📷 Add Photo" else "📷 Photo added ✓",
                        color = GraceGreen,
                        modifier = Modifier
                            .clickable {
                                photoPicker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                            .padding(8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                GraceButton(
                    text = "Post 🌿",
                    onClick = { viewModel.onEvent(FeedEvent.SubmitPost) },
                    enabled = state.draftText.isNotBlank() || state.draftImageUri != null,
                    loading = state.isPosting,
                    containerColor = GraceGreen,
                    contentColor = GraceDeepBlue
                )
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text("⚠ ${state.error}", color = GraceRose)
        }

        Spacer(Modifier.height(12.dp))
        GracePullToRefresh(onRefresh = { viewModel.refresh() }) {
            if (state.posts.isEmpty() && !state.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🌿", fontSize = 40.sp)
                    Text(
                        "No posts yet. Be the first to share!",
                        color = GraceCreamDim
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.posts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            myReaction = state.myReactions[post.id] ?: post.myReaction,
                            onReact = { viewModel.onEvent(FeedEvent.React(post.id, it)) }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
        }  // inner padded Column
    }
}
