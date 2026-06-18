package com.grace.app.presentation.screens.settings

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.grace.app.BuildConfig
import com.grace.app.data.datastore.ThemeMode
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceGreen
import com.grace.app.presentation.theme.GraceRose
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    onNavigateLogin: () -> Unit,
    onBack: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenPrivacy: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest {
            if (it is SettingsEffect.NavigateToLogin) {
                activity?.recreate() ?: onNavigateLogin()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←", color = GraceCream, fontSize = 22.sp,
                modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
            )
            Text("Settings", color = GraceCream, fontSize = 26.sp)
        }

        Spacer(Modifier.height(20.dp))
        SectionHeader("PROFILE")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenEditProfile() }
                .padding(vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(GraceGold.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    state.name.firstOrNull()?.uppercase() ?: "?",
                    color = GraceCream, fontSize = 22.sp
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(state.name.ifBlank { "—" }, color = GraceCream, fontSize = 16.sp)
                Text(state.email, color = GraceCreamDim, fontSize = 12.sp)
                Text(
                    state.role.replace('_', ' ').replaceFirstChar { it.uppercase() },
                    color = GraceGold, fontSize = 11.sp
                )
            }
            Text("Edit ›", color = GraceGold, fontSize = 12.sp)
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader("SECURITY")
        if (state.canChangePassword) {
            Text(
                "Change Password",
                color = GraceGold,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showPasswordDialog = true }
                    .padding(vertical = 10.dp)
            )
        } else {
            Text(
                "Your password is managed by Google. To change it, open " +
                    "your Google account settings.",
                color = GraceCreamDim,
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader("NOTIFICATIONS")
        ToggleRow("Prayer Wall", state.notifPrayer) {
            viewModel.onEvent(SettingsEvent.ToggleNotif("prayer", it))
        }
        ToggleRow("Daily Devotional", state.notifDevo) {
            viewModel.onEvent(SettingsEvent.ToggleNotif("devo", it))
        }
        ToggleRow("Community", state.notifCommunity) {
            viewModel.onEvent(SettingsEvent.ToggleNotif("community", it))
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader("APPEARANCE")
        ThemeModeSelector(
            current = state.themeMode,
            onSelect = { viewModel.onEvent(SettingsEvent.SetThemeMode(it)) }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "System follows your device's dark-theme setting.",
            color = GraceCreamDim, fontSize = 11.sp
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader("TEXT SIZE")
        FontScaleSelector(
            current = state.fontScale,
            onSelect = { viewModel.onEvent(SettingsEvent.SetFontScale(it)) }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Aa  preview at this size",
            color = GraceCream,
            fontSize = 16.sp
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader("PRIVACY")
        Text(
            "Your journal entries are private to you — only you can read them — " +
                "and saved to your account, so they're restored if you reinstall " +
                "or switch phones.",
            color = GraceCreamDim,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Privacy & Guidelines",
            color = GraceGold,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenPrivacy() }
                .padding(vertical = 8.dp)
        )
        Text(
            "Read what data we collect, how it's used, and how we treat each " +
                "other on Royals.",
            color = GraceCreamDim, fontSize = 11.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Delete My Account",
            color = GraceRose,
            modifier = Modifier.clickable { showDeleteDialog = true }
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "Sign Out",
            color = GraceGold,
            modifier = Modifier.clickable { viewModel.onEvent(SettingsEvent.SignOut) }
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader("SUPPORT")
        Text(
            "Found a bug? Have feedback? Tap below to send a report with " +
                "the info I need to help you faster.",
            color = GraceCreamDim, fontSize = 12.sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "📨 Report a problem",
            color = GraceGold,
            modifier = Modifier.clickable {
                activity?.let { act ->
                    openBugReportEmail(
                        activity = act,
                        userEmail = state.email.ifBlank { "(unknown)" },
                        role = state.role,
                        appVersion = viewModel.appVersion
                    )
                }
            }
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader("ABOUT")
        Text(
            "Royals: The Kingdom Builders · v${viewModel.appVersion} (build ${BuildConfig.VERSION_CODE})",
            color = GraceCreamDim, fontSize = 12.sp
        )
        Text("Made with ❤️ for the Church in the Philippines",
            color = GraceCreamDim, fontSize = 12.sp)
        Spacer(Modifier.height(24.dp))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete account?") },
            text = {
                Text("This permanently removes your account and local data. " +
                    "This cannot be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.onEvent(SettingsEvent.DeleteAccount)
                }) { Text("Delete", color = GraceRose) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    state.deleteAccountError?.let { msg ->
        AlertDialog(
            onDismissRequest = {
                viewModel.onEvent(SettingsEvent.DismissDeleteAccountError)
            },
            title = { Text("Couldn't delete account") },
            text = {
                Text("$msg\n\nYour data is still on the server. Please try again, or contact your leader.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onEvent(SettingsEvent.DismissDeleteAccountError)
                }) { Text("OK", color = GraceRose) }
            }
        )
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            isWorking = state.isWorking,
            errorMessage = state.passwordChangeError,
            successShown = state.passwordChangeSuccess,
            onSubmit = { newPwd ->
                viewModel.onEvent(SettingsEvent.ChangePassword(newPwd))
            },
            onDismiss = {
                showPasswordDialog = false
                viewModel.onEvent(SettingsEvent.DismissPasswordResult)
            }
        )
        LaunchedEffect(state.passwordChangeSuccess) {
            if (state.passwordChangeSuccess) {
                kotlinx.coroutines.delay(1200)
                showPasswordDialog = false
                viewModel.onEvent(SettingsEvent.DismissPasswordResult)
            }
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    isWorking: Boolean,
    errorMessage: String?,
    successShown: Boolean,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val mismatch = confirm.isNotEmpty() && newPassword != confirm
    val tooShort = newPassword.isNotEmpty() && newPassword.length < 8
    val canSubmit = newPassword.length >= 8 && newPassword == confirm && !isWorking

    AlertDialog(
        onDismissRequest = { if (!isWorking) onDismiss() },
        title = { Text("Change Password") },
        text = {
            Column {
                if (successShown) {
                    Text("✓ Password updated.", color = GraceGreen)
                } else {
                    Text(
                        "Choose a new password — at least 8 characters.",
                        color = GraceCreamDim, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        isError = tooShort,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (tooShort) {
                        Text(
                            "Must be at least 8 characters.",
                            color = GraceRose, fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("Confirm password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        isError = mismatch,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (mismatch) {
                        Text("Passwords don't match.",
                            color = GraceRose, fontSize = 11.sp)
                    }
                    if (errorMessage != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("⚠ $errorMessage", color = GraceRose, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            if (!successShown) {
                TextButton(
                    enabled = canSubmit,
                    onClick = { onSubmit(newPassword) }
                ) {
                    Text(
                        if (isWorking) "Saving…" else "Update",
                        color = if (canSubmit) GraceGold else GraceCreamDim
                    )
                }
            }
        },
        dismissButton = {
            if (!successShown) {
                TextButton(onClick = onDismiss, enabled = !isWorking) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, color = GraceCreamDim, fontSize = 10.sp, letterSpacing = 3.sp,
        fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
}

private const val SUPPORT_EMAIL = "jhonclarencerulona19@gmail.com"

private fun openBugReportEmail(
    activity: Activity,
    userEmail: String,
    role: String,
    appVersion: String
) {
    val deviceLine = "${Build.MANUFACTURER} ${Build.MODEL}"
    val androidLine = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"

    val subject = "Royals app — bug report"
    val body = buildString {
        appendLine("Hi,")
        appendLine()
        appendLine("Please describe what happened (what you did, what you expected, what actually happened):")
        appendLine()
        appendLine()
        appendLine("---")
        appendLine("Don't edit below — this helps debug your issue:")
        appendLine("App version: $appVersion (build ${BuildConfig.VERSION_CODE})")
        appendLine("Device: $deviceLine")
        appendLine("System: $androidLine")
        appendLine("Reporter: $userEmail")
        appendLine("Role: $role")
    }

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(SUPPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    runCatching { activity.startActivity(intent) }
}

@Composable
private fun ThemeModeSelector(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val options = listOf(
        "Dark" to ThemeMode.DARK,
        "Light" to ThemeMode.LIGHT,
        "System" to ThemeMode.SYSTEM
    )
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (label, mode) ->
            val selected = current == mode
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(
                        if (selected) GraceGold
                        else com.grace.app.presentation.theme.GraceCardBg,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) GraceDeepBlue else GraceCream,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FontScaleSelector(
    current: Float,
    onSelect: (Float) -> Unit
) {
    val options = listOf(
        "Normal" to 1.0f,
        "Large" to 1.15f,
        "Largest" to 1.30f
    )
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (label, scale) ->
            val selected = kotlin.math.abs(current - scale) < 0.01f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(
                        if (selected) GraceGold
                        else com.grace.app.presentation.theme.GraceCardBg,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(scale) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) GraceDeepBlue else GraceCream,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = GraceCream)
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(checkedThumbColor = GraceGold)
        )
    }
}
