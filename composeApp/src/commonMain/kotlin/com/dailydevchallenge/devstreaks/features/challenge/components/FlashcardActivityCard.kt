package com.dailydevchallenge.devstreaks.features.challenge.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.ChallengeActivity

@Composable
fun FlashcardActivityCard(activity: ChallengeActivity, onViewed: () -> Unit) {
    var showBack by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onViewed()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { showBack = !showBack },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📚 Flashcard", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(!showBack) {
                    Text(activity.prompt, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                }
                AnimatedVisibility(showBack) {
                    Text(activity.explanation ?: "No explanation provided.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    if (!showBack) "👆 Tap to reveal answer" else "👆 Tap to hide",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        }
    }
}
