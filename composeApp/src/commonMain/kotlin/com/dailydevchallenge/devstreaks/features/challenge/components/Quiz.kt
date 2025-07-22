package com.dailydevchallenge.devstreaks.features.challenge.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dailydevchallenge.devstreaks.model.ChallengeActivity
import devstreaks.composeapp.generated.resources.Res
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import io.github.alexzhirkevich.compottie.*


@Composable
fun QuizCard(activity: ChallengeActivity, onComplete: () -> Unit) {
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var viewedOnce by remember { mutableStateOf(false) }
    var showConfetti by remember { mutableStateOf(false) }
    val isCorrect = selectedOption == activity.correctAnswer


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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
                            viewedOnce = true
                        }
                        if (isCorrect) {
                            showConfetti = true
                        }
                    },
                    enabled = selectedOption != null,
                    shape = RoundedCornerShape(20)
                ) { Text("Submit") }
            } else {
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
                if (!isCorrect) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            selectedOption = null
                            isSubmitted = false
                            showConfetti = false
                        },
                        shape = RoundedCornerShape(20)
                    ) {
                        Text("Retry")
                    }
                }
                if (isCorrect){
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20)
                    ) {
                        Text("Next")
//                        onComplete()
                    }
                }
            }
        }
    }
}

