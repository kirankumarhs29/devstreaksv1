package com.dailydevchallenge.devstreaks.features.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.challenge.components.ChallengeStoryMissionCard
import com.dailydevchallenge.devstreaks.features.challenge.components.CoachDialogueCard
import com.dailydevchallenge.devstreaks.features.challenge.components.FlashcardActivityCard
import com.dailydevchallenge.devstreaks.features.challenge.components.QuizCard
import com.dailydevchallenge.devstreaks.features.routes.LearnRoute
import com.dailydevchallenge.devstreaks.features.routes.Routes
import com.dailydevchallenge.devstreaks.model.ActivityType
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.theme.extendedColors
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = koinInject()) {
    val requestId = remember { UserPreferences.getPendingRequestId()}
    val isCourseLoading by viewModel.isCourseLoading.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val today by viewModel.todayTask.collectAsState()
    val stats by viewModel.userStats.collectAsState()
    val progress = viewModel.challengeProgress.collectAsState()
    val eta by viewModel.estimatedEndDate.collectAsState()
    val completedTaskIds by viewModel.completedTaskIds.collectAsState()
    val trackName by viewModel.currentTrack.collectAsState()
    val currentDay = progress.value.first
    val totalDays = progress.value.second
    val level = stats.level // Use level directly instead of calculating from xp
    val onboardingCompleted by viewModel.onboardingCompleted

    val isFirstTimeUser = !onboardingCompleted && tasks.isEmpty() && stats.totalXp == 0 // Use totalXp
    val hasActiveChallenges = tasks.isNotEmpty() || today != null

    val motivationalQuotes = remember {
        listOf(
            "Your future self is counting on today's code 💪",
            "Every line of code is a step toward mastery 🚀",
            "Debug your limits, not just your code 🧠",
            "Small commits, big dreams ✨",
            "The best time to code was yesterday. The second best is now 🔥"
        )
    }
    val todayQuote = remember { motivationalQuotes.random() }

    val groupedTasks = tasks.groupBy { it.type }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    val quickPracticeTask by viewModel.quickPractice.collectAsState()
    var showQuickPracticeSheet by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) {
        if (!requestId.isNullOrEmpty()) {
            try {
                viewModel.loadGeneratedCourse(requestId)
            } catch (e: Exception) {
                // Handle error state
            }
        }
    }

    Scaffold(
        topBar = {
            if (isFirstTimeUser) {
                FirstTimeUserTopBar()
            } else {
                ModernTopBar(
                    streak = stats.currentStreak, // Use currentStreak instead of streak
                    level = level,
                    onRefresh = {
                        viewModel.refreshTasks()
                        viewModel.reloadStats()
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isFirstTimeUser) {
                ModernFAB(onClick = { navController.navigate(Routes.AIHub) })
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            if (isFirstTimeUser) {
                // First-time user experience
                item {
                    FirstTimeHeroSection(
                        onStartClick = { navController.navigate(Routes.LearningIntent) }
                    )
                }
                item { WhyDevStreakCard() }
                item { FeaturePreviewCards() }
                item { TestimonialCard() }
                item {
                    ReadyToStartCard(
                        onStartClick = { navController.navigate(Routes.LearningIntent) }
                    )
                }
            } else {
                // Existing user experience
                item {
                    HeroWelcomeSection(
                        stats = DisplayStats(
                            level = level,
                            xp = stats.totalXp, // Use totalXp instead of xp
                            streak = stats.currentStreak, // Use currentStreak instead of streak
                            currentDay = currentDay,
                            totalDays = totalDays,
                            trackName = trackName,
                            eta = eta.toString()
                        ),
                        onboardingCompleted = onboardingCompleted,
                        onStartClick = { navController.navigate(Routes.LearningIntent) }
                    )
                }

                // Daily Mission (if available)
                today?.let { todayTask ->
                    item {
                        CoachDialogueCard(
                            level = level,
                            logicScore = 0 // Temporary fix - model UserStats doesn't have logicScore
                        )
                    }
                    item {
                        ChallengeStoryMissionCard(
                            title = "Spot the Logic Flaw",
                            emoji = "🧠",
                            narrative = "You're reviewing your teammate's code today. Can you catch the bug before it hits production?"
                        )
                    }
                    item {
                        DailyMissionCard(
                            task = todayTask,
                            onClick = { navController.navigate(LearnRoute(todayTask.id)) }
                        )
                    }
                }

                // Quick Actions Row
                item {
                    QuickActionsRow(
                        onQuickPractice = { showQuickPracticeSheet = true },
                        onViewProgress = { navController.navigate(Routes.Progress) },
                        onDevCoach = { navController.navigate(Routes.DevCoach) }
                    )
                }

                // Learning Path Progress
                if (groupedTasks.isNotEmpty()) {
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
                                        exit = fadeOut(),
                                        modifier = Modifier.animateItem()
                                    ) {
                                        ChallengeMiniCard(task = task, isCompleted = isCompleted) {
                                            navController.navigate(LearnRoute(task.id))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Motivational Quote
                item {
                    MotivationalCard(quote = todayQuote)
                }
            }
        }

        // Bottom Sheet for AI Assistant
        if (showSheet) {
            AIAssistantBottomSheet(
                sheetState = sheetState,
                onDismiss = { showSheet = false },
                navController = navController
            )
        }

        // Quick Practice Sheet
        if (showQuickPracticeSheet && quickPracticeTask != null) {
            QuickPracticeBottomSheet(
                task = quickPracticeTask!!,
                onDismiss = { showQuickPracticeSheet = false },
                onComplete = {
                    viewModel.markTaskCompleted(quickPracticeTask!!.id, quickPracticeTask!!.xp)
                    showQuickPracticeSheet = false
                }
            )
        }
    }
}

@Composable
fun FirstTimeUserTopBar() {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🚀 DevStreak",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ModernTopBar(
    streak: Int,
    level: Int,
    onRefresh: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DevStreak",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Level $level Developer",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StreakBadge(streak = streak)
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun StreakBadge(streak: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.extendedColors.achievement,
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "🔥",
                fontSize = 24.sp,
                color = MaterialTheme.extendedColors.onAchievement,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$streak",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.extendedColors.onAchievement,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FirstTimeHeroSection(onStartClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Animated background elements
            AnimatedBackground()

            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Hero emoji with animation
                var isVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    isVisible = true
                }

                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(1000)) +
                            scaleIn(animationSpec = tween(1000))
                ) {
                    Text(
                        text = "🚀",
                        fontSize = 80.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Text(
                    text = "Transform Into a\nSoftware Developer",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp
                )

                Text(
                    text = "Join thousands of developers building coding habits that last. Just 15 minutes a day!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                // Stats preview
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    StatPreview("50K+", "Developers")
                    StatPreview("15min", "Daily")
                    StatPreview("100%", "Free")
                }

                AnimatedStartButton(onClick = onStartClick)
            }
        }
    }
}

@Composable
fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(rotationZ = rotation)
            .alpha(0.1f)
    ) {
        repeat(5) { index ->
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size((50 + index * 30).dp)
                    .offset(
                        x = (index * 40).dp,
                        y = (index * -20).dp
                    )
                    .alpha(0.3f - index * 0.05f)
            ) {}
        }
    }
}

