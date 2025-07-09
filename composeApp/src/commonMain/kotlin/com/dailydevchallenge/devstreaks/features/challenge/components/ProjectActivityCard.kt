package com.dailydevchallenge.devstreaks.features.challenge.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.ChallengeActivity

@Composable
fun ProjectActivityCard(activity: ChallengeActivity, onViewed: () -> Unit) {
    var showResources by remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }

    LaunchedEffect(isSubmitted) {
        if (isSubmitted) onViewed()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🚀 Project Challenge", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(activity.prompt, style = MaterialTheme.typography.bodyLarge)
            activity.starterCode?.let {
                Spacer(Modifier.height(12.dp))
                Text("💻 Starter Code Snippet", style = MaterialTheme.typography.labelMedium)
                Surface(
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                ) {
                    Text(it.trim(), modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            activity.explanation?.let {
                Spacer(Modifier.height(12.dp))
                Text("📌 Why this project:", style = MaterialTheme.typography.labelSmall)
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = {
                showResources = !showResources
                isSubmitted = true
            }) {
                Text(if (showResources) "Hide Extra Resources" else "Show Extra Resources")
            }
            if (showResources) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    activity.videoUrl?.let { Text("🎥 Video: $it", style = MaterialTheme.typography.bodySmall) }
                    activity.language?.let { Text("🧰 Tech Stack: $it", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}
