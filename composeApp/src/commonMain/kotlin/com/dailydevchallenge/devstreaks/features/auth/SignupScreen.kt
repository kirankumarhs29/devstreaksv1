package com.dailydevchallenge.devstreaks.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.dailydevchallenge.devstreaks.auth.AuthResult
import com.dailydevchallenge.devstreaks.auth.AuthService
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.dailydevchallenge.devstreaks.utils.getLogger
import com.dailydevchallenge.devstreaks.utils.logAnalyticsEvent


@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    val authService: AuthService = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val logger = remember { getLogger() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Log screen view once
    LaunchedEffect(Unit) {
        logger.log("SignupScreen viewed")
        logAnalyticsEvent("screen_viewed", mapOf("screen" to "SignupScreen"))
    }


    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .focusable(), // important for keyboard focus behavior
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🚀", fontSize = 64.sp)
            }

            Spacer(Modifier.height(24.dp))

            Text("Let’s Get Started 🎯", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Your dev growth starts here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = null },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; error = null },
                label = { Text("Confirm Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    logger.log("Signup attempt: email=$email")
                    logAnalyticsEvent("signup_attempted", mapOf("email" to email))

                    when {
                        !isValidEmail(email) -> error = "Enter a valid email"
                        !isValidPassword(password) -> error = "Password must be at least 8 characters"
                        password != confirmPassword -> error = "Passwords do not match"
                        else -> {
                            isLoading = true
                            coroutineScope.launch {
                                when (val result = authService.signup(email, password)) {
                                    is AuthResult.Success -> {
                                        logger.log("Signup success: user=${result.userId}")
                                        logAnalyticsEvent("signup_success", mapOf("user_id" to result.userId))
                                        onSignupSuccess()
                                    }
                                    is AuthResult.Error -> {
                                        isLoading = false
                                        error = result.message
                                        logger.log("Signup failed: ${result.message}")
                                        logAnalyticsEvent("signup_failed", mapOf("reason" to result.message))
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
                Text(if (isLoading) "Creating account..." else "💪 Start DevStreak")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = {
                logger.log("Login CTA clicked from SignupScreen")
                logAnalyticsEvent("login_from_signup_clicked")
                onLoginClick()
            }) {
                Text("Already a streaker? Log In →")
            }

            Spacer(modifier = Modifier.height(32.dp)) // enough space at bottom
        }
    }
}

