package com.dailydevchallenge.devstreaks.features

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.home.HomeViewModel
import com.dailydevchallenge.devstreaks.model.CoachingContextType
import org.koin.compose.koinInject
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.dailydevchallenge.devstreaks.features.routes.Routes

@Composable
fun ProgressScreen(
    navController: NavController,
    viewModel: HomeViewModel = koinInject()
) {
    val stats by viewModel.userStats.collectAsState()
    // Fix: Use correct properties from model UserStats
    val level = stats.level
    val nextXP = 100 - (stats.totalXp % 100)
    val progress = (stats.totalXp % 100).toFloat() / 100f
    val animatedProgress by animateFloatAsState(targetValue = progress)
    val eta by viewModel.estimatedEndDate.collectAsState()
    val completedCount by viewModel.completedTaskIds.collectAsState()
    val track by viewModel.currentTrack.collectAsState()
    val scrollState = rememberScrollState()

    // Integrate adaptive intelligence for enhanced progress insights
    val adaptiveOrchestrator: com.dailydevchallenge.devstreaks.service.AdaptiveIntelligenceOrchestrator = koinInject()
    val userId = com.dailydevchallenge.devstreaks.settings.UserPreferences.getSafeUserId()
    var weakAreas by remember { mutableStateOf<List<com.dailydevchallenge.devstreaks.model.WeakArea>>(emptyList()) }
    var adaptiveInsights by remember { mutableStateOf<com.dailydevchallenge.devstreaks.model.PersonalizedCoachingResponse?>(null) }
    var adaptiveXpMultiplier by remember { mutableStateOf(1.0) }

    // Load adaptive intelligence data
    LaunchedEffect(Unit) {
        try {
            // Get weak areas for progress display
            weakAreas = adaptiveOrchestrator.detectWeakAreas(userId)

            // Get adaptive insights for progress coaching
            adaptiveInsights = adaptiveOrchestrator.getPersonalizedCoaching(
                userId = userId,
                userMessage = "Provide progress review insights for user at level $level with ${completedCount.size} completed challenges",
                contextType = CoachingContextType.PROGRESS_REVIEW,
                learningProfile = null
            )

            // Calculate average adaptive XP multiplier from recent challenges
            // This would ideally come from performance metrics
            adaptiveXpMultiplier = 1.2 // Placeholder - would be calculated from actual performance

        } catch (e: Exception) {
            // Handle error silently for better UX
        }
    }

    val quote = remember {
        listOf(
            "Every expert was once a beginner.",
            "Consistency beats intensity.",
            "You're not behind — you're building.",
            "Progress, not perfection."
        ).random()
    }
    val pastWeekData = listOf(10, 30, 50, 70, 60, 80, 90) // dummy sparkline data

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Enhanced Progress Header with Adaptive Insights
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            "Level $level Developer",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Track: ${track ?: "General"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    // Adaptive XP Multiplier Badge
                    if (adaptiveXpMultiplier > 1.0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                            )
                        ) {
                            Text(
                                "🚀 ${(adaptiveXpMultiplier * 10).toInt() / 10.0}x XP",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Adaptive Progress Insights
                adaptiveInsights?.let { insights ->
                    if (insights.motivationalBoost?.isNotEmpty() == true) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                "💪 ${insights.motivationalBoost}",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                Text(quote, style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)
            }
        }

        // XP Progress with Adaptive Multiplier Info
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("XP Progress", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${stats.totalXp} XP ${if (adaptiveXpMultiplier > 1.0) "(+${((adaptiveXpMultiplier - 1.0) * 100).toInt()}% bonus)" else ""}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = animatedProgress,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$nextXP XP until Level ${level + 1}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Weak Areas & Focus Section
        if (weakAreas.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "🎯 Areas to Focus On",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(8.dp))

                    weakAreas.take(3).forEach { area ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        area.skillArea,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "Severity: ${(area.severityScore * 100).toInt()}% | Occurrences: ${area.occurrenceCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Progress indicator for this weak area
                                val weakAreaProgress = ((1.0 - area.severityScore) * 100).toInt()
                                Text(
                                    "$weakAreaProgress%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        weakAreaProgress < 30 -> MaterialTheme.colorScheme.error
                                        weakAreaProgress < 70 -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.tertiary
                                    }
                                )
                            }
                        }
                    }

                    if (weakAreas.size > 3) {
                        Text(
                            "+ ${weakAreas.size - 3} more areas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        // Strategic Guidance Section
        adaptiveInsights?.let { insights ->
            if (insights.personalizedRecommendations.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🧠 AI Strategic Guidance",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.height(8.dp))

                        insights.personalizedRecommendations.take(2).forEach { guidance ->
                            Text(
                                "• $guidance",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Current Streak
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Current Streak", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("${completedCount.size} days", style = MaterialTheme.typography.headlineMedium)
                Text("Keep it up! 🔥", style = MaterialTheme.typography.bodySmall)
            }
        }

        // ETA Section
        eta?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estimated Completion", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(it.toString(), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        // Weekly Progress Chart
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("This Week", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                SparklineChart(
                    data = pastWeekData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
            }
        }

        // Action Items
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { navController.navigate(Routes.DevCoach) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("💬 Chat with DevCoach")
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        // Navigate to challenge screen with adaptive intelligence
                        navController.navigate(Routes.ChallengePath)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🚀 Start Challenge")
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SparklineChart(
    data: List<Int>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepWidth = width / (data.size - 1)
        val maxValue = data.maxOrNull() ?: 1

        for (i in 0 until data.size - 1) {
            val startX = i * stepWidth
            val startY = height - (data[i].toFloat() / maxValue * height)
            val endX = (i + 1) * stepWidth
            val endY = height - (data[i + 1].toFloat() / maxValue * height)

            drawLine(
                color = androidx.compose.ui.graphics.Color.Blue,
                start = androidx.compose.ui.geometry.Offset(startX, startY),
                end = androidx.compose.ui.geometry.Offset(endX, endY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Typing indicator for enhanced chat experience
 */
@Composable
fun TypingIndicator() {
    Card(
        modifier = Modifier.fillMaxWidth(0.6f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "DevCoach is typing",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            // Simple typing animation dots
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            androidx.compose.foundation.shape.CircleShape
                        )
                )
                if (index < 2) Spacer(Modifier.width(2.dp))
            }
        }
    }
}
