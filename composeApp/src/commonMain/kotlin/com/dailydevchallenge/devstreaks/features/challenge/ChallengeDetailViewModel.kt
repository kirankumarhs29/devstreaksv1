package com.dailydevchallenge.devstreaks.features.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.features.home.UserStatsManager
import com.dailydevchallenge.devstreaks.llm.AIFeedbackService
import com.dailydevchallenge.devstreaks.llm.ChallengeFeedback
import com.dailydevchallenge.devstreaks.model.Challenge
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.service.AdaptiveIntelligenceOrchestrator
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfilePreferences
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.utils.getLogger
import com.dailydevchallenge.devstreaks.utils.generateUUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

// Add ChallengeStep enum if not already present
enum class ChallengeStep { LEARN, DO, COMPLETE }

// UI state for ChallengeDetailScreen - Enhanced with adaptive features
 data class ChallengeDetailUiState(
    val started: Boolean = false,
    val showConfetti: Boolean = false,
    val viewedIds: List<String> = emptyList(),
    val allDone: Boolean = false,
    val isCompleted: Boolean = false,
    val items: List<ActivityPagerItem> = emptyList(),
    val step: ChallengeStep = ChallengeStep.LEARN,
    // Phase 2 adaptive features
    val adaptiveConfig: AdaptiveChallengeConfig? = null,
    val personalizedCoaching: PersonalizedCoachingResponse? = null,
    val difficultyLevel: DifficultyLevel = DifficultyLevel.MEDIUM,
    val adaptiveXpMultiplier: Double = 1.0
)

