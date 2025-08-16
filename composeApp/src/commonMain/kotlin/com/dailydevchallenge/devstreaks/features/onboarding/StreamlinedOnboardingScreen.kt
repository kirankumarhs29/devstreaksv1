package com.dailydevchallenge.devstreaks.features.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.model.LearningIntent
import com.dailydevchallenge.devstreaks.theme.extendedColors
import com.dailydevchallenge.devstreaks.features.routes.Routes
import com.dailydevchallenge.devstreaks.utils.getLogger
import org.koin.compose.koinInject

private val logger = getLogger()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamlinedOnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = koinInject()
) {
    var currentQuestion by remember { mutableStateOf(0) }
    var selectedGoal by remember { mutableStateOf("") }
    var selectedExperience by remember { mutableStateOf("") }
    var selectedTimeCommitment by remember { mutableStateOf(0) }
    var isCompleting by remember { mutableStateOf(false) }

    val questions = listOf(
        QuestionData.GoalQuestion,
        QuestionData.ExperienceQuestion,
        QuestionData.TimeQuestion
    )

    // Only complete when all questions are answered and user hasn't started completing yet
    LaunchedEffect(currentQuestion) {
        if (currentQuestion >= questions.size && !isCompleting &&
            selectedGoal.isNotEmpty() && selectedExperience.isNotEmpty() && selectedTimeCommitment > 0) {
            isCompleting = true
            logger.d("All questions completed. Moving to completion screen.")

            // Create learning intent but don't start completion here
            // The CompletionScreen will handle the actual API call
            val learningIntent = LearningIntent(
                primaryGoal = selectedGoal,
                experience = selectedExperience,
                timePerDay = selectedTimeCommitment,
                skillFocus = inferSkillsFromGoal(selectedGoal),
                learningStyle = "adaptive",
                careerTrack = selectedGoal
            )
            logger.d("Created LearningIntent for completion screen")
            viewModel.updateLearningIntent(learningIntent)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Quick Setup")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { (currentQuestion + 1) / questions.size.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                color = MaterialTheme.colorScheme.primary,
            )

            // Question content
            AnimatedContent(
                targetState = currentQuestion,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300)
                    )
                }
            ) { questionIndex ->
                if (questionIndex < questions.size) {
                    when (questions[questionIndex]) {
                        is QuestionData.GoalQuestion -> {
                            GoalSelectionCard(
                                selectedGoal = selectedGoal,
                                onGoalSelected = { goal ->
                                    selectedGoal = goal
                                    currentQuestion++
                                }
                            )
                        }
                        is QuestionData.ExperienceQuestion -> {
                            ExperienceSelectionCard(
                                selectedExperience = selectedExperience,
                                onExperienceSelected = { experience ->
                                    selectedExperience = experience
                                    currentQuestion++
                                }
                            )
                        }
                        is QuestionData.TimeQuestion -> {
                            TimeCommitmentCard(
                                selectedTime = selectedTimeCommitment,
                                onTimeSelected = { time ->
                                    selectedTimeCommitment = time
                                    currentQuestion++
                                }
                            )
                        }
                    }
                } else {
                    // Show completion screen when all questions are done
                    CompletionScreen(
                        viewModel = viewModel,
                        isCompleting = isCompleting,
                        onComplete = {
                            navController.navigate(Routes.Login) {
                                popUpTo(Routes.OnboardingScreen) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalSelectionCard(
    selectedGoal: String,
    onGoalSelected: (String) -> Unit
) {
    QuestionCard(
        title = "What's your main coding goal?",
        subtitle = "We'll customize your learning path based on this"
    ) {
        val goals = listOf(
            GoalOption("🚀 Get my first dev job", "career_switch", Icons.Default.Work),
            GoalOption("📱 Build mobile apps", "mobile_dev", Icons.Default.PhoneAndroid),
            GoalOption("🌐 Master web development", "web_dev", Icons.Default.Web),
            GoalOption("🤖 Learn AI/ML", "ai_ml", Icons.Default.Psychology),
            GoalOption("⚡ Level up current skills", "skill_upgrade", Icons.Default.TrendingUp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(goals) { goal ->
                OptionCard(
                    icon = goal.icon,
                    title = goal.displayText,
                    isSelected = selectedGoal == goal.value,
                    onClick = { onGoalSelected(goal.value) }
                )
            }
        }
    }
}

@Composable
private fun ExperienceSelectionCard(
    selectedExperience: String,
    onExperienceSelected: (String) -> Unit
) {
    QuestionCard(
        title = "What's your experience level?",
        subtitle = "Be honest - we'll adjust the difficulty accordingly"
    ) {
        val experiences = listOf(
            ExperienceOption("🌱 Complete beginner", "beginner", "Never written code before"),
            ExperienceOption("📚 Some basics", "basic", "Learned some programming concepts"),
            ExperienceOption("🔧 Can build things", "intermediate", "Built projects, comfortable with code"),
            ExperienceOption("⚡ Pretty experienced", "advanced", "Professional or extensive experience")
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(experiences) { exp ->
                DetailedOptionCard(
                    title = exp.displayText,
                    subtitle = exp.description,
                    isSelected = selectedExperience == exp.value,
                    onClick = { onExperienceSelected(exp.value) }
                )
            }
        }
    }
}

@Composable
private fun TimeCommitmentCard(
    selectedTime: Int,
    onTimeSelected: (Int) -> Unit
) {
    QuestionCard(
        title = "How much time daily?",
        subtitle = "We'll create a realistic plan that fits your schedule"
    ) {
        val timeOptions = listOf(
            TimeOption("⚡ Quick sessions", 15, "15 min/day"),
            TimeOption("🎯 Focused learning", 30, "30 min/day"),
            TimeOption("💪 Deep dive", 60, "1 hour/day"),
            TimeOption("🚀 Intensive", 120, "2+ hours/day")
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(timeOptions) { timeOpt ->
                DetailedOptionCard(
                    title = timeOpt.displayText,
                    subtitle = timeOpt.description,
                    isSelected = selectedTime == timeOpt.minutes,
                    onClick = { onTimeSelected(timeOpt.minutes) }
                )
            }
        }
    }
}

@Composable
private fun QuestionCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            content()
        }
    }
}

@Composable
private fun OptionCard(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 12.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun DetailedOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 12.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// Data classes for options
private data class GoalOption(
    val displayText: String,
    val value: String,
    val icon: ImageVector
)

private data class ExperienceOption(
    val displayText: String,
    val value: String,
    val description: String
)

private data class TimeOption(
    val displayText: String,
    val minutes: Int,
    val description: String
)

// Question types
private sealed class QuestionData {
    data object GoalQuestion : QuestionData()
    data object ExperienceQuestion : QuestionData()
    data object TimeQuestion : QuestionData()
}

// Helper function to infer skills from goal
private fun inferSkillsFromGoal(goal: String): List<String> {
    return when (goal) {
        "career_switch" -> listOf("fundamentals", "interview_prep", "portfolio")
        "mobile_dev" -> listOf("kotlin", "swift", "react_native", "flutter")
        "web_dev" -> listOf("html", "css", "javascript", "react", "node")
        "ai_ml" -> listOf("python", "tensorflow", "data_science", "algorithms")
        "skill_upgrade" -> listOf("advanced_concepts", "system_design", "best_practices")
        else -> listOf("programming_basics")
    }
}

@Composable
private fun CompletionScreen(
    viewModel: OnboardingViewModel,
    isCompleting: Boolean,
    onComplete: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var hasStartedGeneration by remember { mutableStateOf(false) }

    // Start the course generation when this screen is first shown
    LaunchedEffect(isCompleting, hasStartedGeneration) {
        if (isCompleting && !hasStartedGeneration && uiState.learningIntent != null) {
            hasStartedGeneration = true
            logger.d("CompletionScreen: Starting course generation")
            viewModel.completeStreamlinedOnboarding()
        }
    }

    // Monitor completion state
    LaunchedEffect(uiState.isStreamlinedComplete, uiState.errorMessage) {
        if (uiState.isStreamlinedComplete) {
            logger.d("CompletionScreen: Course generation completed successfully")
            kotlinx.coroutines.delay(1000) // Brief success display
            onComplete()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                uiState.errorMessage != null -> {
                    // Error state
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Something went wrong",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = uiState.errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            viewModel.clearError()
                            viewModel.completeStreamlinedOnboarding()
                        }
                    ) {
                        Text("Try Again")
                    }
                }

                uiState.isStreamlinedComplete -> {
                    // Success state
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "All Set! 🎉",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Your personalized learning path is ready",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                else -> {
                    // Loading state
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Creating your learning path...",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "This will just take a moment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress indicators with animation
                    val progressSteps = listOf(
                        "Analyzing your goals...",
                        "Customizing difficulty...",
                        "Building your curriculum...",
                        "Setting up practice sessions..."
                    )

                    var currentStep by remember { mutableStateOf(0) }

                    LaunchedEffect(Unit) {
                        while (currentStep < progressSteps.size - 1) {
                            kotlinx.coroutines.delay(800)
                            currentStep = (currentStep + 1) % progressSteps.size
                        }
                    }

                    Text(
                        text = progressSteps[currentStep],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
