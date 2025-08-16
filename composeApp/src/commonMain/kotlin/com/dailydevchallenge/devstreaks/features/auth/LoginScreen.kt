package com.dailydevchallenge.devstreaks.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.routes.Routes
import com.dailydevchallenge.devstreaks.session.getSessionManager
import com.dailydevchallenge.devstreaks.utils.logAnalyticsEvent
import com.dailydevchallenge.devstreaks.utils.getLogger
import com.dailydevchallenge.devstreaks.features.auth.LoginViewModel

@Composable
private fun LoginHeader() {
    Spacer(Modifier.height(32.dp))
    Box(
        modifier = Modifier
            .size(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Text("👨‍💻", fontSize = 64.sp)
    }
    Spacer(Modifier.height(24.dp))
    Text("Welcome Back 👋", style = MaterialTheme.typography.headlineMedium)
    Text(
        "Continue building your DevStreak",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
    )
    Spacer(Modifier.height(32.dp))
}

@Composable
private fun EmailField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Email") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("Password") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = null
                )
            }
        }
    )
}

@Composable
private fun ErrorText(error: String?) {
    error?.let {
        Spacer(modifier = Modifier.height(6.dp))
        Text(it, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun LoginButton(isLoading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = !isLoading
    ) {
        Text(if (isLoading) "Logging in..." else "🔥 Continue Streak")
    }
}

@Composable
private fun SignupPrompt(onSignupClick: () -> Unit) {
    TextButton(onClick = onSignupClick) {
        Text("New here? Join the movement →")
    }
}

@Composable
private fun ForgotPasswordButton(onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text("Forgot Password?")
    }
}

@Composable
fun LoginScreen(
    onSignupClick: () -> Unit,
    navController: NavController,
    onLoginSuccess: () -> Unit
) {
    val viewModel: LoginViewModel = remember { LoginViewModel() }
    val uiState by viewModel.uiState.collectAsState()

    // 🔥 Log screen viewed once on first render
    LaunchedEffect(Unit) {
        viewModel.clearError()
        // You may want to move analytics/logging to ViewModel if needed
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LoginHeader()
            EmailField(value = uiState.email, onValueChange = viewModel::onEmailChange)
            PasswordField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                isVisible = uiState.isPasswordVisible,
                onToggleVisibility = viewModel::onTogglePasswordVisibility
            )
            ForgotPasswordButton(onClick = { navController.navigate(Routes.ForgotPassword) })
            ErrorText(error = uiState.error)
            Spacer(modifier = Modifier.height(24.dp))
            LoginButton(isLoading = uiState.isLoading) { viewModel.login { onLoginSuccess() } }
            Spacer(modifier = Modifier.height(12.dp))
            SignupPrompt(onSignupClick = onSignupClick)
        }
    }
}
