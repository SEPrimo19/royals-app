package com.grace.app.presentation.screens.leader

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.WeeklyMeditation
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceRose
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LogReflectionOnBehalfScreen(
    onBack: () -> Unit,
    onLogged: () -> Unit,
    viewModel: LogReflectionOnBehalfViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val weekFmt = DateTimeFormatter.ofPattern("MMM d")

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { fx ->
            if (fx is LogReflectionOnBehalfEffect.Logged) onLogged()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←", color = GraceCream, fontSize = 22.sp,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Log Reflection", color = GraceCream, fontSize = 22.sp)
                Text(
                    "On behalf of this member",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
        }

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = GraceGold)
            }
            return@Column
        }

        if (state.meditations.isEmpty()) {
            Spacer(Modifier.height(32.dp))
            Text(
                "No weekly meditations are available yet. Ask an admin to add this " +
                    "week's content first.",
                color = GraceCreamDim
            )
            return@Column
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Which week's meditation?",
            color = GraceCreamDim, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        // Horizontally scrollable list of week chips — most recent first.
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.meditations, key = { it.id }) { med ->
                val selected = med.id == state.selectedMeditationId
                Text(
                    "Wk ${med.weekNumber} · ${med.startDate.format(weekFmt)}",
                    color = if (selected) GraceDeepBlue else GraceGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(
                            if (selected) GraceGold else GraceGold.copy(alpha = 0.18f),
                            RoundedCornerShape(50)
                        )
                        .clickable {
                            viewModel.onEvent(
                                LogReflectionOnBehalfEvent.MeditationSelected(med.id)
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Selected meditation preview — gives the leader context for what
        // they're logging a reflection on.
        val selectedMed = state.meditations.firstOrNull {
            it.id == state.selectedMeditationId
        }
        if (selectedMed != null) {
            Spacer(Modifier.height(12.dp))
            MeditationPreview(selectedMed)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Reflection (as the member wrote it)",
            color = GraceCreamDim, fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = state.reflectionText,
            onValueChange = {
                viewModel.onEvent(LogReflectionOnBehalfEvent.ReflectionChanged(it))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            placeholder = {
                Text(
                    "Type or paste their reflection here.",
                    color = GraceCreamDim
                )
            }
        )

        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text("⚠ ${state.error}", color = GraceRose, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.onEvent(LogReflectionOnBehalfEvent.Submit) },
            enabled = state.canSubmit,
            colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                if (state.isSubmitting) "Logging…" else "Save Reflection",
                color = GraceDeepBlue, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MeditationPreview(med: WeeklyMeditation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                med.theme.label.uppercase(),
                color = GraceGold, fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                med.title,
                color = GraceCream, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                med.scriptureRef,
                color = GraceCreamDim, fontSize = 11.sp,
                fontStyle = FontStyle.Italic
            )
        }
    }
}
