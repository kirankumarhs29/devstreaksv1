package com.dailydevchallenge.devstreaks.features

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.home.HomeViewModel
import org.koin.compose.koinInject
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.dailydevchallenge.devstreaks.features.routes.Routes

@Composable
fun ProgressScreen(
    navController: NavController,
    viewModel: HomeViewModel = koinInject()
) {
    val stats by viewModel.userStats.collectAsState()
    val level = stats.xp / 100
    val nextXP = 100 - (stats.xp % 100)
    val progress = (stats.xp % 100).toFloat() / 100f
    val animatedProgress by animateFloatAsState(targetValue = progress)
    val eta by viewModel.estimatedEndDate.collectAsState()
    val completedCount by viewModel.completedTaskIds.collectAsState()
    val track by viewModel.currentTrack.collectAsState()
    val scrollState = rememberScrollState()

    val quote = remember {
        listOf(
            "Every expert was once a beginner.",
            "Consistency beats intensity.",
            "You're not behind — you're building.",
            "Progress, not perfection."
        ).random()
    }
    val pastWeekData = listOf(10, 30, 50, 70, 60, 80, 90) // dummy sparkline data

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📊 Your Dev Journey", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text("“$quote”", style = MaterialTheme.typography.bodySmall)
        }

        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                strokeWidth = 10.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                modifier = Modifier.fillMaxSize()
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Level $level", style = MaterialTheme.typography.titleMedium)
                Text("${stats.xp} XP", style = MaterialTheme.typography.labelMedium)
                Text("+$nextXP XP to next", style = MaterialTheme.typography.labelSmall)
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🔥 ${stats.streak}-Day Streak", fontSize = 20.sp)
                Text(
                    "You're building a real habit. Keep it going!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatCard("⭐ Total XP", "${stats.xp}")
            StatCard("📅 Days Active", "${stats.streak}")
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatCard("✅ Challenges", "${completedCount.size}")
            StatCard("🛤️ Track", track)
        }

        StatCard("⏳ ETA", eta.toString())

        // DevCoach insight
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("🧠 DevCoach Insight", style = MaterialTheme.typography.labelMedium)
                Text("You're improving in quizzes! Keep the momentum.", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Sparkline
        Text("📈 Last 7 Days Activity", style = MaterialTheme.typography.labelMedium)
        val sparklineColor = MaterialTheme.colorScheme.primary // ✅ Safe, outside Canvas
        Canvas(modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))) {
            val maxVal = pastWeekData.maxOrNull()?.toFloat() ?: 1f
            val spacing = size.width / (pastWeekData.size - 1)
            for (i in 1 until pastWeekData.size) {
                val x1 = spacing * (i - 1)
                val y1 = size.height - (pastWeekData[i - 1] / maxVal) * size.height
                val x2 = spacing * i
                val y2 = size.height - (pastWeekData[i] / maxVal) * size.height
                drawLine(
                    color = sparklineColor,
                    start = androidx.compose.ui.geometry.Offset(x1, y1),
                    end = androidx.compose.ui.geometry.Offset(x2, y2),
                    strokeWidth = 4f
                )
            }
        }

        Button(
            onClick = { navController.navigate(Routes.HomeScreen) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🚀 Go to Today’s Challenge")
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(80.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}
