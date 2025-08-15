package com.dailydevchallenge.devstreaks.features.onboarding

import com.dailydevchallenge.database.UserProfile
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.model.ChallengePathResponse
import com.dailydevchallenge.devstreaks.model.LearningIntent
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.ai.UserContextManager
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.dailydevchallenge.devstreaks.notification.getNotificationScheduler
import com.dailydevchallenge.devstreaks.utils.generateUUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put

data class OnboardingState(
    val currentStep: OnboardingStep = OnboardingStep.WELCOME,
    val learningIntent: LearningIntent? = null,
    val userProfile: UserProfile? = null,
    val challengePath: ChallengePathResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val progress: Float = 0f,
    val canGoNext: Boolean = false,
    val canGoBack: Boolean = false,
    val isPersonalizationComplete: Boolean = false,
    val isStreamlinedComplete: Boolean = false
)

enum class OnboardingStep(val title: String, val stepNumber: Int, val totalSteps: Int = 4) {
    WELCOME("Welcome to DevStreak", 1),
    STREAMLINED("Quick Setup", 1, 1), // New streamlined step
    LEARNING_INTENT("Your Learning Goals", 2),
    PERSONALIZATION("Personalize Experience", 3),
    GENERATION("Creating Your Path", 4)
}

class OnboardingViewModel(
    private val llmService: LLMService,
    private val challengeRepository: ChallengeRepository,
    private val profilePreferences: LearningProfilePreferences,
    private val userContextManager: UserContextManager
) {
    private val viewModelScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            updateState(errorMessage = "Error: ${e.message}")
        }
    )

    private val _uiState = MutableStateFlow(OnboardingState())
    val uiState: StateFlow<OnboardingState> = _uiState.asStateFlow()

    init {
        // Always start fresh onboarding from step 1
        // This ensures users go through the proper flow every time
        updateState(currentStep = OnboardingStep.WELCOME, progress = 0.25f)

        // Pre-load existing user context if available, but don't skip steps
        viewModelScope.launch {
            try {
                val existingIntent = userContextManager.getUserLearningIntent()
                if (existingIntent != null) {
                    // Pre-populate the form but still start from step 1
                    updateState(learningIntent = existingIntent)
                }
            } catch (e: Exception) {
                // Ignore errors and continue with fresh onboarding
            }
        }
    }

    private suspend fun loadExistingUserContext() {
        // This method is no longer needed since we always start from step 1
        // Keeping it for backward compatibility but making it start from step 1
        updateState(currentStep = OnboardingStep.WELCOME, progress = 0.25f)
    }

    fun nextStep() {
        val currentStep = _uiState.value.currentStep
        val nextStep = when (currentStep) {
            OnboardingStep.WELCOME -> OnboardingStep.LEARNING_INTENT
            OnboardingStep.LEARNING_INTENT -> OnboardingStep.PERSONALIZATION
            OnboardingStep.PERSONALIZATION -> OnboardingStep.GENERATION
            OnboardingStep.GENERATION -> return // Already at the end
            OnboardingStep.STREAMLINED -> OnboardingStep.GENERATION // Streamlined path skips to generation
        }

        updateState(
            currentStep = nextStep,
            progress = nextStep.stepNumber / nextStep.totalSteps.toFloat(),
            canGoBack = nextStep != OnboardingStep.WELCOME
        )
    }

    fun previousStep() {
        val currentStep = _uiState.value.currentStep
        val prevStep = when (currentStep) {
            OnboardingStep.WELCOME -> return // Already at the beginning
            OnboardingStep.LEARNING_INTENT -> OnboardingStep.WELCOME
            OnboardingStep.PERSONALIZATION -> OnboardingStep.LEARNING_INTENT
            OnboardingStep.GENERATION -> OnboardingStep.PERSONALIZATION
            OnboardingStep.STREAMLINED -> OnboardingStep.WELCOME // Streamlined path goes back to welcome
        }

        updateState(
            currentStep = prevStep,
            progress = prevStep.stepNumber / prevStep.totalSteps.toFloat(),
            canGoBack = prevStep != OnboardingStep.WELCOME
        )
    }

    fun updateLearningIntent(intent: LearningIntent) {
        updateState(
            learningIntent = intent,
            canGoNext = intent.primaryGoal.isNotBlank() && intent.skillFocus.isNotEmpty()
        )

        // Save to UserContextManager for immediate personalization
        viewModelScope.launch {
            userContextManager.setUserLearningIntent(intent)
        }
    }

    fun generatePersonalizedPath() {
        val intent = _uiState.value.learningIntent ?: return

        viewModelScope.launch {
            updateState(isLoading = true, errorMessage = null)

            try {
                val requestId = generateUUID()
                val challengePath = llmService.generatePlan(
                    goal = intent.primaryGoal,
                    skills = intent.skillFocus,
                    experience = intent.experience,
                    timePerDay = intent.timePerDay,
                    days = 30, // Default 30-day challenge
                    style = intent.learningStyle,
                    fear = intent.fears,
                    requestId = requestId,
                    useOpenAI = true
                )

                // Save the generated path
                challengeRepository.savePathToDb(challengePath)

                // Update user context with personalized preferences
                userContextManager.updateUserPreferences(mapOf(
                    "primaryGoal" to intent.primaryGoal,
                    "skillFocus" to intent.skillFocus.joinToString(","),
                    "careerTrack" to intent.careerTrack,
                    "learningStyle" to intent.learningStyle,
                    "timePerDay" to intent.timePerDay.toString()
                ))

                // Mark onboarding as complete
                UserPreferences.setOnboardingCompleted(true)
                UserPreferences.setPendingRequestId(requestId)

                updateState(
                    challengePath = challengePath,
                    isLoading = false,
                    isPersonalizationComplete = true
                )

            } catch (e: Exception) {
                updateState(
                    isLoading = false,
                    errorMessage = "Failed to generate your learning path: ${e.message}"
                )
            }
        }
    }

    fun retryGeneration() {
        generatePersonalizedPath()
    }

    fun skipStep() {
        nextStep()
    }

    private fun updateState(
        currentStep: OnboardingStep? = null,
        learningIntent: LearningIntent? = null,
        userProfile: UserProfile? = null,
        challengePath: ChallengePathResponse? = null,
        isLoading: Boolean? = null,
        errorMessage: String? = null,
        progress: Float? = null,
        canGoNext: Boolean? = null,
        canGoBack: Boolean? = null,
        isPersonalizationComplete: Boolean? = null,
        isStreamlinedComplete: Boolean? = null
    ) {
        _uiState.update { current ->
            current.copy(
                currentStep = currentStep ?: current.currentStep,
                learningIntent = learningIntent ?: current.learningIntent,
                userProfile = userProfile ?: current.userProfile,
                challengePath = challengePath ?: current.challengePath,
                isLoading = isLoading ?: current.isLoading,
                errorMessage = errorMessage,
                progress = progress ?: current.progress,
                canGoNext = canGoNext ?: current.canGoNext,
                canGoBack = canGoBack ?: current.canGoBack,
                isPersonalizationComplete = isPersonalizationComplete ?: current.isPersonalizationComplete,
                isStreamlinedComplete = isStreamlinedComplete ?: current.isStreamlinedComplete
            )
        }
    }

    fun clearError() {
        updateState(errorMessage = null)
    }

    fun completeStreamlinedOnboarding() {
        val intent = _uiState.value.learningIntent ?: return

        viewModelScope.launch {
            updateState(isLoading = true, errorMessage = null)

            try {
                // Generate a quick personalized path using the streamlined data
                val requestId = generateUUID()
                val challengePath = llmService.generatePlan(
                    goal = intent.primaryGoal,
                    skills = intent.skillFocus,
                    experience = intent.experience,
                    timePerDay = intent.timePerDay,
                    days = 30, // Default 30-day challenge
                    style = "adaptive", // Use adaptive style for streamlined onboarding
                    fear = "", // No fears collected in streamlined version
                    requestId = requestId,
                    useOpenAI = true
                )

                // Save the generated path
                challengeRepository.savePathToDb(challengePath)

                // Update user context
                userContextManager.updateUserPreferences(mapOf(
                    "primaryGoal" to intent.primaryGoal,
                    "skillFocus" to intent.skillFocus.joinToString(","),
                    "experience" to intent.experience,
                    "timePerDay" to intent.timePerDay.toString(),
                    "onboardingType" to "streamlined"
                ))

                // Mark onboarding as complete
                UserPreferences.setOnboardingCompleted(true)
                UserPreferences.setPendingRequestId(requestId)

                updateState(
                    challengePath = challengePath,
                    isLoading = false,
                    isStreamlinedComplete = true
                )

            } catch (e: Exception) {
                updateState(
                    isLoading = false,
                    errorMessage = "Failed to create your learning path: ${e.message}"
                )
            }
        }
    }

    fun startStreamlinedOnboarding() {
        updateState(
            currentStep = OnboardingStep.STREAMLINED,
            progress = 1.0f,
            canGoBack = true
        )
    }

    fun onCleared() {
        viewModelScope.cancel()
    }
}