class ChallengeDetailViewModel(
    private val challengeRepository: ChallengeRepository,
    private val userStatsManager: UserStatsManager,
    private val aiFeedbackService: AIFeedbackService,
    private val adaptiveOrchestrator: AdaptiveIntelligenceOrchestrator,
    private val profilePreferences: LearningProfilePreferences
) : ViewModel() {

    private val logger = { getLogger() }

    private val _currentTask = MutableStateFlow<ChallengeTask?>(null)
    val currentTask: StateFlow<ChallengeTask?> = _currentTask.asStateFlow()

    private val _uiState = MutableStateFlow(ChallengeDetailUiState())
    val uiState: StateFlow<ChallengeDetailUiState> = _uiState.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // AI Feedback state
    private val _aiFeedback = MutableStateFlow<ChallengeFeedback?>(null)
    val aiFeedback: StateFlow<ChallengeFeedback?> = _aiFeedback.asStateFlow()

    private val _isGeneratingFeedback = MutableStateFlow(false)
    val isGeneratingFeedback: StateFlow<Boolean> = _isGeneratingFeedback.asStateFlow()

    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog: StateFlow<Boolean> = _showFeedbackDialog.asStateFlow()

    private val _completedActivities = MutableStateFlow<Set<String>>(emptySet())

    // Phase 2 adaptive intelligence integration
    private val _adaptiveConfig = MutableStateFlow<AdaptiveChallengeConfig?>(null)
    val adaptiveConfig = _adaptiveConfig.asStateFlow()

    private val _personalizedCoaching = MutableStateFlow<PersonalizedCoachingResponse?>(null)
    val personalizedCoaching = _personalizedCoaching.asStateFlow()

    private val _realTimeResponse = MutableStateFlow<RealTimeAdaptiveResponse?>(null)
    val realTimeResponse = _realTimeResponse.asStateFlow()

    // Session tracking for adaptive intelligence
    private val _sessionStartTime = MutableStateFlow<Long?>(null)
    private val _currentAttempts = MutableStateFlow(0)
    private val _hintsUsed = MutableStateFlow(0)
    private val _errorCount = MutableStateFlow(0)

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val task = challengeRepository.getTaskById(taskId)
                _currentTask.value = task

                val userId = UserPreferences.getSafeUserId()
                val isTaskCompleted = challengeRepository.isTaskCompleted(taskId, userId)
                _isCompleted.value = isTaskCompleted

                // Synchronize the UI state with the completion status
                if (isTaskCompleted) {
                    _uiState.value = _uiState.value.copy(
                        started = true,
                        isCompleted = true,
                        allDone = true,
                        step = ChallengeStep.COMPLETE
                    )
                } else {
                    // Reset to initial state for uncompleted tasks
                    _uiState.value = _uiState.value.copy(
                        started = false,
                        isCompleted = false,
                        allDone = false,
                        showConfetti = false,
                        viewedIds = emptyList(),
                        step = ChallengeStep.LEARN
                    )
                }

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to load task: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun completeChallenge(challenge: Challenge, userResponse: String, isCorrect: Boolean, completionTimeMs: Long = 0) {
        val task = _currentTask.value ?: return
        val userId = UserPreferences.getSafeUserId()

        viewModelScope.launch {
            try {
                // Mark task as completed in repository
                challengeRepository.markTaskCompleted(task.id, task.xp, userId)

                // Update user stats through the stats manager
                userStatsManager.onChallengeCompleted(task.pathId, task.day, task.xp)

                _isCompleted.value = true

                // Generate AI feedback for the completed challenge
                generateAIFeedback(challenge, userResponse, isCorrect, completionTimeMs)

                _error.value = null

            } catch (e: Exception) {
                _error.value = "Failed to complete challenge: ${e.message}"
            }
        }
    }

    private fun generateAIFeedback(
        challenge: Challenge,
        userResponse: String,
        isCorrect: Boolean,
        completionTimeMs: Long
    ) {
        viewModelScope.launch {
            try {
                _isGeneratingFeedback.value = true
                _showFeedbackDialog.value = true

                val feedbackResult = aiFeedbackService.generateChallengeFeedback(
                    challenge = challenge,
                    userResponse = userResponse,
                    isCorrect = isCorrect,
                    completionTime = completionTimeMs
                )

                feedbackResult.fold(
                    onSuccess = { feedback ->
                        _aiFeedback.value = feedback
                        logger().d("AI feedback generated successfully")
                    },
                    onFailure = { error ->
                        logger().e("Failed to generate AI feedback", error)
                        // Provide fallback feedback
                        _aiFeedback.value = createFallbackFeedback(isCorrect)
                    }
                )
            } finally {
                _isGeneratingFeedback.value = false
            }
        }
    }

    private fun createFallbackFeedback(isCorrect: Boolean): ChallengeFeedback {
        return ChallengeFeedback(
            score = if (isCorrect) 85 else 60,
            strengths = if (isCorrect) listOf("Correct answer!", "Good problem-solving approach")
                       else listOf("You attempted the challenge", "Learning from mistakes is valuable"),
            improvements = if (isCorrect) listOf("Try to optimize your solution", "Consider edge cases")
                          else listOf("Review the concept", "Practice similar problems"),
            personalizedTips = listOf("Keep practicing daily", "Focus on understanding the fundamentals"),
            nextSteps = listOf("Continue with the next challenge", "Review related topics"),
            encouragement = if (isCorrect) "Excellent work! Keep building your streak! 🚀"
                           else "Great effort! Every challenge makes you stronger! 💪"
        )
    }

    fun clearError() {
        _error.value = null
    }

    fun startTask() {
        logger().d("Task started for day ${currentTask.value?.day}")
        val task = _currentTask.value
        if (task != null) {
            val items = buildPagerItems(task)
            _uiState.value = _uiState.value.copy(
                started = true,
                step = ChallengeStep.DO,
                items = items
            )
        }
    }

    private fun buildPagerItems(task: ChallengeTask): List<ActivityPagerItem> {
        val items = mutableListOf<ActivityPagerItem>()

        // Add learn phase items
        items.add(ActivityPagerItem(
            id = "${task.id}_learn",
            type = "learn",
            content = task.content
        ))

        // Add challenge activities
        task.challenges.forEachIndexed { index, challenge ->
            items.add(ActivityPagerItem(
                id = "${task.id}_challenge_$index",
                type = challenge.type.toString(),
                content = challenge.prompt,
                challenge = challenge
            ))
        }

        // Add completion item
        items.add(ActivityPagerItem(
            id = "${task.id}_complete",
            type = "complete",
            content = "Challenge completed!"
        ))

        return items
    }

    fun goToLearn() {
        _uiState.value = _uiState.value.copy(step = ChallengeStep.LEARN)
    }

    fun goToDo() {
        _uiState.value = _uiState.value.copy(step = ChallengeStep.DO)
    }

    fun goToComplete() {
        _uiState.value = _uiState.value.copy(step = ChallengeStep.COMPLETE)
    }

    fun onActivityCompleted(activityId: String, isCorrect: Boolean) {
        if (isCorrect) {
            val newCompleted = _completedActivities.value + activityId
            _completedActivities.value = newCompleted

            val totalActivities = currentTask.value?.challenges?.size ?: 0
            val allCompleted = newCompleted.size >= totalActivities

            _uiState.value = _uiState.value.copy(
                viewedIds = _uiState.value.viewedIds + activityId,
                allDone = allCompleted
            )

            if (allCompleted) {
                onAllCompleted()
            }
        }
    }

    fun onActivityViewed(id: String) {
        val newViewed = _uiState.value.viewedIds + id
        _uiState.value = _uiState.value.copy(viewedIds = newViewed)
    }

    fun onAllCompleted() {
        logger().d("All activities completed for day ${currentTask.value?.day}")
        val task = _currentTask.value ?: return
        val userId = UserPreferences.getSafeUserId()

        viewModelScope.launch {
            try {
                // Single point of completion - handles both repository and stats
                challengeRepository.markTaskCompleted(task.id, task.xp, userId)
                userStatsManager.onChallengeCompleted(task.pathId, task.day, task.xp)

                // Update UI state
                _uiState.value = _uiState.value.copy(
                    showConfetti = true,
                    isCompleted = true,
                    step = ChallengeStep.COMPLETE
                )
                _isCompleted.value = true

            } catch (e: Exception) {
                logger().e("Failed to complete challenge: ${e.message}")
            }
        }
    }

    fun dismissConfetti() {
        _uiState.value = _uiState.value.copy(showConfetti = false)
    }

    fun dismissFeedbackDialog() {
        _showFeedbackDialog.value = false
        _aiFeedback.value = null
    }

    init {
        // Load initial adaptive config and coaching tips
        loadAdaptiveConfig()
    }

    private fun loadAdaptiveConfig() {
        viewModelScope.launch {
            try {
                val userId = UserPreferences.getSafeUserId()
                val currentTask = _currentTask.value

                if (currentTask != null) {
                    // Use the correct method name from AdaptiveIntelligenceOrchestrator
                    val config = adaptiveOrchestrator.initializeAdaptiveChallenge(
                        userId = userId,
                        challengeTask = currentTask,
                        learningProfile = null // Will be enhanced later
                    )

                    _uiState.value = _uiState.value.copy(adaptiveConfig = config)

                    // Log the adaptive configuration for debugging
                    logger().d("Adaptive config loaded: $config")
                }

            } catch (e: Exception) {
                logger().e("Failed to load adaptive config: ${e.message}")
            }
        }
    }

    // Call this method to refresh the adaptive config, e.g., after completing a challenge
    fun refreshAdaptiveConfig() {
        loadAdaptiveConfig()
    }

    /**
     * Initialize adaptive intelligence for the current challenge
     * Integrates dynamic difficulty adjustment and personalized coaching
     */
    fun initializeAdaptiveIntelligence(
        userId: String,
        challengeTask: ChallengeTask,
        skillArea: String? = null
    ) {
        viewModelScope.launch {
            try {
                _sessionStartTime.value = Clock.System.now().toEpochMilliseconds()

                // Get user's learning profile for context
                val profile = profilePreferences.getProfile()

                // Initialize adaptive challenge configuration
                val config = adaptiveOrchestrator.initializeAdaptiveChallenge(
                    userId = userId,
                    challengeTask = challengeTask,
                    learningProfile = profile
                )

                _adaptiveConfig.value = config

                // Update UI state with adaptive configuration
                _uiState.value = _uiState.value.copy(
                    adaptiveConfig = config,
                    difficultyLevel = config?.recommendedDifficulty ?: DifficultyLevel.MEDIUM,
                    adaptiveXpMultiplier = config?.adaptiveXpMultiplier ?: 1.0
                )

                // Get initial personalized coaching
                getPersonalizedCoaching(userId, challengeTask)

                logger().d("ChallengeDetail",
                    "Initialized adaptive intelligence for task ${challengeTask.id} with difficulty ${config?.recommendedDifficulty}")

            } catch (e: Exception) {
                logger().e("ChallengeDetail", e, "Error initializing adaptive intelligence: ${e.message}")
                // Fallback to default configuration
                _adaptiveConfig.value = null
            }
        }
    }

    /**
     * Get personalized coaching based on user's weak areas and current challenge
     */
    private fun getPersonalizedCoaching(userId: String, challengeTask: ChallengeTask) {
        viewModelScope.launch {
            try {
                val profile = profilePreferences.getProfile()
                val coaching = adaptiveOrchestrator.getPersonalizedCoaching(
                    userId = userId,
                    userMessage = "Challenge guidance needed for ${challengeTask.skill ?: "general"} challenge",
                    contextType = CoachingContextType.CHALLENGE_GUIDANCE,
                    learningProfile = profile
                )

                _personalizedCoaching.value = coaching

                // Update UI state with coaching context
                _uiState.value = _uiState.value.copy(
                    personalizedCoaching = coaching
                )

            } catch (e: Exception) {
                logger().e("ChallengeDetail", e, "Error getting personalized coaching: ${e.message}")
            }
        }
    }

    /**
     * Track learning phase completion for adaptive intelligence
     */
    fun trackLearningPhaseCompletion() {
        viewModelScope.launch {
            try {
                val task = _currentTask.value ?: return@launch
                val userId = UserPreferences.getSafeUserId()

                // Track real-time progress for adaptive adjustments
                val response = adaptiveOrchestrator.trackRealTimeProgress(
                    userId = userId,
                    challengeTask = task,
                    progressData = mapOf(
                        "phase" to "learning_completed",
                        "timestamp" to Clock.System.now().toEpochMilliseconds().toString()
                    )
                )

                _realTimeResponse.value = response

                // Update personalized coaching based on learning phase completion
                if (response?.shouldUpdateCoaching == true) {
                    getPersonalizedCoaching(userId, task)
                }

            } catch (e: Exception) {
                logger().e("ChallengeDetail", e, "Error tracking learning phase completion: ${e.message}")
            }
        }
    }

    /**
     * Complete challenge with adaptive intelligence tracking
     */
    fun completeAdaptiveChallenge() {
        viewModelScope.launch {
            try {
                val task = _currentTask.value ?: return@launch
                val config = _adaptiveConfig.value
                val userId = UserPreferences.getSafeUserId()

                // Calculate performance metrics for adaptive intelligence
                val completionTime = Clock.System.now().toEpochMilliseconds() -
                    (_sessionStartTime.value ?: Clock.System.now().toEpochMilliseconds())

                val metrics = PerformanceMetrics(
                    id = generateUUID(),
                    userId = userId,
                    taskId = task.id,
                    startTime = _sessionStartTime.value ?: Clock.System.now().toEpochMilliseconds(),
                    endTime = Clock.System.now().toEpochMilliseconds(),
                    durationMillis = completionTime,
                    attempts = _currentAttempts.value,
                    hintsUsed = _hintsUsed.value,
                    completed = true,
                    errorCount = _errorCount.value,
                    difficultyLevel = config?.recommendedDifficulty ?: DifficultyLevel.MEDIUM,
                    timestamp = Clock.System.now().toEpochMilliseconds()
                )

                // Save performance metrics for future adaptive adjustments
                challengeRepository.savePerformanceMetrics(metrics)

                // Calculate adaptive XP with multiplier
                val baseXP = task.xp
                val adaptiveMultiplier = config?.adaptiveXpMultiplier ?: 1.0
                val totalXP = (baseXP * adaptiveMultiplier).toInt()

                // Complete challenge with adaptive XP
                challengeRepository.markTaskCompleted(task.id, totalXP, userId)
                userStatsManager.onChallengeCompleted(task.pathId, task.day, totalXP)

                _isCompleted.value = true

                // Update adaptive intelligence with completion data
                adaptiveOrchestrator.trackRealTimeProgress(
                    userId = userId,
                    challengeTask = task,
                    progressData = mapOf(
                        "phase" to "challenge_completed",
                        "completion_time_ms" to completionTime.toString(),
                        "adaptive_xp_earned" to totalXP.toString(),
                        "difficulty_level" to (config?.recommendedDifficulty?.name ?: "MEDIUM")
                    )
                )

                logger().d("ChallengeDetail",
                    "Completed adaptive challenge ${task.id} with ${totalXP}XP (${adaptiveMultiplier}x multiplier)")

            } catch (e: Exception) {
                logger().e("ChallengeDetail", e, "Error completing adaptive challenge: ${e.message}")
                _error.value = "Failed to complete adaptive challenge: ${e.message}"
            }
        }
    }
}
