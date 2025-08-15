package com.dailydevchallenge.devstreaks.features.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.dailydevchallenge.devstreaks.theme.extendedColors
import com.dailydevchallenge.devstreaks.features.routes.Routes
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegratedOnboardingScreen(
    navController: NavController,
    viewModel: OnboardingViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            OnboardingTopBar(
                currentStep = uiState.currentStep,
                progress = uiState.progress,
                canGoBack = uiState.canGoBack,
                onBackClick = { viewModel.previousStep() }
            )
        },
        bottomBar = {
            OnboardingBottomBar(
                currentStep = uiState.currentStep,
                canGoNext = uiState.canGoNext,
                isLoading = uiState.isLoading,
                onNextClick = {
                    if (uiState.currentStep == OnboardingStep.PERSONALIZATION) {
                        viewModel.generatePersonalizedPath()
                    } else {
                        viewModel.nextStep()
                    }
                },
                onSkipClick = { viewModel.skipStep() },
                onFinishClick = {
                    // After onboarding completion, redirect to login for authentication
                    navController.navigate(Routes.Login) {
                        popUpTo(Routes.OnboardingScreen) { inclusive = true }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main content with smooth step transitions
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { if (targetState.stepNumber > initialState.stepNumber) it else -it },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(300)) togetherWith
                    slideOutHorizontally(
                        targetOffsetX = { if (targetState.stepNumber > initialState.stepNumber) -it else it },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(300))
                },
                modifier = Modifier.fillMaxSize()
            ) { step ->
                when (uiState.currentStep) {
                    OnboardingStep.WELCOME -> {
                        WelcomeStep(
                            onGetStarted = { viewModel.nextStep() },
                            onQuickSetup = { viewModel.startStreamlinedOnboarding() }
                        )
                    }
                    OnboardingStep.STREAMLINED -> {
                        StreamlinedOnboardingScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }
                    OnboardingStep.LEARNING_INTENT -> {
                        LearningIntentStep(
                            currentIntent = uiState.learningIntent,
                            onIntentUpdate = viewModel::updateLearningIntent
                        )
                    }
                    OnboardingStep.PERSONALIZATION -> {
                        PersonalizationStep(
                            learningIntent = uiState.learningIntent,
                            onPersonalize = { viewModel.generatePersonalizedPath() }
                        )
                    }
                    OnboardingStep.GENERATION -> {
                        GenerationStep(
                            isLoading = uiState.isLoading,
                            challengePath = uiState.challengePath,
                            errorMessage = uiState.errorMessage,
                            onRetry = { viewModel.retryGeneration() },
                            onComplete = {
                                // After onboarding completion, redirect to login for authentication
                                navController.navigate(Routes.Login) {
                                    popUpTo(Routes.OnboardingScreen) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }

            // Error overlay
            uiState.errorMessage?.let { error ->
                ErrorOverlay(
                    message = error,
                    onDismiss = { viewModel.clearError() },
                    onRetry = { viewModel.retryGeneration() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingTopBar(
    currentStep: OnboardingStep,
    progress: Float,
    canGoBack: Boolean,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = currentStep.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Step ${currentStep.stepNumber} of ${currentStep.totalSteps}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            if (canGoBack) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Go back"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun OnboardingBottomBar(
    currentStep: OnboardingStep,
    canGoNext: Boolean,
    isLoading: Boolean,
    onNextClick: () -> Unit,
    onSkipClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    // Only show bottom bar for non-streamlined steps
    if (currentStep == OnboardingStep.STREAMLINED) {
        return
    }

    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = { currentStep.stepNumber / currentStep.totalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Skip button (for non-essential steps)
                if (currentStep != OnboardingStep.GENERATION && currentStep != OnboardingStep.WELCOME) {
                    OutlinedButton(
                        onClick = onSkipClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip")
                    }
                }

                // Next/Finish button
                Button(
                    onClick = if (currentStep == OnboardingStep.GENERATION) onFinishClick else onNextClick,
                    enabled = canGoNext && !isLoading,
                    modifier = Modifier.weight(2f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Text(
                        when (currentStep) {
                            OnboardingStep.WELCOME -> "Get Started"
                            OnboardingStep.STREAMLINED -> "Continue"
                            OnboardingStep.LEARNING_INTENT -> "Continue"
                            OnboardingStep.PERSONALIZATION -> if (isLoading) "Creating..." else "Create My Path"
                            OnboardingStep.GENERATION -> "Start Learning!"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorOverlay(
    message: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Oops! Something went wrong",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Dismiss")
                    }

                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}
