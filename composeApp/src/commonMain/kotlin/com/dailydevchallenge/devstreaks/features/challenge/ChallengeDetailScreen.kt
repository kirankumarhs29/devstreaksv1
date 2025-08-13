package com.dailydevchallenge.devstreaks.features.challenge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.challenge.components.ActivityPager
import com.dailydevchallenge.devstreaks.features.challenge.components.CompletionCard
import com.dailydevchallenge.devstreaks.features.navigation.DevStreakTopBar
import com.dailydevchallenge.devstreaks.features.routes.Routes
import com.dailydevchallenge.devstreaks.model.ChallengeActivity
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.utils.getLogger
import org.koin.compose.koinInject


// 1. DATA MODEL (Extend if needed)
data class ActivityPagerItem(
    val id: String,
    val type: String, // "quiz", "code", "flashcard", "project", "why", "tip", "bonus", "aiBreakdown", etc.
    val content: String? = null,
    val challenge: ChallengeActivity? = null
    // ...other fields if needed (quiz options, correctAns, etc.)
)
@Composable
fun ChallengeDetailScreen(
    navController: NavController,
    day: ChallengeTask,
    isCompleted: Boolean = false,
    onMarkAsDone: () -> Unit
) {
    val viewModel: ChallengeDetailViewModel = remember { ChallengeDetailViewModel(day, isCompleted) }
    val uiState by viewModel.uiState.collectAsState()
    val logger = remember { getLogger() }
    val repository: ChallengeRepository = koinInject()

    Scaffold(
        topBar = {
            DevStreakTopBar(
                title = "Day ${day.day}",
                onBack = {
                    navController.popBackStack(Routes.HomeScreen, inclusive = false)
                }
            )
        }
    ) { innerPadding ->
        logger.d("ChallengeDetailScreen composed for day ${day.day}, isCompleted=${uiState.isCompleted}, started=${uiState.started}, allDone=${uiState.allDone}")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompactHero(day)
            if (!uiState.started) {
                StartTaskCard {
                    viewModel.startTask()
                }
            } else {

                ActivityPager(
                    items = uiState.items,
                    isChallengeCompleted = uiState.isCompleted,
                    onAllCompleted = {
                        viewModel.onAllCompleted()
                        onMarkAsDone()
                    },
                    challengeRepository = repository,
                )
                if (uiState.isCompleted && uiState.showConfetti) {
                    CompletionCard(
                        onDismiss = { viewModel.dismissConfetti() },
                        message = "Streak Achieved! 🎉 +${day.xp} XP",
                    )
                }
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
            Text(" ${day.title}", style = MaterialTheme.typography.titleMedium)
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
fun ChallengeMiniIntro(title: String, insight: String, goal: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(4.dp)) {
            Text("📌 $title", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Text("💡 $insight", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text("🎯 Goal: $goal", style = MaterialTheme.typography.labelSmall)
        }
    }
}

