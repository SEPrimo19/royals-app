package com.grace.app.presentation.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.UserRole
import com.grace.app.presentation.components.BirthdateField
import com.grace.app.presentation.components.CompassionSection
import com.grace.app.presentation.components.SexSelector
import com.grace.app.presentation.components.GraceButton
import com.grace.app.presentation.theme.GraceCardAlt
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceRose
import kotlinx.coroutines.flow.collectLatest

private data class RoleOption(val role: UserRole, val emoji: String, val label: String)

private val roleOptions = listOf(
    RoleOption(UserRole.MEMBER, "👤", "Member"),
    RoleOption(UserRole.CELL_LEADER, "🛡️", "Cell Leader"),
    RoleOption(UserRole.COUNCIL, "⚜️", "Council"),
    RoleOption(UserRole.YOUTH_PRESIDENT, "👑", "Youth President"),
    RoleOption(UserRole.PASTOR, "✝️", "Pastor")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onNavigateHome: () -> Unit,
    viewModel: ProfileSetupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                ProfileSetupEffect.NavigateToHome -> onNavigateHome()
                is ProfileSetupEffect.ShowError -> Unit
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(40.dp))
        Text("Welcome to Royals 🙏", color = GraceGold, fontSize = 28.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tell us a little about yourself so we can connect you to the right people.",
            color = GraceCreamDim,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(28.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            roleOptions.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { option ->
                        val selected = state.selectedRole == option.role
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(96.dp)
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) GraceGold else GraceCardAlt,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    viewModel.onEvent(
                                        ProfileSetupEvent.RoleSelected(option.role)
                                    )
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    GraceGold.copy(alpha = 0.15f)
                                } else {
                                    GraceCardAlt
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(option.emoji, fontSize = 28.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    option.label,
                                    color = if (selected) GraceGold else GraceCream,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        if (state.selectedRole == UserRole.COUNCIL) {
            Text(
                "Council officers don't pick a cell group — you'll be tagged as " +
                    "ministry-wide leadership.",
                color = GraceCreamDim, fontSize = 12.sp
            )
        } else {
            Text("Your cell group", color = GraceCreamDim, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))

            var expanded by remember { mutableStateOf(false) }
            val selectedGroupName = state.availableGroups
                .firstOrNull { it.id == state.selectedGroupId }?.name ?: "Select a group"

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedGroupName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    state.availableGroups.forEach { group ->
                        DropdownMenuItem(
                            text = { Text(group.name) },
                            onClick = {
                                viewModel.onEvent(ProfileSetupEvent.GroupSelected(group.id))
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Birthdate",
            color = GraceCreamDim, fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        BirthdateField(
            value = state.birthdate,
            onChange = { viewModel.onEvent(ProfileSetupEvent.BirthdateChanged(it)) }
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "Sex",
            color = GraceCreamDim, fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        SexSelector(
            current = state.sex,
            onSelect = { viewModel.onEvent(ProfileSetupEvent.SexChanged(it)) }
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Required for Compassion participants. Helps tailor age-appropriate content.",
            color = GraceCreamDim, fontSize = 11.sp
        )

        Spacer(Modifier.height(28.dp))
        CompassionSection(
            isCompassion = state.isCompassion,
            digits = state.compassionDigits,
            onToggle = { viewModel.onEvent(ProfileSetupEvent.CompassionToggled(it)) },
            onDigitsChange = {
                viewModel.onEvent(ProfileSetupEvent.CompassionDigitsChanged(it))
            }
        )

        Spacer(Modifier.height(20.dp))
        Text(
            "Emergency contact (optional)",
            color = GraceCreamDim, fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.emergencyContact,
            onValueChange = {
                viewModel.onEvent(ProfileSetupEvent.EmergencyContactChanged(it))
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("e.g. 09XX-XXX-XXXX", color = GraceCreamDim, fontSize = 13.sp)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = GraceCream,
                unfocusedTextColor = GraceCream,
                focusedContainerColor = GraceCardAlt,
                unfocusedContainerColor = GraceCardAlt,
                focusedBorderColor = GraceGold,
                unfocusedBorderColor = GraceCardAlt,
                cursorColor = GraceGold
            )
        )

        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text("⚠ ${state.error}", color = GraceRose)
        }

        Spacer(Modifier.height(28.dp))
        GraceButton(
            text = "Let's Go →",
            onClick = { viewModel.onEvent(ProfileSetupEvent.CompleteSetup) },
            enabled = state.canSubmit,
            loading = state.isSubmitting,
            modifier = Modifier.alpha(if (state.canSubmit) 1f else 0.6f)
        )
        Spacer(Modifier.height(40.dp))
    }
}

