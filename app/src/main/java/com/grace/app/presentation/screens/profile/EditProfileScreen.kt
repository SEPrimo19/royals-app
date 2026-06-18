package com.grace.app.presentation.screens.profile

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.presentation.components.BirthdateField
import com.grace.app.presentation.components.CompassionSection
import com.grace.app.presentation.components.SexSelector
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceRose

@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var toast by remember { mutableStateOf<String?>(null) }

    if (toast != null) {
        LaunchedEffect(toast) {
            kotlinx.coroutines.delay(2500)
            toast = null
        }
    }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            when (fx) {
                EditProfileEffect.Saved -> {
                    toast = "✓ Profile saved."
                    kotlinx.coroutines.delay(900)
                    onBack()
                }
                is EditProfileEffect.ShowError -> toast = "⚠ ${fx.message}"
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
            Text("Edit Profile", color = GraceCream, fontSize = 24.sp)
        }

        if (toast != null) {
            Spacer(Modifier.height(10.dp))
            Text(toast!!, color = if (toast!!.startsWith("✓")) GraceGreen else GraceRose)
        }
        if (state.error != null) {
            Spacer(Modifier.height(10.dp))
            Text("⚠ ${state.error}", color = GraceRose)
        }

        Spacer(Modifier.height(16.dp))
        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(180.dp), Alignment.Center) {
                CircularProgressIndicator(color = GraceGold)
            }
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier.size(84.dp)
                        .background(GraceGold.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        state.name.firstOrNull()?.uppercase() ?: "?",
                        color = GraceCream, fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                state.email.ifBlank { "—" },
                color = GraceCreamDim, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))
            SectionHeader("YOUR NAME")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onEvent(EditProfileEvent.NameChanged(it)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(18.dp))
            SectionHeader("ABOUT YOU")
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.bio,
                onValueChange = { viewModel.onEvent(EditProfileEvent.BioChanged(it)) },
                modifier = Modifier.fillMaxWidth().height(110.dp),
                placeholder = {
                    Text(
                        "A short bio — your story, your verse, what God's doing.",
                        color = GraceCreamDim
                    )
                }
            )
            Text(
                "${state.bio.length}/280",
                color = GraceCreamDim, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )

            Spacer(Modifier.height(18.dp))
            SectionHeader("MESSENGER LINK")
            Spacer(Modifier.height(6.dp))
            Text(
                "Optional. Only Messenger, m.me, fb.me, or facebook.com links " +
                    "are accepted.",
                color = GraceCreamDim, fontSize = 11.sp
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.messengerUrl,
                onValueChange = {
                    viewModel.onEvent(EditProfileEvent.MessengerChanged(it))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("https://m.me/yourname", color = GraceCreamDim) }
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GraceCardBg, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Show my Messenger to other members",
                        color = GraceCream, fontSize = 13.sp
                    )
                    Text(
                        if (state.messengerPublic)
                            "Visible to Life Group members and leaders."
                        else
                            "Hidden — only you can see it.",
                        color = GraceCreamDim, fontSize = 11.sp
                    )
                }
                Switch(
                    checked = state.messengerPublic,
                    onCheckedChange = {
                        viewModel.onEvent(EditProfileEvent.MessengerPublicChanged(it))
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = GraceGold),
                    enabled = state.messengerUrl.isNotBlank()
                )
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader("BIRTHDATE")
            Spacer(Modifier.height(6.dp))
            BirthdateField(
                value = state.birthdate,
                onChange = { viewModel.onEvent(EditProfileEvent.BirthdateChanged(it)) }
            )

            Spacer(Modifier.height(18.dp))
            SectionHeader("SEX")
            Spacer(Modifier.height(6.dp))
            SexSelector(
                current = state.sex,
                onSelect = { viewModel.onEvent(EditProfileEvent.SexChanged(it)) }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Required for Compassion participants.",
                color = GraceCreamDim, fontSize = 11.sp
            )

            Spacer(Modifier.height(24.dp))
            SectionHeader("COMPASSION PARTICIPANT")
            Spacer(Modifier.height(6.dp))
            CompassionSection(
                isCompassion = state.isCompassion,
                digits = state.compassionDigits,
                onToggle = {
                    viewModel.onEvent(EditProfileEvent.CompassionToggled(it))
                },
                onDigitsChange = {
                    viewModel.onEvent(EditProfileEvent.CompassionDigitsChanged(it))
                }
            )

            Spacer(Modifier.height(18.dp))
            SectionHeader("EMERGENCY CONTACT")
            Spacer(Modifier.height(6.dp))
            Text(
                "Optional. Shared with your cell leader for emergencies only.",
                color = GraceCreamDim, fontSize = 11.sp
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.emergencyContact,
                onValueChange = {
                    viewModel.onEvent(EditProfileEvent.EmergencyContactChanged(it))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. 09XX-XXX-XXXX", color = GraceCreamDim) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.onEvent(EditProfileEvent.Save) },
                enabled = state.canSave,
                colors = ButtonDefaults.buttonColors(containerColor = GraceGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) CircularProgressIndicator(
                    color = GraceDeepBlue, modifier = Modifier.size(18.dp)
                ) else Text(
                    "Save changes",
                    color = GraceDeepBlue, fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        label, color = GraceCreamDim, fontSize = 10.sp,
        letterSpacing = 3.sp, fontWeight = FontWeight.Bold
    )
}
