package com.dailydevchallenge.devstreaks.features.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject

@Composable
fun LeaderboardScreen() {
    val viewModel: LeaderboardViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Leaderboard Type Selector
            LeaderboardTypeSelector(
                selectedType = uiState.selectedType,
                onTypeSelected = { viewModel.selectLeaderboardType(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User's current rank card
            uiState.userEntry?.let { userEntry ->
                UserRankCard(
                    userEntry = userEntry,
                    leaderboardType = uiState.selectedType
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Leaderboard content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    ErrorCard(
                        error = uiState.error!!,
                        onRetry = { viewModel.refreshLeaderboards() }
                    )
                }
                else -> {
                    LeaderboardList(
                        entries = viewModel.getCurrentLeaderboard(),
                        leaderboardType = uiState.selectedType,
                        currentUserId = uiState.userEntry?.userId
                    )
                }
            }
        }
}

@Composable
private fun LeaderboardTypeSelector(
    selectedType: LeaderboardType,
    onTypeSelected: (LeaderboardType) -> Unit
) {
    val types = listOf(
        LeaderboardType.GLOBAL_XP to "🌍 Global",
        LeaderboardType.WEEKLY_XP to "📅 Weekly",
        LeaderboardType.MONTHLY_XP to "🗓️ Monthly",
        LeaderboardType.STREAK to "🔥 Streak",
        LeaderboardType.FRIENDS to "👥 Friends"
    )

    ScrollableTabRow(
        selectedTabIndex = types.indexOfFirst { it.first == selectedType },
        modifier = Modifier.fillMaxWidth(),
        edgePadding = 0.dp
    ) {
        types.forEachIndexed { index, (type, label) ->
            Tab(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                text = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

@Composable
private fun UserRankCard(
    userEntry: LeaderboardEntry,
    leaderboardType: LeaderboardType
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${userEntry.rank}",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your Rank",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = getDisplayValue(userEntry, leaderboardType),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Icon(
                Icons.Default.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LeaderboardList(
    entries: List<LeaderboardEntry>,
    leaderboardType: LeaderboardType,
    currentUserId: String?
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(entries) { entry ->
            LeaderboardEntryCard(
                entry = entry,
                leaderboardType = leaderboardType,
                isCurrentUser = entry.userId == currentUserId
            )
        }
    }
}

@Composable
private fun LeaderboardEntryCard(
    entry: LeaderboardEntry,
    leaderboardType: LeaderboardType,
    isCurrentUser: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isCurrentUser) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank with trophy for top 3
            RankBadge(rank = entry.rank)

            Spacer(modifier = Modifier.width(12.dp))

            // Avatar placeholder
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.username.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.username,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentUser) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = "Level ${entry.level}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Score/value
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = getDisplayValue(entry, leaderboardType),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = getDisplayLabel(leaderboardType),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RankBadge(rank: Int) {
    val (icon, color) = when (rank) {
        1 -> Icons.Default.EmojiEvents to Color(0xFFFFD700) // Gold
        2 -> Icons.Default.EmojiEvents to Color(0xFFC0C0C0) // Silver
        3 -> Icons.Default.EmojiEvents to Color(0xFFCD7F32) // Bronze
        else -> null to MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = "Trophy",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

private fun getDisplayValue(entry: LeaderboardEntry, type: LeaderboardType): String {
    return when (type) {
        LeaderboardType.GLOBAL_XP -> "${entry.xp} XP"
        LeaderboardType.WEEKLY_XP -> "${entry.weeklyXp} XP"
        LeaderboardType.MONTHLY_XP -> "${entry.monthlyXp} XP"
        LeaderboardType.STREAK -> "${entry.dailyStreak} days"
        LeaderboardType.FRIENDS -> "${entry.xp} XP"
    }
}

private fun getDisplayLabel(type: LeaderboardType): String {
    return when (type) {
        LeaderboardType.GLOBAL_XP -> "Total XP"
        LeaderboardType.WEEKLY_XP -> "This Week"
        LeaderboardType.MONTHLY_XP -> "This Month"
        LeaderboardType.STREAK -> "Streak"
        LeaderboardType.FRIENDS -> "Total XP"
    }
}
