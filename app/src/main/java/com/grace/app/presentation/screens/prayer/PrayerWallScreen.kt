package com.grace.app.presentation.screens.prayer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.PrayerCategory
import com.grace.app.presentation.components.GraceButton
import com.grace.app.presentation.components.GracePullToRefresh
import com.grace.app.presentation.components.MenuButtonRow
import com.grace.app.presentation.components.PrayerCard
import com.grace.app.presentation.components.categoryColor
import com.grace.app.presentation.theme.GraceBlue
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue

@Composable
fun PrayerWallScreen(
    onOpenMenu: () -> Unit,
    viewModel: PrayerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.refresh() }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            if (fx is PrayerEffect.ShowError) {
                viewModel.onEvent(PrayerEvent.DismissError)
                viewModel.surfaceError(fx.message)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
    ) {
        MenuButtonRow(onOpenMenu)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Prayer Wall 🙏", color = GraceCream, fontSize = 26.sp)
                val n = state.totalActiveCount
                val countText = if (n == 1) "1 prayer being lifted up"
                    else "$n prayers being lifted up"
                Text(countText, color = GraceCreamDim, fontSize = 12.sp)
            }
            Text(
                "+ Request",
                color = GraceDeepBlue,
                modifier = Modifier
                    .background(GraceBlue, RoundedCornerShape(12.dp))
                    .clickable { viewModel.onEvent(PrayerEvent.TogglePostForm) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        AnimatedVisibility(
            visible = state.showPostForm,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            PostForm(state, viewModel)
        }

        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PrayerSort.entries) { s ->
                FilterPill(
                    label = sortLabel(s),
                    selected = state.sort == s
                ) { viewModel.onEvent(PrayerEvent.SortChanged(s)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterPill("All", state.activeFilter == null) {
                    viewModel.onEvent(PrayerEvent.FilterChanged(null))
                }
            }
            items(PrayerCategory.entries) { cat ->
                FilterPill(
                    cat.name.lowercase().replaceFirstChar { it.uppercase() },
                    state.activeFilter == cat
                ) { viewModel.onEvent(PrayerEvent.FilterChanged(cat)) }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (state.error != null) {
            Text("⚠ ${state.error}", color = com.grace.app.presentation.theme.GraceRose)
            Spacer(Modifier.height(8.dp))
        }

        GracePullToRefresh(onRefresh = { viewModel.refresh() }) {
            if (state.prayers.isEmpty() && !state.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🙏", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.activeFilter != null)
                            "No prayers in this category yet."
                        else
                            "No prayers yet. Be the first to ask for prayer.",
                        color = GraceCreamDim,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.prayers, key = { it.id }) { prayer ->
                        PrayerCard(
                            prayer = prayer,
                            realtimePrayCount = maxOf(
                                state.prayCountUpdates[prayer.id] ?: 0,
                                prayer.prayCount
                            ),
                            hasUserPrayed = prayer.id in state.myIntercessions,
                            isOwnPrayer = prayer.userId != null &&
                                prayer.userId == state.currentUserId,
                            onPrayTap = { viewModel.onEvent(PrayerEvent.Intercede(prayer.id)) },
                            onMarkAnswered = {
                                viewModel.onEvent(PrayerEvent.MarkAnswered(prayer.id))
                            }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
        }
    }
}

private fun sortLabel(s: PrayerSort): String = when (s) {
    PrayerSort.NEWEST -> "Newest"
    PrayerSort.OLDEST -> "Oldest"
    PrayerSort.ANSWERED -> "Answered"
    PrayerSort.MOST_PRAYED -> "Most prayed"
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) GraceDeepBlue else GraceCreamDim,
        fontSize = 12.sp,
        modifier = Modifier
            .background(
                if (selected) GraceBlue else GraceCardBg,
                RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    )
}

@Composable
private fun PostForm(state: PrayerUiState, viewModel: PrayerViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .border(1.dp, GraceBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text("SHARE YOUR PRAYER REQUEST", color = GraceBlue, fontSize = 10.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.newPrayerText,
            onValueChange = { viewModel.onEvent(PrayerEvent.PrayerTextChanged(it)) },
            placeholder = { Text("What would you like the community to pray for?") },
            modifier = Modifier.fillMaxWidth().height(110.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GraceBlue,
                unfocusedBorderColor = GraceCreamDim,
                focusedTextColor = GraceCream,
                unfocusedTextColor = GraceCream,
                cursorColor = GraceBlue
            )
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PrayerCategory.entries) { cat ->
                val sel = state.newPrayerCategory == cat
                val c = categoryColor(cat)
                Text(
                    cat.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = if (sel) c else GraceCreamDim,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(c.copy(alpha = 0.2f), RoundedCornerShape(50))
                        .border(
                            if (sel) 1.dp else 0.dp,
                            if (sel) c else Color.Transparent,
                            RoundedCornerShape(50)
                        )
                        .clickable { viewModel.onEvent(PrayerEvent.CategorySelected(cat)) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.isNewPrayerAnonymous,
                onCheckedChange = { viewModel.onEvent(PrayerEvent.AnonymousToggled) },
                colors = CheckboxDefaults.colors(checkedColor = GraceBlue)
            )
            Text(
                "Post anonymously — your name is hidden from members. " +
                    "Leaders can see it for your safety.",
                color = GraceCreamDim,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(12.dp))
        GraceButton(
            text = "Submit 🙏",
            onClick = { viewModel.onEvent(PrayerEvent.SubmitPrayer) },
            enabled = state.newPrayerText.isNotBlank(),
            loading = state.isSubmittingPrayer,
            containerColor = GraceBlue,
            contentColor = GraceDeepBlue
        )
    }
}
