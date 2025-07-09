package com.dailydevchallenge.devstreaks.features.feed

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.UserStats
import com.dailydevchallenge.devstreaks.features.home.HomeViewModel
import com.dailydevchallenge.devstreaks.settings.DarkModeSettings
import org.koin.compose.koinInject

@Composable
fun LeaderboardScreen(viewModel: HomeViewModel = koinInject()) {
    val currentUser by viewModel.userStats.collectAsState()
    val allUsers = listOf(
        UserStats("Anya", 900, 15),
        UserStats("Ravi", 850, 10),
        UserStats("Lina", 780, 14),
        UserStats("Kiran", 740, 12),
        currentUser
    )

    val sortedUsers = allUsers.distinctBy { it.name }.sortedByDescending { it.xp }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "🔥 Leaderboard",
            style = MaterialTheme.typography.headlineSmall.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
        )

        sortedUsers.forEachIndexed { index, user ->
            NetflixLeaderboardCard(
                user = user,
                rank = index + 1,
                isCurrentUser = user.name == currentUser.name
            )
        }
    }
}

@Composable
fun NetflixLeaderboardCard(
    user: UserStats,
    rank: Int,
    isCurrentUser: Boolean
) {
    val progress = (user.xp % 100).toFloat() / 100f
    val isDark by DarkModeSettings.darkModeFlow.collectAsState()

    val cardBg = MaterialTheme.colorScheme.surfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val glow = if (isCurrentUser) accent.copy(alpha = 0.3f) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, glow, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Fancy rank chip
                Surface(
                    color = accent,
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 4.dp,
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Text(
                        text = "#$rank",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = accent,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    )
                }

                Text(
                    text = "${user.xp} XP",
                    color = accent,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (isCurrentUser) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AchievementBadge("🔥 You’re on fire")
                    if (user.streak >= 7) AchievementBadge("💯 Weekly Streak")
                    if (user.xp >= 1000) AchievementBadge("🧠 1K Club")
                }
            }
        }
    }
}


@Composable
fun AchievementBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f),
        modifier = Modifier
            .padding(top = 4.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}