@Composable
fun StatPreview(number: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun AnimatedStartButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        ),
        interactionSource = interactionSource
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Text(
                "Start Your Journey",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text("✨", fontSize = 20.sp)
        }
    }
}

@Composable
fun WhyDevStreakCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Why DevStreak Works 🎯",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            BenefitItem(
                icon = "🧠",
                title = "Micro-Learning",
                description = "Bite-sized lessons that fit your schedule"
            )
            BenefitItem(
                icon = "🔥",
                title = "Habit Building",
                description = "Consistent daily practice builds muscle memory"
            )
            BenefitItem(
                icon = "🎮",
                title = "Gamified Progress",
                description = "Level up, earn XP, and track your growth"
            )
            BenefitItem(
                icon = "🤖",
                title = "AI-Powered",
                description = "Personalized learning path just for you"
            )
        }
    }
}

@Composable
fun BenefitItem(icon: String, title: String, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun FeaturePreviewCards() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "What You'll Experience 🌟",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeaturePreviewCard(
                modifier = Modifier.weight(1f),
                title = "Daily Challenges",
                description = "Fun coding puzzles",
                emoji = "🧩",
                color = MaterialTheme.colorScheme.primaryContainer
            )
            FeaturePreviewCard(
                modifier = Modifier.weight(1f),
                title = "Progress Tracking",
                description = "See your growth",
                emoji = "📈",
                color = MaterialTheme.colorScheme.tertiaryContainer
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeaturePreviewCard(
                modifier = Modifier.weight(1f),
                title = "AI Coaching",
                description = "Personal mentor",
                emoji = "🤖",
                color = MaterialTheme.colorScheme.secondaryContainer
            )
            FeaturePreviewCard(
                modifier = Modifier.weight(1f),
                title = "Community",
                description = "Learn together",
                emoji = "👥",
                color = MaterialTheme.colorScheme.errorContainer
            )
        }
    }
}

