package com.grace.app.presentation.screens.admin

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.grace.app.domain.model.UserRole
import com.grace.app.presentation.theme.GraceCardAlt
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGoldDim
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GraceRose

@Composable
fun BulkEmailScreen(
    onBack: () -> Unit,
    viewModel: BulkEmailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var groupMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = GraceCream, fontSize = 22.sp,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp))
            Column {
                Text("Send Announcement", color = GraceCream, fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold)
                Text("Bulk email to the church family.",
                    color = GraceCreamDim, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(20.dp))

        SectionLabel("AUDIENCE")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()) {
            AudPill("Everyone", state.audienceKind == AudienceKind.ALL, Modifier.weight(1f)) {
                viewModel.onEvent(BulkEmailEvent.AudienceKindChanged(AudienceKind.ALL))
            }
            AudPill("By role", state.audienceKind == AudienceKind.ROLES, Modifier.weight(1f)) {
                viewModel.onEvent(BulkEmailEvent.AudienceKindChanged(AudienceKind.ROLES))
            }
            AudPill("By group", state.audienceKind == AudienceKind.GROUP, Modifier.weight(1f)) {
                viewModel.onEvent(BulkEmailEvent.AudienceKindChanged(AudienceKind.GROUP))
            }
        }

        if (state.audienceKind == AudienceKind.ROLES) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UserRole.values().forEach { role ->
                    RoleCheckRow(
                        label = role.label,
                        checked = role in state.selectedRoles,
                        onToggle = { viewModel.onEvent(BulkEmailEvent.ToggleRole(role)) }
                    )
                }
            }
        }

        if (state.audienceKind == AudienceKind.GROUP) {
            Spacer(Modifier.height(12.dp))
            Box {
                val label = state.groups.firstOrNull { it.id == state.selectedGroupId }
                    ?.name ?: "Choose a cell group…"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GraceCardAlt)
                        .clickable { groupMenuOpen = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, color = GraceCream, modifier = Modifier.weight(1f))
                    Text("▾", color = GraceGold, fontSize = 16.sp)
                }
                DropdownMenu(
                    expanded = groupMenuOpen,
                    onDismissRequest = { groupMenuOpen = false }
                ) {
                    state.groups.forEach { g ->
                        DropdownMenuItem(
                            text = { Text(g.name) },
                            onClick = {
                                viewModel.onEvent(BulkEmailEvent.GroupSelected(g.id))
                                groupMenuOpen = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionLabel("SUBJECT")
        EmailField(
            value = state.subject,
            onChange = { viewModel.onEvent(BulkEmailEvent.SubjectChanged(it)) },
            placeholder = "What is this email about?",
            minLines = 1
        )

        Spacer(Modifier.height(14.dp))
        SectionLabel("MESSAGE")
        EmailField(
            value = state.message,
            onChange = { viewModel.onEvent(BulkEmailEvent.MessageChanged(it)) },
            placeholder = "Write your message — supports paragraph breaks.",
            minLines = 8
        )

        if (state.lastResultText != null) {
            Spacer(Modifier.height(12.dp))
            Text("✓ ${state.lastResultText}", color = GraceGreen, fontSize = 13.sp)
        }
        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text("⚠ ${state.error}", color = GraceRose, fontSize = 13.sp)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { viewModel.onEvent(BulkEmailEvent.Send) },
            enabled = !state.isSending &&
                state.subject.isNotBlank() &&
                state.message.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = GraceGold,
                contentColor = GraceDeepBlue
            )
        ) {
            if (state.isSending)
                CircularProgressIndicator(color = GraceDeepBlue,
                    modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            else
                Text("Send Email", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = GraceGoldDim, fontSize = 10.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun AudPill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) GraceGold else GraceCardBg)
            .border(
                1.dp,
                if (selected) GraceGold else GraceMuted.copy(alpha = 0.4f),
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (selected) GraceDeepBlue else GraceCream,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun RoleCheckRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(GraceCardBg)
            .clickable { onToggle() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) GraceGold else Color.Transparent)
                .border(
                    2.dp,
                    if (checked) GraceGold else GraceMuted,
                    RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) Text("✓", color = GraceDeepBlue, fontSize = 14.sp,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = GraceCream, fontSize = 14.sp)
    }
}

@Composable
private fun EmailField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = GraceCreamDim, fontSize = 13.sp) },
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
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
