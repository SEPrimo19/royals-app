package com.grace.app.presentation.screens.admin

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.domain.model.User
import com.grace.app.domain.model.UserRole
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceMuted
import com.grace.app.presentation.theme.GracePurple
import com.grace.app.presentation.theme.GraceRose

@Composable
fun AdminScreen(
    onBack: () -> Unit,
    onSendAnnouncement: () -> Unit = {},
    onOpenUser: (String) -> Unit = {},
    onOpenComplianceReport: () -> Unit = {},
    viewModel: AdminViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var toast by remember { mutableStateOf<String?>(null) }

    // Auto-dismiss toasts after 2.5s.
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
                is AdminEffect.ShowError -> toast = "⚠ ${fx.message}"
                is AdminEffect.ShowSuccess -> toast = "✓ ${fx.message}"
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
                Text("Manage Users", color = GraceCream, fontSize = 24.sp)
                Text(
                    "Promote leaders, update roles",
                    color = GraceCreamDim, fontSize = 12.sp
                )
            }
            Text(
                "📄 Report",
                color = GraceGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(
                        GraceGold.copy(alpha = 0.15f),
                        RoundedCornerShape(50)
                    )
                    .clickable { onOpenComplianceReport() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "✉ Email",
                color = GraceGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(
                        GraceGold.copy(alpha = 0.15f),
                        RoundedCornerShape(50)
                    )
                    .clickable { onSendAnnouncement() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }

        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = state.query,
            onValueChange = { viewModel.onEvent(AdminEvent.QueryChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search name or email…", color = GraceCreamDim) }
        )

        if (toast != null) {
            Spacer(Modifier.height(8.dp))
            Text(toast!!, color = if (toast!!.startsWith("✓")) GraceGreen else GraceRose)
        }
        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text("⚠ ${state.error}", color = GraceRose)
        }

        Spacer(Modifier.height(12.dp))
        when {
            state.isLoading && state.users.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GraceGold)
                }

            state.visibleUsers.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (state.query.isBlank()) "No users yet."
                        else "No users match your search.",
                        color = GraceCreamDim
                    )
                }

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.visibleUsers, key = { it.id }) { user ->
                    UserRow(user = user, onTap = { onOpenUser(user.id) })
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // Confirmation dialog for role changes.
    state.pendingUpdate?.let { p ->
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(AdminEvent.CancelRoleChange) },
            title = {
                Text(if (p.isSensitive) "Confirm sensitive change" else "Change role?")
            },
            text = {
                Column {
                    Text("Set ${p.user.name}'s role to ${p.newRole.label}?")
                    if (p.isSensitive) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "This grants (or removes) high-trust access — " +
                                "spiritual oversight (Pastor) or operational " +
                                "leadership (Youth President / Admin). " +
                                "All role changes are logged automatically.",
                            color = GraceRose,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onEvent(AdminEvent.ConfirmRoleChange) }) {
                    Text("Confirm", color = if (p.isSensitive) GraceRose else GraceGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(AdminEvent.CancelRoleChange) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UserRow(user: User, onTap: () -> Unit) {
    // Whole row is the tap target → opens AdminUserDetailScreen. Role
    // change lives there now (clearer flow + makes room for the Compassion
    // info that doesn't fit in a list row).
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp)
                    .background(roleColor(user.role).copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    user.name.firstOrNull()?.uppercase() ?: "?",
                    color = GraceCream, fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(user.name.ifBlank { "—" }, color = GraceCream, fontSize = 15.sp)
                    if (user.isCompassion) {
                        Spacer(Modifier.size(6.dp))
                        // Tiny CP badge so admins can scan the list for
                        // Compassion participants without opening each row.
                        Text(
                            "CP", color = GraceGold,
                            fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    GraceGold.copy(alpha = 0.18f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(user.email, color = GraceCreamDim, fontSize = 11.sp)
            }
            // Read-only role pill — tap row to edit in detail screen.
            Text(
                user.role.label,
                color = roleColor(user.role),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(
                        roleColor(user.role).copy(alpha = 0.18f),
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@androidx.compose.runtime.Composable
private fun roleColor(role: UserRole): Color = when (role) {
    UserRole.MEMBER -> GraceMuted
    UserRole.CELL_LEADER -> GraceGreen
    UserRole.YOUTH_PRESIDENT -> GracePurple
    UserRole.PASTOR -> GraceGold
    UserRole.ADMIN -> GraceRose
}
