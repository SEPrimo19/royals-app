package com.grace.app.presentation.screens.games.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.grace.app.presentation.theme.GraceCardAlt
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGoldDim
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GracePurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPassageScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditPassageViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { eff ->
            when (eff) {
                EditPassageEffect.Saved -> onSaved()
                is EditPassageEffect.ShowError -> snackbar.showSnackbar(eff.message)
            }
        }
    }

    Scaffold(
        containerColor = GraceDeepBlue,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isNew) "New Daily Verse" else "Edit Daily Verse",
                        color = GraceCream, fontWeight = FontWeight.SemiBold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back",
                            tint = GraceCream)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onEvent(EditPassageEvent.Save) },
                        enabled = !state.isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save",
                            tint = GraceGold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GraceDeepBlue
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { inner ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(inner), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GraceGold)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionLabel("REFERENCE")
            graceField(
                value = state.reference,
                onChange = { viewModel.onEvent(EditPassageEvent.ReferenceChanged(it)) },
                placeholder = "e.g. John 3:16",
                minLines = 1
            )

            SectionLabel("VERSE TEXT")
            graceField(
                value = state.text,
                onChange = { viewModel.onEvent(EditPassageEvent.TextChanged(it)) },
                placeholder = "Type the full verse exactly as it appears in your translation.",
                minLines = 3
            )

            SectionLabel("BLANK WORD (must appear in the verse text above)")
            graceField(
                value = state.blankWord,
                onChange = { viewModel.onEvent(EditPassageEvent.BlankWordChanged(it)) },
                placeholder = "e.g. believes",
                minLines = 1
            )

            SectionLabel("DISTRACTORS (3 wrong-but-plausible options)")
            graceField(
                value = state.distractor1,
                onChange = { viewModel.onEvent(EditPassageEvent.Distractor1Changed(it)) },
                placeholder = "Distractor 1",
                minLines = 1
            )
            graceField(
                value = state.distractor2,
                onChange = { viewModel.onEvent(EditPassageEvent.Distractor2Changed(it)) },
                placeholder = "Distractor 2",
                minLines = 1
            )
            graceField(
                value = state.distractor3,
                onChange = { viewModel.onEvent(EditPassageEvent.Distractor3Changed(it)) },
                placeholder = "Distractor 3",
                minLines = 1
            )

            if (state.text.isNotBlank() && state.blankWord.isNotBlank()) {
                PreviewCard(verse = state.text, blank = state.blankWord)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GraceCardBg)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Active",
                        color = GraceCream, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold)
                    Text("Inactive verses never appear as Daily Verse.",
                        color = GraceCreamDim, fontSize = 11.sp)
                }
                Switch(
                    checked = state.isActive,
                    onCheckedChange = { viewModel.onEvent(EditPassageEvent.ActiveChanged(it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GraceGold,
                        checkedTrackColor = GraceGold.copy(alpha = 0.4f),
                        uncheckedThumbColor = GraceMuted,
                        uncheckedTrackColor = GraceMuted.copy(alpha = 0.4f)
                    )
                )
            }

            Button(
                onClick = { viewModel.onEvent(EditPassageEvent.Save) },
                enabled = !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GraceGold,
                    contentColor = GraceDeepBlue
                )
            ) {
                if (state.isSaving)
                    CircularProgressIndicator(color = GraceDeepBlue,
                        modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else
                    Text(if (state.isNew) "Save Verse" else "Update Verse",
                        fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun PreviewCard(verse: String, blank: String) {
    val masked = verse.replace(
        Regex("\\b${Regex.escape(blank)}\\b", RegexOption.IGNORE_CASE),
        "______"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GraceCardAlt)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("PREVIEW", color = GracePurple, fontSize = 10.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text(masked, color = GraceCream,
            fontStyle = FontStyle.Italic, fontSize = 14.sp)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = GraceGoldDim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp
    )
}

@Composable
private fun graceField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = GraceCreamDim, fontSize = 13.sp) },
        minLines = minLines,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = GraceCream,
            unfocusedTextColor = GraceCream,
            focusedContainerColor = GraceCardAlt,
            unfocusedContainerColor = GraceCardAlt,
            focusedBorderColor = GraceGold,
            unfocusedBorderColor = GraceMuted.copy(alpha = 0.5f),
            cursorColor = GraceGold
        )
    )
}
