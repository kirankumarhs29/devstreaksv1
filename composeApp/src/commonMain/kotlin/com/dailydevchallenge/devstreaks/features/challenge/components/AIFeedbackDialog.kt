package com.dailydevchallenge.devstreaks.features.challenge.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.dailydevchallenge.devstreaks.llm.ChallengeFeedback
import com.dailydevchallenge.devstreaks.model.Challenge

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AIFeedbackDialog(
    feedback: ChallengeFeedback?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    challenge: Challenge? = null
) {
    if (feedback != null || isLoading) {
        Dialog(onDismissRequest = onDismiss) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Psychology,
                                contentDescription = "AI Feedback",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI Feedback",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    if (isLoading) {
                        // Loading state
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Analyzing your solution...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (feedback != null) {
                        // Feedback content
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.heightIn(max = 400.dp)
                        ) {
                            // Score section
                            item {
                                ScoreSection(feedback.score)
                            }

                            // Encouragement
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            feedback.encouragement,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // Strengths
                            if (feedback.strengths.isNotEmpty()) {
                                item {
                                    FeedbackSection(
                                        title = "What you did well",
                                        icon = Icons.Default.ThumbUp,
                                        items = feedback.strengths,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }

                            // Improvements
                            if (feedback.improvements.isNotEmpty()) {
                                item {
                                    FeedbackSection(
                                        title = "Areas to improve",
                                        icon = Icons.Default.TrendingUp,
                                        items = feedback.improvements,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Tips
                            if (feedback.personalizedTips.isNotEmpty()) {
                                item {
                                    FeedbackSection(
                                        title = "Personalized tips",
                                        icon = Icons.Default.Lightbulb,
                                        items = feedback.personalizedTips,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }

                            // Next steps
                            if (feedback.nextSteps.isNotEmpty()) {
                                item {
                                    FeedbackSection(
                                        title = "Next steps",
                                        icon = Icons.Default.NavigateNext,
                                        items = feedback.nextSteps,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        // Action button
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue Learning")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreSection(score: Int) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                score >= 90 -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                score >= 70 -> Color(0xFFFF9800).copy(alpha = 0.1f)
                else -> Color(0xFFF44336).copy(alpha = 0.1f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Your Score",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$score/100",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        score >= 90 -> Color(0xFF4CAF50)
                        score >= 70 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
            }
            Icon(
                when {
                    score >= 90 -> Icons.Default.Star
                    score >= 70 -> Icons.Default.TrendingUp
                    else -> Icons.Default.School
                },
                contentDescription = null,
                tint = when {
                    score >= 90 -> Color(0xFF4CAF50)
                    score >= 70 -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                },
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun FeedbackSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<String>,
    color: Color
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        items.forEach { item ->
            Row(
                modifier = Modifier.padding(start = 28.dp, bottom = 4.dp)
            ) {
                Text(
                    "•",
                    color = color,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
