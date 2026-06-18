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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.ActivityCategory
import com.grace.app.domain.model.DurationTag
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceRose

@Composable
fun DiscipleshipAuthorScreen(
    onBack: () -> Unit,
    viewModel: DiscipleshipAuthorViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            when (fx) {
                AuthorEffect.Saved -> onBack()
                is AuthorEffect.Error -> Unit
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←", color = GraceCream, fontSize = 22.sp,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (state.isEdit) "Edit Activity" else "New Activity",
                    color = GraceCream, fontSize = 22.sp
                )
                Text(
                    "Visible to all members; tap Save when done.",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
        }

        if (state.isLoading) {
            Spacer(Modifier.height(48.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GraceGold)
            }
            return@Column
        }

        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text("⚠ ${state.error}", color = GraceRose, fontSize = 12.sp)
        }

        Spacer(Modifier.height(14.dp))
        SmallLabel("TITLE  ·  ${state.title.length}/80")
        OutlinedTextField(
            value = state.title,
            onValueChange = { viewModel.onEvent(AuthorEvent.TitleChanged(it)) },
            placeholder = { Text("e.g. Pray for 3 people by name", color = GraceCreamDim) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        SmallLabel("DESCRIPTION  ·  ${state.description.length}/600")
        OutlinedTextField(
            value = state.description,
            onValueChange = { viewModel.onEvent(AuthorEvent.DescChanged(it)) },
            placeholder = {
                Text(
                    "What should the member actually do? Keep it specific.",
                    color = GraceCreamDim, fontSize = 13.sp
                )
            },
            modifier = Modifier.fillMaxWidth().height(140.dp)
        )

        Spacer(Modifier.height(12.dp))
        SmallLabel("CATEGORY")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(ActivityCategory.entries.toList(), key = { it.slug }) { c ->
                Chip(
                    label = "${c.emoji} ${c.label}",
                    selected = state.category == c,
                    onClick = { viewModel.onEvent(AuthorEvent.CategoryChanged(c)) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        SmallLabel("DURATION")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DurationTag.entries.forEach { d ->
                Chip(
                    label = d.label,
                    selected = state.durationTag == d,
                    onClick = { viewModel.onEvent(AuthorEvent.DurationChanged(d)) }
                )
            }
        }

        if (state.isEdit) {
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Active", color = GraceCream, fontSize = 13.sp,
                    modifier = Modifier.weight(1f))
                Switch(
                    checked = state.isActive,
                    onCheckedChange = { viewModel.onEvent(AuthorEvent.ActiveChanged(it)) }
                )
            }
            Text(
                "Inactive activities stop appearing in the picker and library, " +
                    "but past completions are preserved.",
                color = GraceCreamDim, fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            if (state.isSaving) "Saving…" else "Save",
            color = GraceDeepBlue, fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .background(GraceGold, RoundedCornerShape(50))
                .alpha(if (state.canSubmit) 1f else 0.5f)
                .clickable(enabled = state.canSubmit) {
                    viewModel.onEvent(AuthorEvent.Save)
                }
                .padding(vertical = 14.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SmallLabel(text: String) {
    Text(
        text, color = GraceCreamDim, fontSize = 10.sp,
        letterSpacing = 2.sp, fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(6.dp))
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
