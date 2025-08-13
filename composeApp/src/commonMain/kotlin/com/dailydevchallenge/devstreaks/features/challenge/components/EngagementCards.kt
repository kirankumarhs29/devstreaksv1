package com.dailydevchallenge.devstreaks.features.challenge.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CoachDialogueCard(level: Int, logicScore: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF4FF)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("🎯 DevCoach says:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("You're at Level $level with Logic score $logicScore%. Let’s win 2 mini battles today.")
        }
    }
}

@Composable
fun ChallengeStoryMissionCard(title: String, emoji: String, narrative: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFFAF5)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$emoji $title", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(narrative, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
