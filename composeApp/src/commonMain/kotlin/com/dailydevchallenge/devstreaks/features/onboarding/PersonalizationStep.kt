package com.dailydevchallenge.devstreaks.features.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.LearningIntent

@Composable
fun PersonalizationStep(
    learningIntent: LearningIntent?,
    onPersonalize: () -> Unit
) {
    var showAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showAnimation = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = showAnimation,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(animationSpec = tween(800))
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "✨",
                        style = MaterialTheme.typography.displayLarge
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Perfect! Let's create your personalized path",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Our AI will craft a learning journey tailored specifically to your goals and preferences.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Learning Intent Summary
        learningIntent?.let { intent ->
            var summaryVisible by remember { mutableStateOf(false) }

            LaunchedEffect(showAnimation) {
                kotlinx.coroutines.delay(400)
                summaryVisible = true
            }

            AnimatedVisibility(
                visible = summaryVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(600, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(600))
            ) {
                PersonalizationSummaryCard(intent)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Features that will be personalized
        var featuresVisible by remember { mutableStateOf(false) }

        LaunchedEffect(showAnimation) {
            kotlinx.coroutines.delay(800)
            featuresVisible = true
        }

        AnimatedVisibility(
            visible = featuresVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(600))
        ) {
            PersonalizationFeaturesCard()
        }
    }
}

@Composable
private fun PersonalizationSummaryCard(intent: LearningIntent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your Learning Profile",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (intent.primaryGoal.isNotEmpty()) {
                SummaryItem(
                    label = "Goal",
                    value = intent.primaryGoal,
                    icon = "🎯"
                )
            }

            if (intent.skillFocus.isNotEmpty()) {
                SummaryItem(
                    label = "Skills",
                    value = intent.skillFocus.take(3).joinToString(", ") +
                            if (intent.skillFocus.size > 3) " +${intent.skillFocus.size - 3} more" else "",
                    icon = "💻"
                )
            }

            if (intent.careerTrack.isNotEmpty()) {
                SummaryItem(
                    label = "Career Track",
                    value = intent.careerTrack,
                    icon = "🚀"
                )
            }

            if (intent.experience.isNotEmpty()) {
                SummaryItem(
                    label = "Experience",
                    value = intent.experience,
                    icon = "📈"
                )
            }

            SummaryItem(
                label = "Daily Time",
                value = "${intent.timePerDay} minutes",
                icon = "⏰"
            )

            if (intent.learningStyle.isNotEmpty()) {
                SummaryItem(
                    label = "Learning Style",
                    value = intent.learningStyle,
                    icon = "🎓"
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    icon: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PersonalizationFeaturesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "What we'll personalize for you:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            val features = listOf(
                "🎯 Challenge difficulty and topics",
                "🤖 AI coach personality and guidance style",
                "📚 Learning resources and recommendations",
                "🏆 Achievement goals and milestones",
                "📊 Progress tracking and insights"
            )

            features.forEach { feature ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✓",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = feature.substring(2), // Remove emoji since we use ✓
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
