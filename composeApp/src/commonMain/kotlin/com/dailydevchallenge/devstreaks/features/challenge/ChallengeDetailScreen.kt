package com.dailydevchallenge.devstreaks.features.challenge

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.navigation.DevStreakTopBar
import com.dailydevchallenge.devstreaks.features.routes.Routes
import com.dailydevchallenge.devstreaks.features.challenge.components.ActivitySection
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.model.effectiveChallenges
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.launch
import io.github.raamcosta.lottie.compose.LottieCompositionSpec
import io.github.raamcosta.lottie.compose.rememberLottieComposition
import io.github.raamcosta.lottie.compose.LottieAnimation
import io.github.raamcosta.lottie.compose.LottieConstants
import androidx.compose.ui.res.useResource

@Composable
fun ChallengeDetailScreen(
    navController: NavController,
    day: ChallengeTask,
    isCompleted: Boolean = false,
    onMarkAsDone: () -> Unit
) {
    val viewedIds = remember { mutableStateListOf<String>() }
    val allDone = viewedIds.size >= day.challenges.size
    var started by remember { mutableStateOf(false) }
    var showInsights by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // logger
    val logger = remember { getLogger() }

    Scaffold(
        topBar = {
            DevStreakTopBar(
                title = "Day ${day.day}",
                onBack = { // navigate to home
                    logger.d("Back pressed on ChallengeDetailScreen for day ${day.day}")
                    scope.launch {
                        navController.popBackStack(Routes.HomeScreen, inclusive = false)
                    }
                }
            )
        }
    ) { innerPadding ->
        logger.d("ChallengeDetailScreen composed for day ${day.day}, isCompleted=$isCompleted, started=$started, allDone=$allDone")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompactHero(day)

            if (!started) {
                logger.d("StartTaskCard shown for day ${day.day}")
                StartTaskCard {
                    logger.d("Task started for day ${day.day}")
                    started = true
                }
            } else {
                val fallbackChallenges = day.effectiveChallenges()
                logger.d("ActivitySection shown for day ${day.day}, viewedIds=${viewedIds.size}")
                ActivitySection(fallbackChallenges) { id ->
                    if (id !in viewedIds) {
                        logger.d("Challenge viewed: $id for day ${day.day}")
                        viewedIds.add(id)
                    }
                }
            }

            if (allDone && !isCompleted) {
                logger.d("All challenges done for day ${day.day}, showing Mark as Done button")
                Button(
                    onClick = {
                        logger.d("Mark as Done clicked for day ${day.day}")
                        onMarkAsDone()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("✅ Mark as Done")
                }
            }

            if (isCompleted) {
                logger.d("CompletionCard shown for day ${day.day}")
                CompletionCard()
            }

            TextButton(onClick = {
                logger.d("Show/Hide Insights toggled for day ${day.day}, now: ${!showInsights}")
                showInsights = !showInsights
            }) {
                Text(if (showInsights) "Hide Insights" else "Show Insights")
            }

            if (showInsights) {
                logger.d("OverviewInsights shown for day ${day.day}")
                OverviewInsights(day)
            }
        }
    }
}

@Composable
fun CompactHero(day: ChallengeTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Day ${day.day}: ${day.title}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("⭐ XP: ${day.xp}    🧩 ${day.type}", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun StartTaskCard(onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Ready to start today’s challenge?", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onStart) {
                Text("🚀 Start Task")
            }
        }
    }
}

@Composable
fun OverviewInsights(day: ChallengeTask) {
    var expandedSection by remember { mutableStateOf<String?>(null) }

    @Composable
    fun sectionCard(title: String, content: String, key: String) {
        Card {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable { expandedSection = if (expandedSection == key) null else key }
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium)
                if (expandedSection == key) {
                    Spacer(Modifier.height(4.dp))
                    Text(content, style = MaterialTheme.typography.bodySmall)
                } else {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        content.take(80) + if (content.length > 80) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        sectionCard("📖 Overview", day.content, "overview")
        day.whyItMatters?.let { sectionCard("📌 Why it matters", it, "why") }
        day.tip?.let { sectionCard("💡 Tip", it, "tip") }
        day.bonus?.let { sectionCard("🎁 Bonus", it, "bonus") }
        day.aiBreakdown?.let { sectionCard("🤖 AI Breakdown", it, "ai") }
    }
}

@Composable
fun CompletionCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "🎉 Challenge Completed!",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Great job finishing today’s tasks! You’re leveling up. 🔥",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
        }
        LottieCompositionSpec.RawRes(com.dailydevchallenge.devstreaks.R.raw.success_animation)
    }
}
