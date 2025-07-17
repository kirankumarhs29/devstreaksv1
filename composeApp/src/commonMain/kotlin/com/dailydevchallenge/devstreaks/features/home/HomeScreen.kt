package com.dailydevchallenge.devstreaks.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.routes.LearnRoute
import com.dailydevchallenge.devstreaks.features.navigation.DevStreakTopBar
import com.dailydevchallenge.devstreaks.features.routes.Routes
import com.dailydevchallenge.devstreaks.features.challenge.components.FlashcardActivityCard
import com.dailydevchallenge.devstreaks.features.challenge.components.QuizCard
import com.dailydevchallenge.devstreaks.model.ActivityType
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import devstreaks.composeapp.generated.resources.Res
import devstreaks.composeapp.generated.resources.hero_banner
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = koinInject()) {
    val tasks by viewModel.tasks.collectAsState()
    val today by viewModel.todayTask.collectAsState()
    val stats by viewModel.userStats.collectAsState()
    val progress = viewModel.challengeProgress.collectAsState()
    val eta by viewModel.estimatedEndDate.collectAsState()
    val completedTaskIds by viewModel.completedTaskIds.collectAsState()
    val trackName by viewModel.currentTrack.collectAsState()

    val currentDay = progress.value.first
    val totalDays = progress.value.second
//    val percent = if (totalDays == 0) 0f else currentDay.toFloat() / totalDays
    val level = stats.xp / 100
    val quote = remember {
        listOf(
            "Every expert was once a beginner.",
            "Keep showing up. Progress compounds.",
            "Code like today defines your tomorrow.",
            "Tiny steps build massive momentum."
        ).random()
    }

    val groupedTasks = tasks.groupBy { it.type }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val quickPracticeTask by viewModel.quickPractice.collectAsState()
    var showQuickPracticeSheet by remember { mutableStateOf(false) }


    Scaffold(
        topBar = {
            DevStreakTopBar(
                title = "DevStreak",
                actions = {
                    IconButton(onClick = {
                        viewModel.refreshTasks()
                        viewModel.reloadStats()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Ask Me") },
                icon = { Icon(Icons.Default.Chat, contentDescription = null) },
                onClick = { showSheet = true }
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
//            if (stats.xp == 0) {
//                item {
//                    OnboardingCard {
//                        navController.navigate(Routes.LearningIntent)
//                    }
//                }
//            }
//            item {
//                Text("Welcome back! 🔥", style = MaterialTheme.typography.titleLarge)
//                Text("You're on a ${stats.streak}-day streak.", style = MaterialTheme.typography.bodyMedium)
//            }
//            item {
//                UserStatsCard(level, stats.xp, stats.streak, currentDay, totalDays, eta.toString(), trackName)
//            }
            val stats1: () -> DisplayStats = {
                DisplayStats(
                    level = level,
                    xp = stats.xp,
                    streak = stats.streak,
                    currentDay = currentDay,
                    totalDays = totalDays,
                    trackName = trackName,
                    eta = eta.toString()
                )
            }

            val onboardingCompleted by viewModel.onboardingCompleted
            if (onboardingCompleted) {
                item {
                    WelcomeHeroCard(
                        stats = stats1,
                        onStartClick = {
                            viewModel.setOnboardingCompleted(true)
                            navController.navigate(Routes.LearningIntent)
                        },
                        onBoardingCompleted = onboardingCompleted
                    )
                }
            }
             if (today != null) {
                    item {
                        HeroBanner(task = today!!) {
                                navController.navigate(LearnRoute(today!!.id))
                        }
                    }
            }

            groupedTasks.forEach { (type, group) ->
                item {
                    Text("🎯 ${type.uppercase()}", style = MaterialTheme.typography.titleMedium)
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(group, key = { it.id }) { task ->
                            val isCompleted = completedTaskIds.contains(task.id)
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                ChallengeMiniCard(task = task, isCompleted = isCompleted) {
                                    navController.navigate(LearnRoute(task.id))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "💬 $quote",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("🧠 What do you want help with?", style = MaterialTheme.typography.titleMedium)

                    SheetOption("✍️ Generate Custom Plan") {
                        showSheet = false
                        navController.navigate(Routes.LearningIntent)
                    }

                    SheetOption("📊 View Progress Report") {
                        showSheet = false
                        navController.navigate(Routes.Progress)
                    }

                    SheetOption("🧩 Quick Practice Task") {
                        showSheet = false
                    }

                    SheetOption("🔁 Review My Resume") {
                        showSheet = false
                        navController.navigate(Routes.ResumeAnalysis)
                    }

                    SheetOption("💬 Talk to DevCoach") {
                        showSheet = false
                        navController.navigate(Routes.DevCoach)
                    }

                }
            }
        }
        if (showQuickPracticeSheet && quickPracticeTask != null) {
            val challenge = quickPracticeTask!!.challenges.firstOrNull()

            ModalBottomSheet(
                onDismissRequest = { showQuickPracticeSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("⚡ Quick Practice", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))

                    when (challenge?.type) {
                        ActivityType.QUIZ -> {
                            QuizCard(activity = challenge) {
                                viewModel.markTaskCompleted(quickPracticeTask!!.id, quickPracticeTask!!.xp)
                            }
                        }

                        ActivityType.FLASHCARD -> {
                            FlashcardActivityCard(activity = challenge) {
                                viewModel.markTaskCompleted(quickPracticeTask!!.id, quickPracticeTask!!.xp)
                            }
                        }

                        else -> {
                            Text("Unsupported quick practice type.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SheetOption(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


@Composable
fun HeroBanner(task: ChallengeTask, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp) // reduced height for modern compactness
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        // Background Image
        Image(
            painter = painterResource(Res.drawable.hero_banner),
            contentDescription = "Today's Task",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // Subtle gradient overlay for readability
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f)
                        )
                    )
                )
        )

        // Glass card overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top label
            Text(
                text = "Day ${task.day} • ${task.type.uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.85f)
            )

            // Centered mission title and CTA
            Column(
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 120.dp)
                        .height(36.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Tap to Start",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeMiniCard(task: ChallengeTask, isCompleted: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isCompleted)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val textColor = if (isCompleted)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .width(200.dp)
            .height(130.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(task.title, style = MaterialTheme.typography.bodyMedium, color = textColor, maxLines = 2)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                if (!isCompleted) {
                    Text("🎁 +${task.xp} XP", style = MaterialTheme.typography.labelSmall, color = textColor)
                }
                Text(if (isCompleted) "✅" else "⏳", modifier = Modifier.padding(start = 8.dp))

            }
        }
    }
}

@Composable
fun UserStatsContent(
    level: Int,
    xp: Int,
    streak: Int,
    currentDay: Int,
    totalDays: Int,
    eta: String,
    currentTrack: String
) {
    val progress = (xp % 100) / 100f
    val animatedProgress by animateFloatAsState(targetValue = progress)
    val glowLevelUp = remember { derivedStateOf { xp % 100 == 0 && xp != 0 } }
    val glow by animateFloatAsState(
        targetValue = if (glowLevelUp.value) 1.1f else 1f,
        animationSpec = tween(durationMillis = 500)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .graphicsLayer(scaleX = glow, scaleY = glow)
            .fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                strokeWidth = 8.dp,
                modifier = Modifier.size(90.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lv $level", style = MaterialTheme.typography.titleMedium)
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text("🔥 $streak-Day Streak", style = MaterialTheme.typography.bodyMedium)
            Text("📅 Day $currentDay of $totalDays", style = MaterialTheme.typography.bodyMedium)
            Text("⭐ ${xp % 100} XP to next level", style = MaterialTheme.typography.bodyMedium)
            Text("🛤️ Track: $currentTrack", style = MaterialTheme.typography.bodyMedium)
            Text("📆 ETA: $eta", style = MaterialTheme.typography.bodySmall)
        }
    }
}


@Composable
fun TapToStartButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1f)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .height(44.dp)
            .defaultMinSize(minWidth = 140.dp),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Tap to Start",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
// adding onboarding for first time users
@Composable
fun OnboardingCard(onStartClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🚀 Welcome to DevStreak!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = "Ready to build your streak and grow your dev skills daily?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            TapToStartButton(onClick = onStartClick)
        }
    }
}
@Composable
fun WelcomeHeroCard(
    stats: () -> DisplayStats,
    onStartClick: () -> Unit,
    onBoardingCompleted: Boolean
) {
    val values = stats()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (!onBoardingCompleted) "🚀 Welcome to DevStreak!" else "🔥 Welcome back!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = if (!onBoardingCompleted)
                    "Ready to build your streak and grow your dev skills daily?"
                else
                    "You're on a ${values.streak}-day streak. Let's keep the momentum!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Use non-card stats layout for alignment consistency
            UserStatsContent(
                level = values.level,
                xp = values.xp,
                streak = values.streak,
                currentDay = values.currentDay,
                totalDays = values.totalDays,
                eta = values.eta,
                currentTrack = values.trackName
            )

            if (!onBoardingCompleted) {
                TapToStartButton(onClick = onStartClick)
            }
        }
    }
}



