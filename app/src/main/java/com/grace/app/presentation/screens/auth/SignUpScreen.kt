package com.grace.app.presentation.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import com.grace.app.presentation.theme.GraceCreamDim
import com.grace.app.presentation.theme.GraceDeepBlue
import com.grace.app.presentation.theme.GraceGold
import com.grace.app.presentation.theme.GraceRose
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SignUpScreen(
    onNavigateProfileSetup: () -> Unit,
    onNavigateLogin: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                SignUpEffect.NavigateToProfileSetup -> onNavigateProfileSetup()
                SignUpEffect.NavigateToLogin -> onNavigateLogin()
                is SignUpEffect.ShowError -> Unit
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
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text("Create your account", color = GraceGold, fontSize = 28.sp)
        Text(
            "Join your church youth community",
            color = GraceCreamDim,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(32.dp))

        GraceTextField(
            value = state.name,
            onValueChange = { viewModel.onEvent(SignUpEvent.NameChanged(it)) },
            label = "Full Name",
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        Spacer(Modifier.height(14.dp))
        GraceTextField(
            value = state.email,
            onValueChange = { viewModel.onEvent(SignUpEvent.EmailChanged(it)) },
            label = "Email",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            )
        )
        Spacer(Modifier.height(14.dp))
        GraceTextField(
            value = state.password,
            onValueChange = { viewModel.onEvent(SignUpEvent.PasswordChanged(it)) },
            label = "Password",
            isPassword = true,
            passwordVisible = state.isPasswordVisible,
            onTogglePasswordVisibility = {
                viewModel.onEvent(SignUpEvent.PasswordVisibilityToggled)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            )
        )
        Spacer(Modifier.height(14.dp))
        GraceTextField(
            value = state.confirmPassword,
            onValueChange = { viewModel.onEvent(SignUpEvent.ConfirmPasswordChanged(it)) },
            label = "Confirm Password",
            isPassword = true,
            passwordVisible = state.isConfirmVisible,
            onTogglePasswordVisibility = {
                viewModel.onEvent(SignUpEvent.ConfirmVisibilityToggled)
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
                color = GraceRose,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(24.dp))
        GraceButton(
            text = "Create Account",
            onClick = { viewModel.onEvent(SignUpEvent.SignUpClicked) },
            loading = state.isLoading
        )

        Spacer(Modifier.height(20.dp))
        OrDivider()
        Spacer(Modifier.height(16.dp))
        GoogleSignInButton(enabled = !state.isLoading) { result ->
            when (result) {
                io.github.jan.supabase.compose.auth.composable.NativeSignInResult.Success ->
                    Unit // MainActivity reactively switches to MAIN_GRAPH
                is io.github.jan.supabase.compose.auth.composable.NativeSignInResult.Error ->
                    viewModel.onEvent(SignUpEvent.GoogleSignInFailed(
                        friendlyOAuthError(result.message)
                    ))
                is io.github.jan.supabase.compose.auth.composable.NativeSignInResult.ClosedByUser ->
                    Unit
                is io.github.jan.supabase.compose.auth.composable.NativeSignInResult.NetworkError ->
                    viewModel.onEvent(
                        SignUpEvent.GoogleSignInFailed("Network error. Try again.")
                    )
            }
        }

        TextButton(
            onClick = { viewModel.onEvent(SignUpEvent.NavigateToLogin) },
            enabled = !state.isLoading
        ) {
            Text("Already have an account? Sign In", color = GraceGold)
        }
        Spacer(Modifier.height(40.dp))
    }
}
