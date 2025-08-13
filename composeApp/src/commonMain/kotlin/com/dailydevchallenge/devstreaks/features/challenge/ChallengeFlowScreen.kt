package com.dailydevchallenge.devstreaks.features.challenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.dailydevchallenge.devstreaks.features.challenge.components.CompletionCard
import com.dailydevchallenge.devstreaks.model.ChallengeTask


@Composable
fun ChallengeFlowScreen(
    navController: NavController,
    task: ChallengeTask,
    isCompleted: Boolean = false,
    onMarkAsDone: () -> Unit
) {
    val viewModel: ChallengeDetailViewModel = remember { ChallengeDetailViewModel(task, isCompleted) }
    val uiState by viewModel.uiState.collectAsState()

    when (uiState.step) {
        ChallengeStep.LEARN -> ChallengeLearnScreen(
            task = task,
            onNext = { viewModel.goToDo() },
            navController = navController
        )
        ChallengeStep.DO -> ChallengeDetailStepScreen(
            task = task,
            onMarkAsDone = {
                onMarkAsDone()
                viewModel.goToComplete()
            }
        )
        ChallengeStep.COMPLETE -> CompletionCard(
            onDismiss = { navController.popBackStack() },
            message = "Streak Achieved! 🎉 +${task.xp} XP"
        )
    }
}

@Composable
fun ChallengeDetailStepScreen(
    task: ChallengeTask,
    onMarkAsDone: () -> Unit
) {
    ChallengeDetailScreen(
        navController = rememberNavController(), // or pass NavController if needed
        day = task,
        isCompleted = false,
        onMarkAsDone = onMarkAsDone
    )
}
