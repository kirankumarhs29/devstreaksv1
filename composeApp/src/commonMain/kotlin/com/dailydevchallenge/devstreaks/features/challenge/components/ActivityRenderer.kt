package com.dailydevchallenge.devstreaks.features.challenge.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.ActivityType
import com.dailydevchallenge.devstreaks.model.ChallengeActivity

@Composable
fun ActivityRenderer(activity: ChallengeActivity, onViewed: (String) -> Unit = {}) {
    Spacer(Modifier.height(16.dp))
    when (activity.type) {
        ActivityType.QUIZ -> QuizCard(activity, onViewed = { onViewed(activity.id) })
        ActivityType.CODE -> CodeChallengeCard(activity, onViewed = { onViewed(activity.id) })
        ActivityType.FLASHCARD -> FlashcardActivityCard(activity, onViewed = { onViewed(activity.id) })
        ActivityType.PROJECT -> ProjectActivityCard(activity, onViewed = { onViewed(activity.id) })
    }
    activity.videoUrl?.let {
        Spacer(Modifier.height(12.dp))
        Text("🎥 Learn More:", style = MaterialTheme.typography.labelMedium)
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }

    activity.explanation?.let {
        Spacer(Modifier.height(8.dp))
        Text("ℹ️ Explanation:", style = MaterialTheme.typography.labelSmall)
        Text(it, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ActivitySection(
    activities: List<ChallengeActivity>,
    onActivityViewed: (String) -> Unit
) {
    activities.forEach { activity ->
        ActivityRenderer(activity, onActivityViewed)
    }
}