@Composable
fun FeaturePreviewCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    emoji: String,
    color: Color
) {
    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = emoji,
                fontSize = 32.sp
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun TestimonialCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "💬",
                fontSize = 40.sp
            )
            Text(
                text = "\"DevStreak helped me land my first developer job in just 3 months. The daily habit made all the difference!\"",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "- Sarah, Frontend Developer",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ReadyToStartCard(onStartClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Ready to Begin? 🚀",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "Your coding journey starts with a single click. Let's build something amazing together!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            )

            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Create My Learning Path",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("✨", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun HeroWelcomeSection(
    stats: DisplayStats,
    onboardingCompleted: Boolean,
    onStartClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!onboardingCompleted) {
                    WelcomeMessage()
                } else {
                    WelcomeBackMessage(stats.streak)
                }

                ProgressSection(stats)

                if (!onboardingCompleted) {
                    GetStartedButton(onClick = onStartClick)
                }
            }
        }
    }
}

@Composable
fun WelcomeMessage() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Welcome to DevStreak! 🚀",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = "Your coding journey starts here. Build habits, gain skills, level up daily.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun WelcomeBackMessage(streak: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Welcome back, Developer! 🔥",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = "Day $streak of your coding streak. You're building something amazing!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun ProgressSection(stats: DisplayStats) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressCircle(
                level = stats.level,
                xp = stats.xp
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ProgressStat("📈", "Level ${stats.level}")
                ProgressStat("🎯", "Day ${stats.currentDay}/${stats.totalDays}")
                ProgressStat("🛤️", stats.trackName)
                ProgressStat("⏰", "ETA: ${stats.eta}")
            }
        }
    }
}

@Composable
fun ProgressStat(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
        )
    }
}

@Composable
fun ProgressCircle(level: Int, xp: Int) {
    val progress = (xp % 100) / 100f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "progress_animation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(80.dp)
    ) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            strokeWidth = 6.dp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
            modifier = Modifier.size(80.dp)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "L$level",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun DailyMissionCard(
    task: ChallengeTask,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(60.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "🎯",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Today's Mission",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = "Day ${task.day} • +${task.xp} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Start Mission",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun QuickActionsRow(
    onQuickPractice: () -> Unit,
    onViewProgress: () -> Unit,
    onDevCoach: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton(
            modifier = Modifier.weight(1f),
            icon = "⚡",
            title = "Quick Practice",
            subtitle = "2 min",
            onClick = onQuickPractice
        )
        QuickActionButton(
            modifier = Modifier.weight(1f),
            icon = "📊",
            title = "Progress",
            subtitle = "View stats",
            onClick = onViewProgress
        )
        QuickActionButton(
            modifier = Modifier.weight(1f),
            icon = "🤖",
            title = "AI Coach",
            subtitle = "Get help",
            onClick = onDevCoach
        )
    }
}

@Composable
fun QuickActionButton(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ModernFAB(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        text = {
            Text(
                "Ask AI",
                fontWeight = FontWeight.Bold
            )
        },
        icon = {
            Icon(
                Icons.Default.Chat,
                contentDescription = null
            )
        },
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun MotivationalCard(quote: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💡",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = quote,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun GetStartedButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "Start Your Journey",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
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

// Bottom sheet composables
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAssistantBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    navController: NavController
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
                onDismiss()
                navController.navigate(Routes.LearningIntent)
            }
            SheetOption("📊 View Progress Report") {
                onDismiss()
                navController.navigate(Routes.Progress)
            }
            SheetOption("Interview Home") {
                onDismiss()
                navController.navigate(Routes.InterviewHome)
            }
            SheetOption("🔁 Review My Resume") {
                onDismiss()
                navController.navigate(Routes.ResumeAnalysis)
            }
            SheetOption("💬 Talk to DevCoach") {
                onDismiss()
                navController.navigate(Routes.DevCoach)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPracticeBottomSheet(
    task: ChallengeTask,
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    val challenge = task.challenges.firstOrNull()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("⚡ Quick Practice", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            when (challenge?.type) {
                ActivityType.QUIZ -> {
                    QuizCard(activity = challenge) {
                        onComplete()
                    }
                }
                ActivityType.FLASHCARD -> {
                    FlashcardActivityCard(activity = challenge) {
                        onComplete()
                    }
                }
                else -> {
                    Text("Unsupported quick practice type.")
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
