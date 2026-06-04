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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

/**
 * Admin → tap a user → this screen. Read-mostly: shows name, email,
 * Compassion status, Compassion number, emergency contact, group + role.
 * The only mutating action is changing the role (sensitive changes
 * require confirmation, matching the existing AdminScreen contract).
 */
@Composable
fun AdminUserDetailScreen(
    onBack: () -> Unit,
    viewModel: AdminUserDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var toast by remember { mutableStateOf<String?>(null) }

    if (toast != null) {
        LaunchedEffect(toast) {
            kotlinx.coroutines.delay(2500)
            toast = null
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { fx ->
            when (fx) {
                is AdminUserDetailEffect.ShowError -> toast = "⚠ ${fx.message}"
                is AdminUserDetailEffect.ShowSuccess -> toast = "✓ ${fx.message}"
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
            Text("Member Details", color = GraceCream, fontSize = 24.sp)
        }

        if (toast != null) {
            Spacer(Modifier.height(8.dp))
            Text(toast!!, color = if (toast!!.startsWith("✓")) GraceGreen else GraceRose)
        }
        if (state.error != null) {
            Spacer(Modifier.height(8.dp))
            Text("⚠ ${state.error}", color = GraceRose)
        }

        Spacer(Modifier.height(20.dp))
        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                CircularProgressIndicator(color = GraceGold)
            }
            return@Column
        }
        val user = state.user ?: return@Column

        // Avatar + name + email header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(84.dp)
                    .background(roleColor(user.role).copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    user.name.firstOrNull()?.uppercase() ?: "?",
                    color = GraceCream, fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                user.name.ifBlank { "—" },
                color = GraceCream, fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(user.email, color = GraceCreamDim, fontSize = 12.sp)
        }

        Spacer(Modifier.height(20.dp))
        DetailCard {
            // Role row — tappable to change. Dropdown emerges in-place.
            var menuOpen by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    SmallLabel("ROLE")
                    Text(
                        user.role.label,
                        color = roleColor(user.role),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Box {
                    Text(
                        "Change role ▾",
                        color = GraceGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                GraceGold.copy(alpha = 0.15f),
                                RoundedCornerShape(50)
                            )
                            .clickable(enabled = !state.isCommitting) {
                                menuOpen = true
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        UserRole.values().forEach { role ->
                            DropdownMenuItem(
                                text = { Text(role.label) },
                                onClick = {
                                    menuOpen = false
                                    if (role != user.role) {
                                        viewModel.onEvent(
                                            AdminUserDetailEvent.StartRoleChange(role)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        DetailCard {
            SmallLabel("COMPASSION PARTICIPANT")
            if (user.isCompassion) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Yes", color = GraceGreen, fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.size(10.dp))
                    user.compassionNumber?.let { num ->
                        Text(
                            num, color = GraceGold, fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    GraceGold.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            } else {
                Text("No", color = GraceCreamDim, fontSize = 16.sp)
            }
        }

        Spacer(Modifier.height(12.dp))
        DetailCard {
            SmallLabel("EMERGENCY CONTACT")
            Text(
                user.emergencyContact?.takeIf { it.isNotBlank() } ?: "Not provided",
                color = if (user.emergencyContact.isNullOrBlank())
                    GraceCreamDim else GraceCream,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(32.dp))
    }

    // Sensitive-change confirmation, mirroring AdminScreen's pattern.
    state.pendingRole?.let { role ->
        AlertDialog(
            onDismissRequest = {
                viewModel.onEvent(AdminUserDetailEvent.CancelRoleChange)
            },
            title = {
                Text(
                    if (state.pendingIsSensitive) "Confirm sensitive change"
                    else "Change role?"
                )
            },
            text = {
                Column {
                    Text(
                        "Set ${state.user?.name}'s role to ${role.label}?"
                    )
                    if (state.pendingIsSensitive) {
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
                TextButton(onClick = {
                    viewModel.onEvent(AdminUserDetailEvent.ConfirmRoleChange)
                }) {
                    Text("Confirm",
                        color = if (state.pendingIsSensitive)
                            GraceRose else GraceGold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.onEvent(AdminUserDetailEvent.CancelRoleChange)
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GraceCardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) { content() }
    }
}

@Composable
private fun SmallLabel(text: String) {
    Text(
        text, color = GraceCreamDim, fontSize = 10.sp,
        letterSpacing = 3.sp, fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(4.dp))
}

// Mirror of AdminScreen.roleColor() — kept local so the two screens can
// evolve independently. Tiny enough that duplication < dependency.
@androidx.compose.runtime.Composable
private fun roleColor(role: UserRole): Color = when (role) {
    UserRole.MEMBER -> GraceMuted
    UserRole.CELL_LEADER -> GraceGreen
    UserRole.YOUTH_PRESIDENT -> GracePurple
    UserRole.PASTOR -> GraceGold
    UserRole.ADMIN -> GraceRose
}
