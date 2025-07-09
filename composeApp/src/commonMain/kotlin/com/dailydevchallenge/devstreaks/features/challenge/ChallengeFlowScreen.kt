package com.dailydevchallenge.devstreaks.features.challenge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.dailydevchallenge.devstreaks.model.ChallengeTask

enum class ChallengeStep { LEARN, DO, COMPLETE }


@Composable
fun ChallengeFlowScreen(
    navController: NavController,
    task: ChallengeTask,
    isCompleted: Boolean = false,
    onMarkAsDone: () -> Unit
) {
    var currentStep by remember { mutableStateOf(ChallengeStep.LEARN) }

    when (currentStep) {
        ChallengeStep.LEARN -> ChallengeLearnScreen(
            task = task,
            onNext = { currentStep = ChallengeStep.DO }
        )

        ChallengeStep.DO -> ChallengeDetailStepScreen(task,
            onMarkAsDone = {
                onMarkAsDone()
                currentStep = ChallengeStep.COMPLETE
            }
        )

        ChallengeStep.COMPLETE -> CompletionScreen(
            xp = task.xp,
            onContinue = { navController.popBackStack() }
        )
    }
}

@Composable
fun CompletionScreen(xp: Int, onContinue: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🎉 Great job!", style = MaterialTheme.typography.headlineMedium)
            Text("+$xp XP", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onContinue) {
                Text("Back to Home")
            }
        }
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




