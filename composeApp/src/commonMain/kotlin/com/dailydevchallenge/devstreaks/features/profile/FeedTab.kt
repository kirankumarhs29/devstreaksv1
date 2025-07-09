package com.dailydevchallenge.devstreaks.features.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.dailydevchallenge.devstreaks.model.FeedItem
import com.dailydevchallenge.devstreaks.settings.DarkModeSettings
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

    LaunchedEffect(Unit) {
        rawFeed.forEachIndexed { index, item ->
            delay(index * 200L) // stagger animation
            animatedFeed.add(item)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(animatedFeed) { item ->
            AnimatedFeedItemCard(item)
        }
    }
}

@Composable
fun AnimatedFeedItemCard(item: FeedItem) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(tween(400)) { it / 2 },
        exit = fadeOut()
    ) {
        FeedItemCard(item)
    }
}

@Composable
fun FeedItemCard(item: FeedItem) {
    val isDark by DarkModeSettings.darkModeFlow.collectAsState()
    val bgColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF9F9FA)
    val accentColor = MaterialTheme.colorScheme.primary

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = item.badge,
                    color = accentColor,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = item.date,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
