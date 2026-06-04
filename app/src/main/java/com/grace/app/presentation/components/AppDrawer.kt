package com.grace.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceRose

/**
 * Side drawer surfaced via the burger icon in the top app bar. Replaces the
 * old kitchen-sink Settings screen as the primary nav for community shortcuts,
 * reminder times, leader tools, and admin tools — keeping Settings itself
 * focused on account-management only (Edit Profile / Change Password / etc.).
 *
 * Every nav action closes the drawer BEFORE navigating so the user lands on
 * the new screen with no drawer overlay still animating out.
 */
@Composable
fun AppDrawer(
    onCloseDrawer: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenLifeGroup: () -> Unit,
    onOpenMyContent: () -> Unit,
    onOpenMyAttendance: () -> Unit,
    onOpenMyProgress: () -> Unit,
    onOpenMyJournal: () -> Unit,
    onOpenMyMembers: () -> Unit,
    onOpenManageContent: () -> Unit,
    onOpenAdmin: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: AppDrawerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Helper to close the drawer first, then invoke the navigate lambda.
    // Without this the drawer animates out while the new screen builds and
    // the first frame of the destination flickers behind the scrim.
    val nav: (() -> Unit) -> () -> Unit = { action ->
        { onCloseDrawer(); action() }
    }

    ModalDrawerSheet(
        modifier = Modifier
            .fillMaxHeight()
            .width(304.dp),
        drawerContainerColor = GraceDeepBlue
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            ProfileHeader(
                name = state.name,
                email = state.email,
                role = state.role,
                onEdit = nav(onOpenEditProfile)
            )

            Spacer(Modifier.height(20.dp))
            SectionHeader("MY CONTENT")
            DrawerLink("🏘️", "My Life Group", onTap = nav(onOpenLifeGroup))
            DrawerLink("📖", "My Content", onTap = nav(onOpenMyContent))
            DrawerLink("✅", "My Attendance", onTap = nav(onOpenMyAttendance))
            DrawerLink("📈", "My Progress", onTap = nav(onOpenMyProgress))
            DrawerLink("📝", "My Journal", onTap = nav(onOpenMyJournal))

            Spacer(Modifier.height(20.dp))
            SectionHeader("REMINDERS")
            ReminderRow(
                label = "Devotional",
                hour = state.reminderHour,
                onDecrement = { viewModel.setDevoReminderHour(state.reminderHour - 1) },
                onIncrement = { viewModel.setDevoReminderHour(state.reminderHour + 1) }
            )
            ReminderRow(
                label = "Prayer",
                hour = state.prayerReminderHour,
                onDecrement = { viewModel.setPrayerReminderHour(state.prayerReminderHour - 1) },
                onIncrement = { viewModel.setPrayerReminderHour(state.prayerReminderHour + 1) }
            )

            if (state.isLeader) {
                Spacer(Modifier.height(20.dp))
                SectionHeader("LEADER")
                DrawerLink("👥", "My Members", onTap = nav(onOpenMyMembers))
                DrawerLink("🎮", "Manage Questions", onTap = nav(onOpenManageContent))
            }

            if (state.isAdmin) {
                Spacer(Modifier.height(20.dp))
                SectionHeader("ADMIN")
                DrawerLink("🛡️", "Manage Users", accent = GraceRose,
                    onTap = nav(onOpenAdmin))
            }

            Spacer(Modifier.height(28.dp))
            // Footer divider + Settings.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(GraceCreamDim.copy(alpha = 0.18f))
            )
            Spacer(Modifier.height(8.dp))
            DrawerLink("⚙️", "Settings", onTap = nav(onOpenSettings))
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    email: String,
    role: String,
    onEdit: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(GraceGold.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.firstOrNull()?.uppercase() ?: "?",
                color = GraceCream, fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name.ifBlank { "—" },
                color = GraceCream, fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(email, color = GraceCreamDim, fontSize = 11.sp, maxLines = 1)
            Text(
                role.replace('_', ' ').replaceFirstChar { it.uppercase() },
                color = GraceGold, fontSize = 10.sp
            )
        }
        Text("Edit ›", color = GraceGold, fontSize = 11.sp)
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text, color = GraceCreamDim, fontSize = 10.sp,
        letterSpacing = 3.sp, fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun DrawerLink(
    emoji: String,
    label: String,
    accent: androidx.compose.ui.graphics.Color = GraceGold,
    onTap: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(vertical = 10.dp)
    ) {
        Text(emoji, fontSize = 16.sp)
        Spacer(Modifier.width(12.dp))
        Text(label, color = accent, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text("›", color = accent.copy(alpha = 0.5f), fontSize = 16.sp)
    }
}

@Composable
private fun ReminderRow(
    label: String,
    hour: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = GraceCream, fontSize = 13.sp,
            modifier = Modifier.weight(1f))
        StepperButton("−", onDecrement)
        Text(
            "%02d:00".format(hour),
            color = GraceGold, fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        StepperButton("+", onIncrement)
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(GraceCardBg, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = GraceGold, fontSize = 16.sp,
            fontWeight = FontWeight.Bold)
    }
}
