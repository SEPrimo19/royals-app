package com.grace.app.presentation.screens.lifegroup

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.grace.app.domain.model.BrowsableGroup
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GraceRose

@Composable
fun FindCellScreen(
    onBack: () -> Unit,
    viewModel: FindCellViewModel = hiltViewModel()
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
                is FindCellEffect.Toast -> toast = fx.message to fx.isError
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
                Text("Find a Cell 🤝", color = GraceCream, fontSize = 24.sp)
                Text(
                    "Browse cell groups · Request to join",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.onEvent(FindCellEvent.QueryChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search by name or leader…", color = GraceCreamDim) }
        )

        if (toast != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                (if (toast!!.second) "⚠ " else "✓ ") + toast!!.first,
                color = if (toast!!.second) GraceRose else GraceGreen,
                fontSize = 12.sp
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
            state.visibleGroups.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.query.isBlank()) "No cell groups yet."
                        else "No cells match your search.",
                        color = GraceCreamDim
                    )
                }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.visibleGroups, key = { it.id }) { g ->
                    GroupRow(
                        group = g,
                        isWorking = state.workingGroupId == g.id,
                        onRequest = {
                            viewModel.onEvent(FindCellEvent.RequestToJoin(g.id))
                        },
                        onCancel = { rid ->
                            viewModel.onEvent(FindCellEvent.CancelPending(g.id, rid))
                        }
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun GroupRow(
    group: BrowsableGroup,
    isWorking: Boolean,
    onRequest: () -> Unit,
    onCancel: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp)
                        .background(GraceGold.copy(alpha = 0.20f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        group.name.firstOrNull()?.uppercase() ?: "?",
                        color = GraceGold, fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        group.name, color = GraceCream, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Led by ${group.leaderName} · ${group.memberCount} member" +
                            if (group.memberCount == 1) "" else "s",
                        color = GraceCreamDim, fontSize = 11.sp
                    )
                }
                ActionPill(
                    group = group,
                    isWorking = isWorking,
                    onRequest = onRequest,
                    onCancel = { group.myPendingRequestId?.let(onCancel) }
                )
            }
            if (!group.description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    group.description, color = GraceCreamDim, fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun ActionPill(
    group: BrowsableGroup,
    isWorking: Boolean,
    onRequest: () -> Unit,
    onCancel: () -> Unit
) {
    val (label, fg, bg, action) = when {
        group.isMyGroup -> PillSpec("Member", GraceGreen, GraceGreen.copy(alpha = 0.18f), null)
        group.myPendingRequestId != null ->
            PillSpec("Pending · Cancel", GraceGold, GraceGold.copy(alpha = 0.18f), onCancel)
        else -> PillSpec("Request to Join", GraceCream, GraceMuted, onRequest)
    }
    val mod = if (isWorking || action == null) Modifier
              else Modifier.clickable { action() }
    Text(
        if (isWorking) "…" else label,
        color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = mod
            .background(bg, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

private data class PillSpec(
    val label: String,
    val fg: androidx.compose.ui.graphics.Color,
    val bg: androidx.compose.ui.graphics.Color,
    val onClick: (() -> Unit)?
)
