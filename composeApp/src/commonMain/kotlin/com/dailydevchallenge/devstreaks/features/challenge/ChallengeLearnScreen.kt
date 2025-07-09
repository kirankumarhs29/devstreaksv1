package com.dailydevchallenge.devstreaks.features.challenge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.features.navigation.DevStreakTopBar
import com.dailydevchallenge.devstreaks.model.ChallengeTask

@Composable
fun ChallengeLearnScreen(
    task: ChallengeTask,
    onNext: () -> Unit
) {
    Scaffold(
        topBar = {
            DevStreakTopBar(
                title = "Learn: Day ${task.day}"
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("📚 Learn This First", style = MaterialTheme.typography.headlineSmall)

            Spacer(Modifier.height(12.dp))
            Text(task.whyItMatters ?: "Why this challenge matters...", style = MaterialTheme.typography.bodyMedium)

            task.tip?.let {
                Spacer(Modifier.height(12.dp))
                Text("💡 Tip:", style = MaterialTheme.typography.labelLarge)
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            task.bonus?.let {
                Spacer(Modifier.height(12.dp))
                Text("🎁 Bonus:", style = MaterialTheme.typography.labelLarge)
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("🚀 Start Challenge")
            }
        }
    }
}


