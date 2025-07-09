package com.dailydevchallenge.devstreaks.features.onboarding

import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.model.ChallengePathResponse
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.dailydevchallenge.devstreaks.notification.getNotificationScheduler


class OnboardingViewModel(
    private val llmService: LLMService,
    private val challengeRepository: ChallengeRepository,
    private val profilePreferences: LearningProfilePreferences
) {

    data class OnboardingState(
        val challengePath: ChallengePathResponse? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null
    )


    private val viewModelScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, e ->
            println("OnboardingViewModel Error: ${e.message}")
        }
    )
    init {
        viewModelScope.launch {
            if (profilePreferences.isNewChallengeAvailable()) {
                val savedPath = challengeRepository.getSavedChallengePath()
                if (savedPath != null) {
                    updateState(challengePath = savedPath)
                    profilePreferences.setNewChallengeAvailable(false) // mark it as shown
                }
            }
        }
    }



    private val _uiState = MutableStateFlow(OnboardingState())
    val uiState: StateFlow<OnboardingState> = _uiState.asStateFlow()

    suspend fun submitIntent(profile: LearningProfile) {
        if (!validateInput(profile.goal, profile.timePerDay)) return

        updateState(isLoading = true, errorMessage = null)

        try {
            profilePreferences.saveProfile(profile)
            val generated = llmService.generatePlan(
                goal = profile.goal,
                skills = profile.skills,
                experience = profile.experience,
                timePerDay = parseTimeToMinutes(profile.timePerDay),
                days = profile.days.toIntOrNull() ?: 7, // Default to 7 days if not provided
                style = profile.style,
                fear = profile.fear,
                useOpenAI = true // Set to true to use OpenAI, false for Gemini
            )
            challengeRepository.savePathToDb(generated)
            updateState(challengePath = generated, isLoading = false)

            profilePreferences.setNewChallengeAvailable(true)
            getNotificationScheduler().scheduleOneTimeNotification(
                title = "🎓 Your Dev Course is Ready!"+ profile.goal,
                message = "Tap to begin your 7-day challenge journey.",
                type = "course",
            )
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("network", ignoreCase = true) == true -> "Network error. Check your connection."
                e.message?.contains("timeout", ignoreCase = true) == true -> "Request timed out. Try again."
                else -> e.message ?: "Unexpected error occurred"
            }
            updateState(errorMessage = message, isLoading = false)
        }
    }

    fun resetState() {
        updateState(
            challengePath = null,
            isLoading = false,
            errorMessage = null
        )
    }

    private fun validateInput(goal: String, timePerDay: String): Boolean {
        val minutes = when {
            timePerDay.contains("15") -> 15
            timePerDay.contains("30") -> 30
            timePerDay.contains("1 hour", ignoreCase = true) -> 60
            else -> 0
        }

        return when {
            goal.isBlank() -> {
                updateState(errorMessage = "Please enter a learning goal")
                false
            }
            minutes !in 1..180 -> {
                updateState(errorMessage = "Time per day must be between 1 and 180 minutes")
                false
            }
            else -> true
        }
    }



    private fun updateState(
        challengePath: ChallengePathResponse? = _uiState.value.challengePath,
        isLoading: Boolean = _uiState.value.isLoading,
        errorMessage: String? = _uiState.value.errorMessage
    ) {
        _uiState.value = OnboardingState(
            challengePath = challengePath,
            isLoading = isLoading,
            errorMessage = errorMessage
        )
    }
    private fun parseTimeToMinutes(time: String): Int {
        return when {
            time.contains("15") -> 15
            time.contains("30") -> 30
            time.contains("1", ignoreCase = true) && time.contains("hour") -> 60
            time.contains("2", ignoreCase = true) && time.contains("hour") -> 120
            else -> 0
        }
    }


    fun onCleared() {
        viewModelScope.cancel()
    }
}
