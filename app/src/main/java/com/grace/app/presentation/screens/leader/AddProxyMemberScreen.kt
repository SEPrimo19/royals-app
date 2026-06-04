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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.presentation.components.BirthdateField
import com.grace.app.presentation.components.CompassionSection
import com.grace.app.presentation.components.SexSelector
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceRose
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AddProxyMemberScreen(
    onBack: () -> Unit,
    onMemberAdded: (String) -> Unit,
    viewModel: AddProxyMemberViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { fx ->
            if (fx is AddProxyMemberEffect.MemberAdded) onMemberAdded(fx.newMemberId)
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
                Text("Add Cell Member", color = GraceCream, fontSize = 22.sp)
                Text(
                    "For a member who doesn't have the app yet",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        // Name
        FormLabel("Full name")
        OutlinedTextField(
            value = state.name,
            onValueChange = { viewModel.onEvent(AddProxyMemberEvent.NameChanged(it)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Mark Reyes", color = GraceCreamDim) }
        )

        Spacer(Modifier.height(16.dp))
        FormLabel("Birthdate")
        BirthdateField(
            value = state.birthdate,
            onChange = { viewModel.onEvent(AddProxyMemberEvent.BirthdateChanged(it)) }
        )

        Spacer(Modifier.height(16.dp))
        FormLabel("Sex")
        SexSelector(
            current = state.sex,
            onSelect = { viewModel.onEvent(AddProxyMemberEvent.SexChanged(it)) }
        )

        Spacer(Modifier.height(16.dp))
        // Compassion section reuses the shared component — same look as
        // signup + edit profile flows. Composes "PH867-XXXX" on submit.
        CompassionSection(
            isCompassion = state.isCompassion,
            digits = state.compassionDigits,
            onToggle = { viewModel.onEvent(AddProxyMemberEvent.CompassionToggled(it)) },
            onDigitsChange = {
                viewModel.onEvent(AddProxyMemberEvent.CompassionDigitsChanged(it))
            }
        )

        Spacer(Modifier.height(16.dp))
        FormLabel("Emergency contact (optional)")
        OutlinedTextField(
            value = state.emergencyContact,
            onValueChange = {
                viewModel.onEvent(AddProxyMemberEvent.EmergencyChanged(it))
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("09171234567", color = GraceCreamDim) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Spacer(Modifier.height(16.dp))
        FormLabel("Email (optional — for future claim flow)")
        OutlinedTextField(
            value = state.email,
            onValueChange = { viewModel.onEvent(AddProxyMemberEvent.EmailChanged(it)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("mark@example.com", color = GraceCreamDim) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )

        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text("⚠ ${state.error}", color = GraceRose, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))
        // Phase P.2.6 — when launched from an EventRoster (attendForEventId
        // set), promote "Save & Mark Attended" to the primary CTA. Leaders
        // mid-meeting overwhelmingly want both actions in one tap. Plain
        // "Add Member" stays available as a secondary in case they don't
        // want to attribute attendance (e.g. registering for a future
        // event).
        if (state.attendForEventId != null) {
            Button(
                onClick = {
                    viewModel.onEvent(AddProxyMemberEvent.SubmitAndMarkAttended)
                },
                enabled = state.canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    if (state.isSubmitting) "Adding…" else "Save & Mark Attended",
                    color = GraceDeepBlue, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.onEvent(AddProxyMemberEvent.Submit) },
                enabled = state.canSubmit,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text("Add without marking attended", color = GraceCream)
            }
        } else {
            Button(
                onClick = { viewModel.onEvent(AddProxyMemberEvent.Submit) },
                enabled = state.canSubmit,
                colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    if (state.isSubmitting) "Adding…" else "Add Member",
                    color = GraceDeepBlue, fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Mark will appear in your cell roster + reports. He won't " +
                "have his own app account — you'll log his actions for him.",
            color = GraceCreamDim, fontSize = 11.sp
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FormLabel(text: String) {
    Text(text, color = GraceCreamDim, fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(4.dp))
}

// SexSelector + BirthdateField now live in
// com.grace.app.presentation.components for cross-screen reuse —
// ProfileSetup and EditProfile share the same widgets.
