package com.dailydevchallenge.devstreaks.features.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dailydevchallenge.devstreaks.auth.AuthService
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.utils.getLogger
import com.dailydevchallenge.devstreaks.utils.logAnalyticsEvent


@Composable
fun ForgotPasswordScreen(
    onLogin: () -> Unit,
    authService: AuthService = koinInject()
) {
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val logger = remember { getLogger() }

    // Track screen view
    LaunchedEffect(Unit) {
        logger.log("ForgotPasswordScreen viewed")
        logAnalyticsEvent("screen_viewed", mapOf("screen" to "ForgotPasswordScreen"))
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Reset Password", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Enter your email to receive a reset link", style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it; error = null; message = null },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            message?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    logger.log("Reset password attempt for email: $email")
                    logAnalyticsEvent("reset_password_attempted", mapOf("email" to email))
                    if (!isValidEmail(email)) {
                        error = "Please enter a valid email"
                        return@Button
                    }

                    isLoading = true
                    authService.sendPasswordResetEmail(email) { success, response ->
                        isLoading = false
                        if (success) {
                            message = "Reset link sent to your email"
                            logger.log("Reset link sent for email: $email")
                            logAnalyticsEvent("reset_password_sent", mapOf("email" to email))
                        } else {
                            error = response
                            logger.log("Reset password failed: $response")
                            logAnalyticsEvent("reset_password_failed", mapOf("email" to email,
                                "error" to response.toString()))
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Sending..." else "Send Reset Link")
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = {
                logger.log("Back to login clicked from ForgotPasswordScreen")
                logAnalyticsEvent("navigate_back_to_login")
                onLogin()
            }) {
                Text("Back to Login")
            }
        }
    }
}
