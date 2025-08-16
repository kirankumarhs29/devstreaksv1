package com.dailydevchallenge.devstreaks.features.challenge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.koinInject
import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.settings.UserPreferences

/**
 * Adaptive Challenge Screen with integrated AI coaching, difficulty adjustment,
 * real-time feedback, and personalized guidance
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveChallengeScreen(
    modifier: Modifier = Modifier,
    viewModel: AdaptiveChallengeViewModel = koinInject(),
    onNavigateBack: () -> Unit = {},
    initialSkillArea: String? = null,
    initialDifficulty: DifficultyLevel? = null
) {
    val currentChallenge by viewModel.currentChallenge.collectAsState(null)
    val adaptiveConfig by viewModel.adaptiveConfig.collectAsState(null)
    val realTimeResponse by viewModel.realTimeResponse.collectAsState(null)
    val personalizedCoaching by viewModel.personalizedCoaching.collectAsState(null)
    val isLoading by viewModel.isLoading.collectAsState(false)
    val currentAttempts by viewModel.currentAttempts.collectAsState(0)
    val hintsUsed by viewModel.hintsUsed.collectAsState(0)
    val adaptiveHints by viewModel.adaptiveHints.collectAsState(emptyList())
    val motivationalMessage by viewModel.motivationalMessage.collectAsState(null)
    val totalXP by viewModel.totalXP.collectAsState(0)
    val shouldShowInsights by viewModel.shouldShowInsights.collectAsState(false)

    // Initialize adaptive challenge on first composition
    LaunchedEffect(Unit) {
        val userId = UserPreferences.getSafeUserId()
        viewModel.initializeAdaptiveChallenge(
            userId = userId,
            skillArea = initialSkillArea,
            requestedDifficulty = initialDifficulty
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with adaptive insights
        AdaptiveHeaderSection(
            adaptiveConfig = adaptiveConfig,
            onNavigateBack = onNavigateBack
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Loading state
        if (isLoading) {
            LoadingSection()
            return@Column
        }

        // Current challenge section
        currentChallenge?.let { challenge ->
            AdaptiveChallengeCard(
                challenge = challenge,
                adaptiveConfig = adaptiveConfig,
                currentAttempts = currentAttempts,
                hintsUsed = hintsUsed,
                totalXP = totalXP,
                onAttemptSubmitted = { userAnswer, isCorrect ->
                    val userId = UserPreferences.getSafeUserId()
                    viewModel.processAttempt(userId, userAnswer, isCorrect)
                },
                onHintRequested = {
                    val userId = UserPreferences.getSafeUserId()
                    viewModel.useHint(userId)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Real-time feedback section
        realTimeResponse?.let { response ->
            RealTimeFeedbackSection(
                response = response,
                adaptiveHints = adaptiveHints
            )
        }

        // Motivational message
        motivationalMessage?.let { message ->
            MotivationalMessageCard(message = message)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Adaptive insights panel (collapsible)
        if (shouldShowInsights) {
            AdaptiveInsightsPanel(
                adaptiveConfig = adaptiveConfig,
                personalizedCoaching = personalizedCoaching,
                onCoachingRequest = { message ->
                    val userId = UserPreferences.getSafeUserId()
                    viewModel.getPersonalizedCoaching(message, userId)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        AdaptiveActionButtons(
            onSkipChallenge = {
                val userId = UserPreferences.getSafeUserId()
                viewModel.skipToNextAdaptiveChallenge(userId)
            },
            onRequestCoaching = {
                val userId = UserPreferences.getSafeUserId()
                viewModel.getPersonalizedCoaching(
                    "I need help with this challenge",
                    userId
                )
            }
        )
    }
}

@Composable
private fun AdaptiveHeaderSection(
    adaptiveConfig: AdaptiveChallengeConfig?,
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Adaptive Challenge",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            adaptiveConfig?.let { config ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DifficultyChip(difficulty = config.recommendedDifficulty)
                    XPMultiplierChip(multiplier = config.xpMultiplier)
                }
            }
        }

        // Adaptive insights indicator
        if (adaptiveConfig?.shouldShowPerformanceInsights == true) {
            Badge(
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("AI")
            }
        }
    }
}

@Composable
private fun DifficultyChip(difficulty: DifficultyLevel) {
    val color = when (difficulty) {
        DifficultyLevel.BEGINNER -> Color(0xFF4CAF50)
        DifficultyLevel.EASY -> Color(0xFF8BC34A)
        DifficultyLevel.MEDIUM -> Color(0xFFFF9800)
        DifficultyLevel.HARD -> Color(0xFFFF5722)
        DifficultyLevel.EXPERT -> Color(0xFFF44336)
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = difficulty.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun XPMultiplierChip(multiplier: Double) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "${multiplier}x XP",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LoadingSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Preparing your adaptive challenge...",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AdaptiveChallengeCard(
    challenge: ChallengeTask,
    adaptiveConfig: AdaptiveChallengeConfig?,
    currentAttempts: Int,
    hintsUsed: Int,
    totalXP: Int,
    onAttemptSubmitted: (String, Boolean) -> Unit,
    onHintRequested: () -> Unit
) {
    var userAnswer by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Challenge header with adaptive info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = challenge.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$totalXP XP",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (adaptiveConfig?.xpMultiplier != 1.0) {
                        Text(
                            text = "Adaptive Bonus!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProgressChip(
                    label = "Attempts",
                    value = currentAttempts.toString(),
                    icon = Icons.Default.Refresh
                )
                ProgressChip(
                    label = "Hints",
                    value = hintsUsed.toString(),
                    icon = Icons.Default.Lightbulb
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Challenge content
            Text(
                text = challenge.content,
                style = MaterialTheme.typography.bodyMedium
            )

            // Focus areas indicator
            adaptiveConfig?.focusAreas?.takeIf { it.isNotEmpty() }?.let { areas ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Focus: ${areas.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Answer input
            OutlinedTextField(
                value = userAnswer,
                onValueChange = { userAnswer = it },
                label = { Text("Your Answer") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        // Simple validation - in real app, implement proper answer checking
                        val isCorrect = userAnswer.isNotBlank() && userAnswer.length > 10
                        onAttemptSubmitted(userAnswer, isCorrect)
                        if (isCorrect) userAnswer = ""
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Submit")
                }

                OutlinedButton(
                    onClick = onHintRequested
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hint")
                }
            }
        }
    }
}

@Composable
private fun ProgressChip(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$label: $value",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun RealTimeFeedbackSection(
    response: RealTimeAdaptiveResponse,
    adaptiveHints: List<String>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Psychology, contentDescription = null)
                Text(
                    text = "AI Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Adaptive hints
            if (adaptiveHints.isNotEmpty()) {
                adaptiveHints.forEach { hint ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Bonus XP notification
            if (response.bonusXP > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Bonus XP: +${response.bonusXP}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MotivationalMessageCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AdaptiveInsightsPanel(
    adaptiveConfig: AdaptiveChallengeConfig?,
    personalizedCoaching: PersonalizedCoachingResponse?,
    onCoachingRequest: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Performance Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(16.dp))

                // Weak areas
                adaptiveConfig?.weakAreas?.takeIf { it.isNotEmpty() }?.let { weakAreas ->
                    Text(
                        text = "Areas for Improvement:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    weakAreas.take(3).forEach { area ->
                        WeakAreaChip(weakArea = area)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Personalized coaching
                personalizedCoaching?.let { coaching ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AI Coach Says:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = coaching.response,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    coaching.recommendations.forEach { recommendation ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• $recommendation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeakAreaChip(weakArea: WeakArea) {
    val severity = when {
        weakArea.severityScore > 0.7 -> "High"
        weakArea.severityScore > 0.4 -> "Medium"
        else -> "Low"
    }

    val color = when {
        weakArea.severityScore > 0.7 -> Color(0xFFFF5722)
        weakArea.severityScore > 0.4 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "${weakArea.skillArea} - $severity Priority",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AdaptiveActionButtons(
    onSkipChallenge: () -> Unit,
    onRequestCoaching: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onSkipChallenge,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Next Challenge")
        }

        Button(
            onClick = onRequestCoaching,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Psychology, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Ask AI Coach")
        }
    }
}
