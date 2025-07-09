package com.dailydevchallenge.devstreaks.features.challenge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.model.ChallengePathResponse
import com.dailydevchallenge.devstreaks.features.navigation.DevStreakTopBar
import com.dailydevchallenge.devstreaks.features.routes.Routes
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChallengePathScreen(
    navController: NavController,
    challengePath: ChallengePathResponse,
    onStartDayClick: (ChallengeTask) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    // logger
    val logger = remember { getLogger() }

    LaunchedEffect(challengePath) {
        logger.log("Navigated to ChallengePathScreen for track: ${challengePath.track}")
        logger.log("Challenge Path Details: ${challengePath.days.size} days")
    }


    Scaffold(
        topBar = {
            DevStreakTopBar(
                title = challengePath.track,
                onBack = { navController.popBackStack() }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = "🎉 You're all set! Day 1 awaits...",
                                duration = SnackbarDuration.Short
                            )
                            delay(500)
                            navController.navigate(Routes.HomeScreen) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("🚀 Start Your Journey", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Consistency = XP ⚡ Stay on the streak!",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(challengePath.days) { day ->
                ChallengeDayCard(day = day)
            }
        }
    }
}

@Composable
fun ChallengeDayCard(day: ChallengeTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "📅 Day ${day.day}: ${day.title}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            Spacer(Modifier.height(6.dp))

            val previewContent = day.content.take(100).trimEnd() + if (day.content.length > 100) "..." else ""
            Text(previewContent, style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = {},
                    label = { Text("⭐ XP: ${day.xp}") },
                    enabled = false,
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFFFFF9C4))
                )
                Spacer(Modifier.width(8.dp))
                AssistChip(
                    onClick = {},
                    label = { Text("📂 ${day.type}") },
                    enabled = false
                )
            }
        }
    }
}
