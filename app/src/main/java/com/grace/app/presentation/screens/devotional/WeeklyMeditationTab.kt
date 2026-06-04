package com.grace.app.presentation.screens.devotional

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.MeditationTheme
import com.grace.app.domain.model.WeeklyMeditation
import com.grace.app.presentation.theme.GraceBlue
import com.grace.app.presentation.theme.GraceCardAlt
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GraceOrange
import com.grace.app.presentation.theme.GracePurple
import com.grace.app.presentation.theme.GraceRose
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val dateFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d")
private val savedFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, h:mm a")

@Composable
fun WeeklyMeditationTab(
    viewModel: WeeklyMeditationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Inline toast still used for ERROR cases only — the success path now
    // promotes to a proper modal dialog (audit item #2).
    var toast by remember { mutableStateOf<String?>(null) }
    // Captures the time the dialog opened, so the modal can show "Submitted
    // at 4:32 PM" matching when the user actually tapped Submit. We don't
    // wait for the server round-trip to fill this — Saved fires AFTER
    // server confirms.
    var submittedAt by remember { mutableStateOf<LocalDateTime?>(null) }

    if (toast != null) {
        LaunchedEffect(toast) {
            kotlinx.coroutines.delay(2500)
            toast = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            when (fx) {
                WeeklyMeditationEffect.Saved -> submittedAt = LocalDateTime.now()
                is WeeklyMeditationEffect.ShowError -> toast = "⚠ ${fx.message}"
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(GraceDeepBlue)) {
        when {
            state.isLoading && state.meditation == null -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }
            }
            state.meditation == null -> {
                NoMeditationState()
            }
            else -> MeditationContent(state, viewModel, toast)
        }
    }

    // Confirmation modal — replaces the old inline toast for the success
    // path. More affirming for the user, matches the gravity of the action.
    if (submittedAt != null) {
        ReflectionSubmittedDialog(
            submittedAt = submittedAt!!,
            onClose = { submittedAt = null }
        )
    }
}

@Composable
private fun ReflectionSubmittedDialog(
    submittedAt: LocalDateTime,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text("✓ Reflection Submitted", color = GraceGreen,
                fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Your reflection has been saved.",
                    color = GraceCream, fontSize = 14.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Submitted at ${submittedAt.format(savedFmt)}.",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("Close", color = GraceGold, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun NoMeditationState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📅", fontSize = 44.sp)
        Spacer(Modifier.height(10.dp))
        Text(
            "No meditation scheduled this week.",
            color = GraceCream, fontSize = 16.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "The next one will appear when its week begins.",
            color = GraceCreamDim, fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MeditationContent(
    state: WeeklyMeditationUiState,
    viewModel: WeeklyMeditationViewModel,
    toast: String?
) {
    val context = LocalContext.current
    val meditation = state.meditation ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // ---- Header band ---------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "THIS WEEK'S MEDITATION",
                    color = GraceGold,
                    fontSize = 10.sp,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    meditation.title,
                    color = GraceCream,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.size(12.dp))
            ThemeChip(meditation.theme)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Week ${meditation.weekNumber} · " +
                "${meditation.startDate.format(dateFmt)} – " +
                meditation.endDate.format(dateFmt),
            color = GraceCreamDim, fontSize = 11.sp
        )

        // ---- Scripture card -----------------------------------------------
        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = GraceCardAlt
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "✦",
                    color = GraceGold,
                    fontSize = 24.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    meditation.scriptureText,
                    color = GraceCream,
                    fontSize = 17.sp,
                    fontStyle = FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    meditation.scriptureRef,
                    color = GraceGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }

        // ---- Reflection prompt ---------------------------------------------
        Spacer(Modifier.height(18.dp))
        Text(
            "REFLECT",
            color = GraceGold,
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            meditation.reflectionPrompt,
            color = GraceCream,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )

        // ---- Further reading ------------------------------------------------
        if (!meditation.furtherReadingUrl.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            val label = meditation.furtherReadingLabel
                ?: "Further reading"
            Text(
                "📖 $label",
                color = GraceBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(
                        GraceBlue.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        runCatching {
                            // ACTION_VIEW handles http/https links; no need for
                            // a Chrome Custom Tab dependency here — the user's
                            // default browser is the right choice.
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW,
                                    Uri.parse(meditation.furtherReadingUrl))
                            )
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
            Spacer(Modifier.height(4.dp))
            // Audit-item #2 helper copy — tells the youth user that the chip
            // is tappable and what they'll get out of it.
            Text(
                "Click the link for more knowledge.",
                color = GraceCreamDim,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic
            )
        }

        // ---- Privacy badge (CRITICAL — meditation is leader-visible) -------
        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    GraceOrange.copy(alpha = 0.15f),
                    RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("👁", fontSize = 16.sp)
            Spacer(Modifier.size(8.dp))
            Column {
                Text(
                    "Your cell leader will see this reflection.",
                    color = GraceOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "For private thoughts, use the Journal step in Today's Devotional instead.",
                    color = GraceCreamDim,
                    fontSize = 11.sp
                )
            }
        }

        // ---- Reflection text field ----------------------------------------
        Spacer(Modifier.height(14.dp))
        Text(
            "YOUR REFLECTION",
            color = GraceCreamDim,
            fontSize = 10.sp,
            letterSpacing = 3.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
            value = state.reflectionText,
            onValueChange = {
                viewModel.onEvent(WeeklyMeditationEvent.ReflectionChanged(it))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            placeholder = {
                Text(
                    "Write what God is teaching you through this week's verse…",
                    color = GraceCreamDim,
                    fontSize = 13.sp
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = GraceCream,
                unfocusedTextColor = GraceCream,
                focusedContainerColor = GraceCardBg,
                unfocusedContainerColor = GraceCardBg,
                focusedBorderColor = GraceGold,
                unfocusedBorderColor = GraceMuted.copy(alpha = 0.5f),
                cursorColor = GraceGold
            )
        )

        // ---- Saved indicator + Submit button ------------------------------
        state.mySubmission?.let { sub ->
            Spacer(Modifier.height(8.dp))
            Text(
                "✓ Submitted on ${sub.submittedAt.format(savedFmt)}" +
                    if (sub.updatedAt != sub.submittedAt)
                        " · edited ${sub.updatedAt.format(savedFmt)}"
                    else "",
                color = GraceGreen,
                fontSize = 11.sp
            )
        }

        if (toast != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                toast,
                color = if (toast.startsWith("✓")) GraceGreen else GraceRose,
                fontSize = 12.sp
            )
        }
        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text("⚠ ${state.error}", color = GraceRose, fontSize = 12.sp)
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = { viewModel.onEvent(WeeklyMeditationEvent.Submit) },
            enabled = state.canSubmit,
            colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    color = GraceDeepBlue,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Text(
                    if (state.mySubmission != null) "Update Reflection"
                    else "Submit Reflection",
                    color = GraceDeepBlue,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ThemeChip(theme: MeditationTheme) {
    val color = when (theme) {
        MeditationTheme.JESUS -> GraceGold
        MeditationTheme.EDUCATION -> GraceBlue
        MeditationTheme.FAMILY -> GraceGreen
        MeditationTheme.FRIENDS -> GracePurple
        MeditationTheme.CHURCH -> GraceRose
        MeditationTheme.RELATIONSHIPS -> GraceOrange
    }
    Text(
        theme.label,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
