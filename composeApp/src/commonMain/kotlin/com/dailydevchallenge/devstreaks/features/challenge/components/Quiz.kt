package com.dailydevchallenge.devstreaks.features.challenge.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.ChallengeActivity

@Composable
fun QuizCard(activity: ChallengeActivity, onViewed: () -> Unit) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var viewedOnce by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🧠 Quiz", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(activity.prompt, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            activity.options?.forEach { option ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    RadioButton(
                        selected = selectedOption == option,
                        onClick = { if (!isSubmitted) selectedOption = option }
                    )
                    Text(option, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (!isSubmitted) {
                Button(
                    onClick = {
                        isSubmitted = true
                        if (!viewedOnce) {
                            onViewed()
                            viewedOnce = true
                        }
                    },
                    enabled = selectedOption != null,
                    shape = RoundedCornerShape(20)
                ) { Text("Submit") }
            } else {
                val isCorrect = selectedOption == activity.correctAnswer
                Text(
                    if (isCorrect) "✅ Correct!" else "❌ Incorrect",
                    color = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                activity.explanation?.let {
                    if (it.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("💡 $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

