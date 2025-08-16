package com.dailydevchallenge.devstreaks.features.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.*
import org.koin.compose.koinInject

/**
 * Predictive insights dashboard with LLM-powered learning analytics and recommendations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictiveInsightsDashboard(
    onNavigateToSkillTree: () -> Unit,
    onNavigateToProjects: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PredictiveInsightsViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val learningPredictions by viewModel.learningPredictions.collectAsState()
    val nextChallengePrediction by viewModel.nextChallengePrediction.collectAsState()
    val performanceTrends by viewModel.performanceTrends.collectAsState()
    val optimalSchedule by viewModel.optimalSchedule.collectAsState()
    val skillMasteryPredictions by viewModel.skillMasteryPredictions.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPredictiveInsights()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            PredictiveInsightsHeader(
                onGeneratePredictions = { viewModel.refreshPredictions() },
                onNavigateToSkillTree = onNavigateToSkillTree,
                onNavigateToProjects = onNavigateToProjects
            )
        }

        // Insight Summary Card
        item {
            learningPredictions?.let { predictions ->
                InsightSummaryCard(
                    predictions = predictions,
                    nextChallenge = nextChallengePrediction,
                    performanceTrends = performanceTrends
                )
            }
        }

        // Performance Trends
        item {
            performanceTrends?.let { trends ->
                PerformanceTrendsCard(trends = trends)
            }
        }

        // Next Challenge Recommendation
        item {
            nextChallengePrediction?.let { prediction ->
                NextChallengeCard(
                    prediction = prediction,
                    onAcceptChallenge = { /* TODO: Navigate to challenge */ }
                )
            }
        }

        // Optimal Learning Schedule
        item {
            optimalSchedule?.let { schedule ->
                OptimalScheduleCard(
                    schedule = schedule,
                    onUpdatePreferences = { time, difficulty ->
                        viewModel.updateSchedulePreferences(time, difficulty)
                    }
                )
            }
        }

        // Skill Mastery Predictions
        if (skillMasteryPredictions.isNotEmpty()) {
            item {
                Text(
                    text = "Skill Mastery Timeline",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            items(skillMasteryPredictions) { prediction ->
                SkillMasteryCard(prediction = prediction)
            }
        }

        // Recommendations
        item {
            learningPredictions?.recommendations?.let { recommendations ->
                if (recommendations.isNotEmpty()) {
                    RecommendationsCard(
                        recommendations = recommendations,
                        onApplyRecommendation = { /* TODO: Apply recommendation */ }
                    )
                }
            }
        }
    }

    // Loading overlay
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Analyzing your learning patterns...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // TODO: Show snackbar with error message
            viewModel.clearError()
        }
    }
}

@Composable
private fun PredictiveInsightsHeader(
    onGeneratePredictions: () -> Unit,
    onNavigateToSkillTree: () -> Unit,
    onNavigateToProjects: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI Learning Insights",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Personalized predictions and recommendations",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                IconButton(
                    onClick = onGeneratePredictions,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Generate Insights",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToSkillTree,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AccountTree, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Skill Tree")
                }

                OutlinedButton(
                    onClick = onNavigateToProjects,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Work, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Projects")
                }
            }
        }
    }
}

@Composable
private fun InsightSummaryCard(
    predictions: LearningPredictions,
    nextChallenge: ChallengePrediction?,
    performanceTrends: PerformanceTrends?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Learning Insights Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InsightMetric(
                    label = "Performance",
                    value = when (performanceTrends?.overallTrend) {
                        TrendDirection.IMPROVING -> "↗️ Improving"
                        TrendDirection.DECLINING -> "↘️ Declining"
                        else -> "→ Stable"
                    },
                    color = when (performanceTrends?.overallTrend) {
                        TrendDirection.IMPROVING -> MaterialTheme.colorScheme.primary
                        TrendDirection.DECLINING -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondary
                    }
                )

                InsightMetric(
                    label = "Confidence",
                    value = "${(predictions.confidenceScore * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.primary
                )

                InsightMetric(
                    label = "Next Goal",
                    value = predictions.difficultyProgression.nextMilestone.name,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun InsightMetric(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PerformanceTrendsCard(
    trends: PerformanceTrends
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Performance Trends",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Overall trend: ${trends.overallTrend.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Plateau risk: ${(trends.plateauRisk * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    trends.plateauRisk > 0.7 -> MaterialTheme.colorScheme.error
                    trends.plateauRisk > 0.4 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}

@Composable
private fun NextChallengeCard(
    prediction: ChallengePrediction,
    onAcceptChallenge: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Recommended Next Challenge",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = prediction.challengeType,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Success probability: ${(prediction.successProbability * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = onAcceptChallenge,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Challenge")
            }
        }
    }
}

@Composable
private fun OptimalScheduleCard(
    schedule: LearningSchedule,
    onUpdatePreferences: (Int, DifficultyLevel?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Optimal Learning Schedule",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Focus areas: ${schedule.priorityAreas.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = schedule.difficultyProgression,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SkillMasteryCard(
    prediction: SkillMasteryPrediction
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = prediction.skillArea,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = prediction.currentLevel.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Text(
                text = "${prediction.estimatedHoursToMastery}h to mastery",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Confidence: ${(prediction.confidenceScore * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecommendationsCard(
    recommendations: List<LearningRecommendation>,
    onApplyRecommendation: (LearningRecommendation) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "AI Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            recommendations.take(3).forEach { recommendation ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = recommendation.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Impact: ${recommendation.expectedImpact}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TextButton(
                        onClick = { onApplyRecommendation(recommendation) }
                    ) {
                        Text("Apply")
                    }
                }

                if (recommendation != recommendations.take(3).last()) {
                    HorizontalDivider()
                }
            }
        }
    }
}
