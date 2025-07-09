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
import com.dailydevchallenge.devstreaks.settings.UserPreferences

import com.dailydevchallenge.devstreaks.auth.AuthResult
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.dailydevchallenge.devstreaks.auth.AuthService
import com.dailydevchallenge.devstreaks.notification.getNotificationScheduler
import com.dailydevchallenge.devstreaks.features.routes.Routes
import com.dailydevchallenge.devstreaks.session.getSessionManager
import com.dailydevchallenge.devstreaks.utils.logAnalyticsEvent
import com.dailydevchallenge.devstreaks.utils.getLogger


@Composable
fun LoginScreen(
    onSignupClick: () -> Unit,
    navController: NavController,
    onLoginSuccess: () -> Unit
) {
    val authService: AuthService = koinInject()
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val logger = remember { getLogger() }
    // 🔥 Log screen viewed once on first render
    LaunchedEffect(Unit) {
        logger.log("LoginScreen viewed")
        logAnalyticsEvent("screen_viewed", mapOf("screen" to "LoginScreen"))
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // 🎯 Placeholder for banner illustration
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

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null
                        )
                    }
                }
            )
            TextButton(
                onClick = {
                    navController.navigate(Routes.ForgotPassword)
                    logger.log("Forgot password clicked on LoginScreen")
                    logAnalyticsEvent("forgot_password_clicked")
                }
            ) {
                Text("Forgot Password?")
            }


            error?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    logger.log("Login button clicked: email=$email")
                    logAnalyticsEvent("login_attempted", mapOf("email" to email))
                    when {
                        !isValidEmail(email) -> error = "Enter a valid email."
                        !isValidPassword(password) -> error = "Password must be at least 8 characters."
                        else -> {
                            isLoading = true
                            coroutineScope.launch {
                                when (val result = authService.login(email, password)) {
                                    is AuthResult.Success -> {
                                        logger.log("Login successful for user=${result.userId}")
                                        logAnalyticsEvent("login_success", mapOf("user_id" to result.userId))
                                        onLoginSuccess()
                                        UserPreferences.setLoggedIn(true)
                                        UserPreferences.setUserId(result.userId)
                                        getSessionManager().saveToken(result.token)
                                        getNotificationScheduler().scheduleOneTimeNotification("you logged in successfully","Welcome back to DevStreak!", "login")
                                    }
                                    is AuthResult.Error -> {
                                        isLoading = false
                                        error = result.message
                                        logger.log("Login failed: ${result.message}")
                                        logAnalyticsEvent("login_failed", mapOf("reason" to result.message))
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(if (isLoading) "Logging in..." else "🔥 Continue Streak")
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = {
                logger.log("Navigated to Signup from LoginScreen")
                logAnalyticsEvent("signup_from_login_clicked")
                onSignupClick()
            }) {

                Text("New here? Join the movement →")
            }
        }
    }
}



fun isValidEmail(email: String): Boolean {
    val emailPattern = Regex("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
    return emailPattern.matches(email)
}

fun isValidPassword(password: String): Boolean {
    return password.length >= 8
}
