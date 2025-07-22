package com.dailydevchallenge.devstreaks.features.challenge

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
import com.dailydevchallenge.devstreaks.features.challenge.components.ActivityPager
import com.dailydevchallenge.devstreaks.features.challenge.components.CompletionCard
import com.dailydevchallenge.devstreaks.features.navigation.DevStreakTopBar
import com.dailydevchallenge.devstreaks.features.routes.Routes
import com.dailydevchallenge.devstreaks.model.ChallengeActivity
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.launch

// 1. DATA MODEL (Extend if needed)
data class ActivityPagerItem(
    val id: String,
    val type: String, // "quiz", "code", "flashcard", "project", "why", "tip", "bonus", "aiBreakdown", etc.
    val content: String? = null,
    val challenge: ChallengeActivity? = null // Optional, for quiz or code challenges
    // ...other fields if needed (quiz options, correctAns, etc.)
)
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
    val scope = rememberCoroutineScope()
    var showConfetti by remember { mutableStateOf(false) }
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
                val items: List<ActivityPagerItem> = buildFullPagerList(day)
                ActivityPager(
                    items = items,
                    isChallengeCompleted = isCompleted,
                    onAllCompleted = {
                        logger.d("All activities completed for day ${day.day}")
                        onMarkAsDone()
                        showConfetti = true
                    }
                )
                if (isCompleted) {
                    logger.d("CompletionCard shown for day ${day.day}")
                    CompletionCard(
                        onDismiss = { showConfetti = false },
                        message = "Streak Achieved! 🎉 +${day.xp} XP",
                    )

                }
            }
            }
        }
    }

fun getInjectedInsights(day: ChallengeTask): List<ActivityPagerItem> {
    val insightCards = mutableListOf<ActivityPagerItem>()
    day.whyItMatters?.let { insightCards += ActivityPagerItem("why", "why", it ) }
    day.tip?.let       { insightCards += ActivityPagerItem("tip", "tip", it) }
    day.bonus?.let     { insightCards += ActivityPagerItem("bonus", "bonus", it) }
    day.aiBreakdown?.let { insightCards += ActivityPagerItem("aiBreakdown", "aiBreakdown", it) }
    return insightCards
}

fun buildFullPagerList(day: ChallengeTask): List<ActivityPagerItem> {
    // Mix insights into the flow (e.g., at the start and between activities)
    val insights = getInjectedInsights(day)
    val activities = day.challenges.map {
        ActivityPagerItem(
            id = it.id,
            type = it.type.toString(),
            content = it.prompt ,// or whatever field matches quiz code
            challenge = it
        )
    }

    // Interleave: why before 1st activity, tip after 1st, bonus after 2nd, ai after 3rd (if present)
    return buildList {
        var i = 0
        if (insights.isNotEmpty()) add(insights[0])
        for (a in activities) {
            add(a)
            i++
            if (i < insights.size) add(insights[i])
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

