package com.dailydevchallenge.devstreaks.features.challenge

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign


@Composable
fun ChallengeLoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated Progress
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Bold, Motivating Text
        Text(
            text = "🚀 Generating Your DevStreak Path...",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic, Rotating Dev Quotes or Fun Facts
        val messages = listOf(
            "“Code is like humor. When you have to explain it, it’s bad.”",
            "Your XP journey starts in a moment...",
            "Optimizing your streak logic...",
            "Unlocking your first dev challenge...",
            "Almost done! Just compiling awesomeness..."
        )
        val message by remember { mutableStateOf(messages.random()) }

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

