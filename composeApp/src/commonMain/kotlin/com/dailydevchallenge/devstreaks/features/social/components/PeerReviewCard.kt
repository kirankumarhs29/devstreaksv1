package com.dailydevchallenge.devstreaks.features.social.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.service.SocialEngine
import kotlinx.coroutines.launch

/**
 * Peer review card for code submissions with LLM-generated structured feedback
 * Allows users to submit reviews and interact with existing feedback
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeerReviewCard(
    review: CodeReview,
    socialEngine: SocialEngine? = null,
    onReviewClick: () -> Unit = {},
    onHelpfulClick: (() -> Unit)? = null,
    onReplyClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }
    var helpfulCount by remember { mutableStateOf(review.helpfulCount) }
    var isHelpfulPressed by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onReviewClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (review.generatedByLLM)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Review header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = review.reviewerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        if (review.generatedByLLM) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text("AI", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Text(
                        text = "Review for: ${review.submissionTitle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    OverallRatingChip(rating = review.overallRating)
                    Text(
                        text = formatTimeAgo(review.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Code snippet preview (if available)
            review.codeSnippet?.let { snippet ->
                CodeSnippetPreview(
                    snippet = snippet,
                    language = review.language ?: "kotlin",
                    isExpanded = isExpanded,
                    onExpandClick = { isExpanded = !isExpanded }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Feedback summary
            Text(
                text = review.feedback,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            // LLM-generated structured feedback
            if (review.generatedByLLM && review.structuredFeedback != null) {
                Spacer(modifier = Modifier.height(12.dp))
                StructuredFeedbackSection(
                    feedback = review.structuredFeedback,
                    isExpanded = isExpanded
                )
            }

            // Areas for improvement tags
            if (review.areasForImprovement.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(review.areasForImprovement.take(if (isExpanded) Int.MAX_VALUE else 3)) { area ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(area, style = MaterialTheme.typography.labelMedium) },
                            modifier = Modifier.height(28.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Helpful button
                    FilterChip(
                        selected = isHelpfulPressed,
                        onClick = {
                            scope.launch {
                                socialEngine?.let { engine ->
                                    isHelpfulPressed = !isHelpfulPressed
                                    helpfulCount += if (isHelpfulPressed) 1 else -1
                                    engine.markReviewHelpful(review.id, isHelpfulPressed)
                                }
                                onHelpfulClick?.invoke()
                            }
                        },
                        label = { Text("Helpful ($helpfulCount)") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isHelpfulPressed) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.height(32.dp)
                    )

                    // Reply button
                    if (onReplyClick != null) {
                        TextButton(
                            onClick = onReplyClick,
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Reply,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reply")
                        }
                    }
                }

                // Expand/Collapse button
                TextButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(if (isExpanded) "Show Less" else "Show More")
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeSnippetPreview(
    snippet: String,
    language: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = onExpandClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                        contentDescription = if (isExpanded) "Collapse code" else "Expand code",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = snippet,
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            )
        }
    }
}

@Composable
private fun StructuredFeedbackSection(
    feedback: StructuredFeedback,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI Analysis",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Strengths
            if (feedback.strengths.isNotEmpty()) {
                FeedbackSection(
                    title = "Strengths",
                    items = feedback.strengths,
                    icon = Icons.Default.CheckCircle,
                    color = MaterialTheme.colorScheme.primary,
                    isExpanded = isExpanded
                )
            }

            // Improvements
            if (feedback.improvements.isNotEmpty()) {
                FeedbackSection(
                    title = "Improvements",
                    items = feedback.improvements,
                    icon = Icons.Default.Error,
                    color = MaterialTheme.colorScheme.error,
                    isExpanded = isExpanded
                )
            }

            // Best practices
            if (feedback.bestPractices.isNotEmpty()) {
                FeedbackSection(
                    title = "Best Practices",
                    items = feedback.bestPractices,
                    icon = Icons.Default.Star,
                    color = MaterialTheme.colorScheme.tertiary,
                    isExpanded = isExpanded
                )
            }
        }
    }
}

@Composable
private fun FeedbackSection(
    title: String,
    items: List<String>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }

        val displayItems = if (isExpanded) items else items.take(2)
        displayItems.forEach { item ->
            Text(
                text = "• $item",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 18.dp, top = 2.dp)
            )
        }

        if (!isExpanded && items.size > 2) {
            Text(
                text = "... and ${items.size - 2} more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 18.dp, top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun OverallRatingChip(
    rating: ReviewRating,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, text) = when (rating) {
        ReviewRating.EXCELLENT -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            "Excellent"
        )
        ReviewRating.GOOD -> Triple(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary,
            "Good"
        )
        ReviewRating.NEEDS_WORK -> Triple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
            "Needs Work"
        )
        ReviewRating.FAIR -> Triple(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary,
            "Fair"
        )
    }

    Badge(
        modifier = modifier,
        containerColor = backgroundColor,
        contentColor = textColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatTimeAgo(timestamp: kotlinx.datetime.Instant): String {
    val now = kotlinx.datetime.Clock.System.now()
    val duration = now - timestamp

    return when {
        duration.inWholeDays > 0 -> "${duration.inWholeDays}d ago"
        duration.inWholeHours > 0 -> "${duration.inWholeHours}h ago"
        duration.inWholeMinutes > 0 -> "${duration.inWholeMinutes}m ago"
        else -> "Just now"
    }
}
