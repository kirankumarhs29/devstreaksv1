package com.dailydevchallenge.devstreaks.features.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.dailydevchallenge.devstreaks.model.FeedItem
import com.dailydevchallenge.devstreaks.settings.DarkModeSettings
import com.dailydevchallenge.devstreaks.theme.extendedColors
import kotlinx.coroutines.delay

@Composable
fun FeedTab() {
    val rawFeed = listOf(
        FeedItem("Completed 3 challenges", "+150 XP", "🔥 Streak Continued!", "2025-06-27"),
        FeedItem("Joined a new track: DSA Java", "+50 XP", "🎯 Dev Goal Set", "2025-06-26"),
        FeedItem("7-day streak achieved!", "+300 XP", "🏆 Weekly Win!", "2025-06-25"),
        FeedItem("Logged in after 1 day", "+20 XP", "🕒 Comeback", "2025-06-24"),
        FeedItem("Completed 5 challenges", "+250 XP", "🎉 Milestone Reached!", "2025-06-23"),
        FeedItem("Joined a new track: Flutter", "+50 XP", "🚀 New Journey", "2025-06-22"),
        FeedItem("Completed 2 challenges", "+100 XP", "🌟 Progress Made!", "2025-06-21")
    )

    val animatedFeed = remember { mutableStateListOf<FeedItem>() }
    val isDark by DarkModeSettings.darkModeFlow.collectAsState()

    LaunchedEffect(Unit) {
        rawFeed.forEachIndexed { index, item ->
            delay(index * 150L) // Faster stagger animation
            animatedFeed.add(item)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with stats
        item {
            FeedHeaderCard()
        }

        // Filter chips
        item {
            FilterChipsRow()
        }

        // Activity feed items
        items(animatedFeed) { item ->
            EnhancedFeedItemCard(item)
        }

        // Load more placeholder
        item {
            LoadMoreCard()
        }
    }
}

@Composable
private fun FeedHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📈 Your Activity",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Track your learning journey and celebrate achievements",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun FilterChipsRow() {
    val filters = listOf("All", "Challenges", "Streaks", "Achievements", "Learning")
    var selectedFilter by remember { mutableStateOf("All") }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { selectedFilter = filter },
                label = { Text(filter) },
                leadingIcon = if (selectedFilter == filter) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun EnhancedFeedItemCard(item: FeedItem) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(400))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Activity icon based on type
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = getActivityColor(item.title).copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = getActivityIcon(item.title),
                            contentDescription = null,
                            tint = getActivityColor(item.title),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = item.badge,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = item.badge,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatDate(item.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Achievement badge or arrow
                if (item.badge.contains("🏆") || item.badge.contains("🔥")) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.extendedColors.achievement.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Achievement",
                            tint = MaterialTheme.extendedColors.achievement,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadMoreCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = "Load more",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Load more activities...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getActivityIcon(title: String): ImageVector {
    return when {
        title.contains("challenge", ignoreCase = true) -> Icons.Default.Code
        title.contains("streak", ignoreCase = true) -> Icons.Default.Whatshot
        title.contains("track", ignoreCase = true) -> Icons.Default.School
        title.contains("login", ignoreCase = true) -> Icons.Default.Login
        else -> Icons.Default.CheckCircle
    }
}

private fun getActivityColor(title: String): Color {
    return when {
        title.contains("challenge", ignoreCase = true) -> Color(0xFF4CAF50)
        title.contains("streak", ignoreCase = true) -> Color(0xFFFF6B35)
        title.contains("track", ignoreCase = true) -> Color(0xFF2196F3)
        title.contains("login", ignoreCase = true) -> Color(0xFF9C27B0)
        else -> Color(0xFF607D8B)
    }
}

private fun formatDate(date: String): String {
    // Simple date formatting - you might want to use a proper date library
    return when (date) {
        "2025-06-27" -> "Today"
        "2025-06-26" -> "Yesterday"
        else -> date
    }
}
