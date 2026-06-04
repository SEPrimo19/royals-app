package com.grace.app.presentation.screens.games.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.grace.app.domain.model.GameDifficulty
import com.grace.app.domain.model.QuestionCategory
import com.grace.app.presentation.theme.GraceCardAlt
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGoldDim
import com.grace.app.presentation.theme.GraceMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQuestionScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditQuestionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { eff ->
            when (eff) {
                EditQuestionEffect.Saved -> onSaved()
                is EditQuestionEffect.ShowError -> snackbar.showSnackbar(eff.message)
            }
        }
    }

    Scaffold(
        containerColor = GraceDeepBlue,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isNew) "New Question" else "Edit Question",
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
                        onClick = { viewModel.onEvent(EditQuestionEvent.Save) },
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
            SectionLabel("CATEGORY")
            ChipRow(
                values = QuestionCategory.values().map { it to chipLabel(it) },
                selected = state.category,
                onSelected = { viewModel.onEvent(EditQuestionEvent.CategoryChanged(it)) }
            )

            SectionLabel("DIFFICULTY")
            ChipRow(
                values = GameDifficulty.values().map { it to it.name },
                selected = state.difficulty,
                onSelected = { viewModel.onEvent(EditQuestionEvent.DifficultyChanged(it)) }
            )

            SectionLabel("QUESTION")
            graceField(
                value = state.question,
                onChange = { viewModel.onEvent(EditQuestionEvent.QuestionChanged(it)) },
                placeholder = "What question do you want to ask?",
                minLines = 2
            )

            SectionLabel("OPTIONS (tap the dot to mark the correct answer)")
            state.options.forEachIndexed { idx, opt ->
                OptionField(
                    index = idx,
                    value = opt,
                    isCorrect = state.correctIndex == idx,
                    onValueChange = {
                        viewModel.onEvent(EditQuestionEvent.OptionChanged(idx, it))
                    },
                    onPickCorrect = {
                        viewModel.onEvent(EditQuestionEvent.CorrectChanged(idx))
                    }
                )
            }

            SectionLabel("EXPLANATION (optional)")
            graceField(
                value = state.explanation,
                onChange = { viewModel.onEvent(EditQuestionEvent.ExplanationChanged(it)) },
                placeholder = "Shown after answering. Why is this the right answer?",
                minLines = 2
            )

            SectionLabel("SOURCE REFERENCE (optional)")
            graceField(
                value = state.sourceRef,
                onChange = { viewModel.onEvent(EditQuestionEvent.SourceRefChanged(it)) },
                placeholder = "e.g. John 3:16",
                minLines = 1
            )

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
                    Text("Inactive questions never appear in games.",
                        color = GraceCreamDim, fontSize = 11.sp)
                }
                Switch(
                    checked = state.isActive,
                    onCheckedChange = { viewModel.onEvent(EditQuestionEvent.ActiveChanged(it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GraceGold,
                        checkedTrackColor = GraceGold.copy(alpha = 0.4f),
                        uncheckedThumbColor = GraceMuted,
                        uncheckedTrackColor = GraceMuted.copy(alpha = 0.4f)
                    )
                )
            }

            Button(
                onClick = { viewModel.onEvent(EditQuestionEvent.Save) },
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
                    Text(if (state.isNew) "Save Question" else "Update Question",
                        fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun OptionField(
    index: Int,
    value: String,
    isCorrect: Boolean,
    onValueChange: (String) -> Unit,
    onPickCorrect: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isCorrect) GraceGold else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (isCorrect) GraceGold else GraceMuted,
                    shape = RoundedCornerShape(50)
                )
                .clickable { onPickCorrect() },
            contentAlignment = Alignment.Center
        ) {
            if (isCorrect) {
                Icon(
                    Icons.Default.Check, contentDescription = "Correct",
                    tint = GraceDeepBlue, modifier = Modifier.size(16.dp)
                )
            } else {
                Text(('A' + index).toString(),
                    color = GraceCreamDim, fontSize = 12.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        graceField(
            value = value,
            onChange = onValueChange,
            placeholder = "Option ${('A' + index)}",
            minLines = 1,
            modifier = Modifier.weight(1f)
        )
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
private fun <T> ChipRow(
    values: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        values.forEach { (v, label) ->
            val active = v == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (active) GraceGold else GraceCardBg)
                    .border(
                        1.dp,
                        if (active) GraceGold else GraceMuted.copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelected(v) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (active) GraceDeepBlue else GraceCream,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
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

private fun chipLabel(c: QuestionCategory): String = when (c) {
    QuestionCategory.OLD_TESTAMENT -> "Old Testament"
    QuestionCategory.NEW_TESTAMENT -> "New Testament"
    QuestionCategory.CHARACTER -> "Character"
}
