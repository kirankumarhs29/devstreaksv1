package com.dailydevchallenge.devstreaks.features.social.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyGroupCard(
    studyGroup: StudyGroup,
    onJoinClick: () -> Unit = {},
    onViewClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onViewClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with group name and member count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = studyGroup.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = studyGroup.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                MemberCountBadge(
                    currentMembers = studyGroup.memberIds.size,
                    maxMembers = studyGroup.maxMembers
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Focus skills chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(studyGroup.focusSkills) { skill ->
                    SkillChip(skill = skill)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meeting schedule and target level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                studyGroup.meetingSchedule?.let { schedule ->
                    MeetingInfo(schedule = schedule)
                } ?: run {
                    Text(
                        text = "No scheduled meetings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TargetLevelChip(level = studyGroup.targetLevel)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onJoinClick,
                    modifier = Modifier.weight(1f),
                    enabled = studyGroup.memberIds.size < studyGroup.maxMembers
                ) {
                    Icon(Icons.Default.Group, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Join Group")
                }

                OutlinedButton(
                    onClick = onViewClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Details")
                }
            }
        }
    }
}

@Composable
fun LeaderboardEntryCard(
    entry: LeaderboardEntry,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            RankBadge(rank = entry.rank)

            Spacer(modifier = Modifier.width(16.dp))

            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Level ${entry.level}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Score/XP
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${entry.points}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CommunityCharlengeCard(
    challenge: CommunityChallenge,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with trending topic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Trending: ${challenge.trendingTopic}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Badge(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text("${challenge.participantCount} joined")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Challenge metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${challenge.estimatedTimeMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${challenge.xpReward} XP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                SocialDifficultyChip(difficulty = challenge.difficulty)
            }

            if (challenge.skillAreas.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(challenge.skillAreas.take(3)) { skill ->
                        SkillChip(skill = skill)
                    }
                }
            }
        }
    }
}

// Helper composables
@Composable
private fun MemberCountBadge(
    currentMembers: Int,
    maxMembers: Int,
    modifier: Modifier = Modifier
) {
    Badge(
        modifier = modifier,
        containerColor = if (currentMembers < maxMembers)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.errorContainer
    ) {
        Text("$currentMembers/$maxMembers")
    }
}

@Composable
private fun SkillChip(
    skill: String,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = { },
        label = {
            Text(
                text = skill,
                style = MaterialTheme.typography.labelMedium
            )
        },
        modifier = modifier.height(28.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    )
}

@Composable
private fun MeetingInfo(
    schedule: MeetingSchedule,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Event,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${schedule.frequency.name.lowercase().replaceFirstChar { it.uppercase() }} at ${schedule.timeUtc}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TargetLevelChip(
    level: DifficultyLevel,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, text) = when (level) {
        DifficultyLevel.EASY -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            "Beginner"
        )
        DifficultyLevel.MEDIUM -> Triple(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary,
            "Intermediate"
        )
        DifficultyLevel.HARD -> Triple(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary,
            "Advanced"
        )
        else -> Triple(
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.onSurface,
            "Unknown"
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

@Composable
private fun RankBadge(
    rank: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .background(
                color = when (rank) {
                    1 -> MaterialTheme.colorScheme.primary
                    2 -> MaterialTheme.colorScheme.secondary
                    3 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.outline
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

// Renamed to avoid conflict with CollaborativeChallengeCard's DifficultyChip
@Composable
private fun SocialDifficultyChip(
    difficulty: DifficultyLevel,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor, text) = when (difficulty) {
        DifficultyLevel.EASY -> Triple(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            "Easy"
        )
        DifficultyLevel.MEDIUM -> Triple(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary,
            "Medium"
        )
        DifficultyLevel.HARD -> Triple(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary,
            "Hard"
        )
        else -> Triple(
            MaterialTheme.colorScheme.outline,
            MaterialTheme.colorScheme.onSurface,
            "Unknown"
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
