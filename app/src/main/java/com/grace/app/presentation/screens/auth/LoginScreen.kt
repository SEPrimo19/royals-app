package com.grace.app.presentation.screens.auth

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.grace.app.R
import com.grace.app.presentation.components.GraceButton
import com.grace.app.presentation.components.GraceTextField
import com.grace.app.di.SupabaseEntryPoint
import com.grace.app.presentation.theme.GraceCardBg
import com.grace.app.presentation.theme.GraceCream
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGreen
import androidx.compose.ui.text.font.FontWeight
import com.grace.app.presentation.theme.GraceGold
import dagger.hilt.android.EntryPointAccessors
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
import io.github.jan.supabase.compose.auth.composeAuth
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    onNavigateHome: () -> Unit,
    onNavigateSignUp: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                LoginEffect.NavigateToHome -> onNavigateHome()
                LoginEffect.NavigateToSignUp -> onNavigateSignUp()
                is LoginEffect.ShowError -> Unit
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GraceDeepBlue)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(40.dp))
        Image(
            painter = painterResource(id = R.drawable.royals_logo_official),
            contentDescription = "Royals: The Kingdom Builders logo",
            modifier = Modifier.size(160.dp)
        )
        Text(
            "Connect. Pray. Grow.",
            color = GraceCreamDim,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(32.dp))

        GraceTextField(
            value = state.email,
            onValueChange = { viewModel.onEvent(LoginEvent.EmailChanged(it)) },
            label = "Email",
            error = state.emailError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        Spacer(Modifier.height(16.dp))
        GraceTextField(
            value = state.password,
            onValueChange = { viewModel.onEvent(LoginEvent.PasswordChanged(it)) },
            label = "Password",
            error = state.passwordError,
            isPassword = true,
            passwordVisible = state.isPasswordVisible,
            onTogglePasswordVisibility = {
                viewModel.onEvent(LoginEvent.PasswordVisibilityToggled)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            )
        )

        if (state.generalError != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "⚠ ${state.generalError}",
                color = com.grace.app.presentation.theme.GraceRose,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { viewModel.onEvent(LoginEvent.OpenForgotPasswordDialog) },
                enabled = !state.isLoading
            ) {
                Text(
                    "Forgot password?",
                    color = GraceGold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        GraceButton(
            text = "Sign In",
            onClick = { viewModel.onEvent(LoginEvent.LoginClicked) },
            loading = state.isLoading
        )

        Spacer(Modifier.height(20.dp))
        OrDivider()
        Spacer(Modifier.height(16.dp))
        GoogleSignInButton(enabled = !state.isLoading) { result ->
            when (result) {
                NativeSignInResult.Success -> Unit
                is NativeSignInResult.Error -> viewModel.onEvent(
                    LoginEvent.GoogleSignInFailed(friendlyOAuthError(result.message))
                )
                is NativeSignInResult.ClosedByUser -> Unit
                is NativeSignInResult.NetworkError -> viewModel.onEvent(
                    LoginEvent.GoogleSignInFailed("Network error. Try again.")
                )
            }
        }

        TextButton(
            onClick = { viewModel.onEvent(LoginEvent.NavigateToSignUp) },
            enabled = !state.isLoading
        ) {
            Text("Don't have an account? Sign Up", color = GraceGold)
        }
        Spacer(Modifier.height(40.dp))
    }

    if (state.showForgotPasswordDialog) {
        ForgotPasswordDialog(
            email = state.forgotPasswordEmail,
            errorMessage = state.forgotPasswordError,
            isSending = state.isSendingPasswordReset,
            sent = state.passwordResetSent,
            onEmailChange = {
                viewModel.onEvent(LoginEvent.ForgotPasswordEmailChanged(it))
            },
            onSend = { viewModel.onEvent(LoginEvent.SendPasswordReset) },
            onDismiss = { viewModel.onEvent(LoginEvent.CloseForgotPasswordDialog) }
        )
    }
}

@Composable
private fun ForgotPasswordDialog(
    email: String,
    errorMessage: String?,
    isSending: Boolean,
    sent: Boolean,
    onEmailChange: (String) -> Unit,
    onSend: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (sent) "✓ Check your email" else "Reset your password",
                color = if (sent) GraceGreen else GraceCream,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (sent) {
                    Text(
                        "If an account exists for that email, you'll receive a link to reset your password. The link expires in 1 hour.",
                        color = GraceCreamDim, fontSize = 13.sp, lineHeight = 18.sp
                    )
                } else {
                    Text(
                        "Enter the email you signed up with. We'll send a link to reset your password.",
                        color = GraceCreamDim, fontSize = 13.sp, lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = onEmailChange,
                        singleLine = true,
                        enabled = !isSending,
                        placeholder = {
                            Text("you@example.com", color = GraceCreamDim)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "⚠ $errorMessage",
                            color = com.grace.app.presentation.theme.GraceRose,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (sent) {
                TextButton(onClick = onDismiss) {
                    Text("Done", color = GraceGold, fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(
                    onClick = onSend,
                    enabled = !isSending && email.isNotBlank()
                ) {
                    Text(
                        if (isSending) "Sending…" else "Send link",
                        color = GraceGold, fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            if (!sent) {
                TextButton(onClick = onDismiss, enabled = !isSending) {
                    Text("Cancel", color = GraceCreamDim)
                }
            }
        }
    )
}

@OptIn(SupabaseExperimental::class)
@Composable
internal fun GoogleSignInButton(
    enabled: Boolean = true,
    onResult: (NativeSignInResult) -> Unit
) {
    val context = LocalContext.current
    val supabase: SupabaseClient = remember(context) {
        EntryPointAccessors
            .fromApplication(context.applicationContext, SupabaseEntryPoint::class.java)
            .supabaseClient()
    }
    val action = supabase.composeAuth.rememberSignInWithGoogle(onResult = onResult)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, GraceCreamDim, RoundedCornerShape(12.dp))
            .background(GraceCardBg, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { action.startFlow() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text("G", color = GraceGold, fontSize = 18.sp)
        Spacer(Modifier.height(0.dp))
        Text(
            "    Continue with Google",
            color = GraceCream,
            fontSize = 15.sp
        )
    }
}

internal fun friendlyOAuthError(raw: String): String {
    val lower = raw.lowercase()
    return when {
        lower.contains("timeout") ->
            "Sign-in took too long. Check your connection and try again."
        lower.contains("no credentials available") ||
            lower.contains("no matching credential") ->
            "No Google account is set up for this app yet. Ask the admin to finish Google sign-in setup, then try again."
        lower.contains("provider is not enabled") ->
            "Google sign-in isn't enabled yet. Please contact your church admin."
        lower.contains("idp validation failed") ||
            lower.contains("invalid id token") ->
            "Google rejected this sign-in. Try again or use email + password."
        lower.contains("cancel") ->
            "Sign-in cancelled."
        else ->
            "Couldn't sign in with Google. Try again or use email + password."
    }
}

@Composable
internal fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(GraceCreamDim.copy(alpha = 0.3f))
        )
        Text(
            "  or  ",
            color = GraceCreamDim,
            fontSize = 12.sp
        )
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(GraceCreamDim.copy(alpha = 0.3f))
        )
    }
}
