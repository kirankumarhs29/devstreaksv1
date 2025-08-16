package com.dailydevchallenge.devstreaks.features.challenge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.dailyCoach.DevCoachLottieAvatar
import com.dailydevchallenge.devstreaks.features.navigation.DevStreakTopBar
import com.dailydevchallenge.devstreaks.model.ChallengeTask

@Composable
fun ChallengeLearnScreen(
    task: ChallengeTask,
    onNext: () -> Unit,
    navController: NavController,
    // Phase 2 adaptive intelligence parameters
    adaptiveConfig: com.dailydevchallenge.devstreaks.model.AdaptiveChallengeConfig? = null,
    personalizedCoaching: com.dailydevchallenge.devstreaks.model.PersonalizedCoachingResponse? = null
) {
    Scaffold(
        topBar = {
            DevStreakTopBar(
                title = "Learn: Day ${task.day}",
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Adaptive difficulty indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("📚 Learn This First", style = MaterialTheme.typography.headlineSmall)

                // Show adaptive difficulty badge
                adaptiveConfig?.let { config ->
                    val difficultyColor = when (config.recommendedDifficulty) {
                        com.dailydevchallenge.devstreaks.model.DifficultyLevel.EASY -> MaterialTheme.colorScheme.primary
                        com.dailydevchallenge.devstreaks.model.DifficultyLevel.MEDIUM -> MaterialTheme.colorScheme.secondary
                        com.dailydevchallenge.devstreaks.model.DifficultyLevel.HARD -> MaterialTheme.colorScheme.tertiary
                        com.dailydevchallenge.devstreaks.model.DifficultyLevel.EXPERT -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = difficultyColor.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = "${config.recommendedDifficulty.name} MODE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = difficultyColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Show personalized learning tip if available
            personalizedCoaching?.let { coaching ->
                if (coaching.learningTips.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "🎯 Personalized for You",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.height(4.dp))
                            coaching.learningTips.forEach { tip ->
                                Text(
                                    "• $tip",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Show weak areas to focus on
            adaptiveConfig?.let { config ->
                if (config.weakAreas.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "💡 Areas to Focus On",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.height(4.dp))
                            config.weakAreas.forEach { area ->
                                Text(
                                    "• ${area.skillArea}: ${area.recommendedActions.firstOrNull() ?: "Focus on improving this skill"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            Text(task.whyItMatters ?: "Why this challenge matters...", style = MaterialTheme.typography.bodyMedium)
            DevCoachLottieAvatar()

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
                val nextText = when (adaptiveConfig?.recommendedDifficulty) {
                    com.dailydevchallenge.devstreaks.model.DifficultyLevel.EASY -> "🌱 Let's Practice!"
                    com.dailydevchallenge.devstreaks.model.DifficultyLevel.MEDIUM -> "🚀 Ready to Code!"
                    com.dailydevchallenge.devstreaks.model.DifficultyLevel.HARD -> "💪 Challenge Accepted!"
                    com.dailydevchallenge.devstreaks.model.DifficultyLevel.EXPERT -> "🔥 Expert Mode!"
                    else -> "🚀 Let's Code!"
                }
                Text(nextText)
            }
        }
    }
}
