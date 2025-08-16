package com.dailydevchallenge.devstreaks.features.challenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.dailydevchallenge.devstreaks.features.challenge.components.CompletionCard
import com.dailydevchallenge.devstreaks.features.home.UserStatsManager
import com.dailydevchallenge.devstreaks.llm.AIFeedbackService
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.model.DifficultyLevel
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.service.AdaptiveIntelligenceOrchestrator
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfilePreferences
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import org.koin.compose.koinInject

/**
 * Enhanced Challenge Flow Screen with integrated adaptive intelligence features:
 * - Dynamic difficulty adjustment based on user performance
 * - Personalized AI coaching throughout the challenge
 * - Real-time weak area detection and recommendations
 * - Adaptive XP scaling based on challenge complexity
 */
@Composable
fun ChallengeFlowScreen(
    navController: NavController,
    task: ChallengeTask,
    isCompleted: Boolean = false,
    onMarkAsDone: () -> Unit
) {
    val repository: ChallengeRepository = koinInject()
    val userStatsManager: UserStatsManager = koinInject()
    val aiFeedbackService: AIFeedbackService = koinInject()
    val adaptiveOrchestrator: AdaptiveIntelligenceOrchestrator = koinInject()
    val profilePreferences: LearningProfilePreferences = koinInject()

    // Use adaptive intelligence-enhanced ViewModel
    val viewModel: ChallengeDetailViewModel = remember {
        ChallengeDetailViewModel(
            challengeRepository = repository,
            userStatsManager = userStatsManager,
            aiFeedbackService = aiFeedbackService,
            adaptiveOrchestrator = adaptiveOrchestrator,
            profilePreferences = profilePreferences
        )
    }

    val uiState by viewModel.uiState.collectAsState()
    val adaptiveConfig by viewModel.adaptiveConfig.collectAsState()
    val personalizedCoaching by viewModel.personalizedCoaching.collectAsState()

    // Initialize adaptive intelligence for this challenge
    LaunchedEffect(task.id) {
        val userId = UserPreferences.getSafeUserId()
        viewModel.loadTask(task.id)

        // Initialize adaptive intelligence with user context
        viewModel.initializeAdaptiveIntelligence(
            userId = userId,
            challengeTask = task,
            skillArea = task.skill
        )
    }

    when (uiState.step) {
        ChallengeStep.LEARN -> ChallengeLearnScreen(
            task = task,
            onNext = {
                viewModel.goToDo()
                // Track learning phase completion for adaptive intelligence
                viewModel.trackLearningPhaseCompletion()
            },
            navController = navController,
            // Pass adaptive intelligence context for enhanced learning experience
            adaptiveConfig = adaptiveConfig,
            personalizedCoaching = personalizedCoaching
        )
        ChallengeStep.DO -> ChallengeDetailStepScreen(
            task = task,
            onMarkAsDone = {
                onMarkAsDone()
                viewModel.goToComplete()
                // Complete challenge with adaptive intelligence tracking
                viewModel.completeAdaptiveChallenge()
            },
            adaptiveConfig = adaptiveConfig,
            personalizedCoaching = personalizedCoaching
        )
        ChallengeStep.COMPLETE -> {
            // Calculate adaptive XP bonus based on difficulty and performance
            val baseXP = task.xp
            val adaptiveMultiplier = adaptiveConfig?.adaptiveXpMultiplier ?: 1.0
            val totalXP = (baseXP * adaptiveMultiplier).toInt()
            val bonusXP = totalXP - baseXP

            CompletionCard(
                onDismiss = { navController.popBackStack() },
                message = buildAdaptiveCompletionMessage(
                    baseXP = baseXP,
                    bonusXP = bonusXP,
                    difficultyLevel = adaptiveConfig?.recommendedDifficulty ?: DifficultyLevel.MEDIUM,
                    personalizedMessage = personalizedCoaching?.motivationalMessage
                )
            )
        }
    }
}

/**
 * Enhanced ChallengeDetailStepScreen with adaptive intelligence support
 */
@Composable
fun ChallengeDetailStepScreen(
    task: ChallengeTask,
    onMarkAsDone: () -> Unit,
    adaptiveConfig: com.dailydevchallenge.devstreaks.model.AdaptiveChallengeConfig? = null,
    personalizedCoaching: com.dailydevchallenge.devstreaks.model.PersonalizedCoachingResponse? = null
) {
    ChallengeDetailScreen(
        navController = rememberNavController(),
        day = task,
        isCompleted = false,
        onMarkAsDone = onMarkAsDone,
        // Pass adaptive intelligence context to enhance the challenge experience
        adaptiveConfig = adaptiveConfig,
        personalizedCoaching = personalizedCoaching
    )
}

/**
 * Build adaptive completion message based on performance and difficulty
 */
private fun buildAdaptiveCompletionMessage(
    baseXP: Int,
    bonusXP: Int,
    difficultyLevel: DifficultyLevel,
    personalizedMessage: String?
): String {
    val difficultyEmoji = when (difficultyLevel) {
        DifficultyLevel.EASY -> "🌱"
        DifficultyLevel.MEDIUM -> "🚀"
        DifficultyLevel.HARD -> "💪"
        DifficultyLevel.EXPERT -> "🔥"
        else -> "✨"
    }

    val baseMessage = "Streak Achieved! $difficultyEmoji +$baseXP XP"

    return when {
        bonusXP > 0 -> "$baseMessage (+$bonusXP bonus) = ${baseXP + bonusXP} XP total!"
        personalizedMessage != null -> "$baseMessage\n\n💡 $personalizedMessage"
        else -> baseMessage
    }
}
