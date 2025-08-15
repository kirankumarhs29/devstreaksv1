package com.dailydevchallenge.devstreaks.features.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.service.AdaptiveIntelligenceOrchestrator
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfilePreferences
import com.dailydevchallenge.devstreaks.repository.ProfileRepository
import com.dailydevchallenge.devstreaks.utils.generateUUID
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Adaptive Challenge ViewModel that integrates all Phase 2 features:
 * - Dynamic Difficulty Adjustment
 * - Personalized AI Coaching
 * - Real-time Weak Area Detection
 * - Adaptive XP Scaling
 * - Context-aware Challenge Content
 */
class AdaptiveChallengeViewModel(
    private val adaptiveOrchestrator: AdaptiveIntelligenceOrchestrator,
    private val challengeRepository: ChallengeRepository,
    private val profilePreferences: LearningProfilePreferences,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val logger = { getLogger() }

    // Core state flows
    private val _currentChallenge = MutableStateFlow<ChallengeTask?>(null)
    val currentChallenge = _currentChallenge.asStateFlow()

    private val _adaptiveConfig = MutableStateFlow<AdaptiveChallengeConfig?>(null)
    val adaptiveConfig = _adaptiveConfig.asStateFlow()

    private val _realTimeResponse = MutableStateFlow<RealTimeAdaptiveResponse?>(null)
    val realTimeResponse = _realTimeResponse.asStateFlow()

    private val _personalizedCoaching = MutableStateFlow<PersonalizedCoachingResponse?>(null)
    val personalizedCoaching = _personalizedCoaching.asStateFlow()

    // Challenge session state
    private val _sessionStartTime = MutableStateFlow<Long?>(null)
    private val _currentAttempts = MutableStateFlow(0)
    val currentAttempts = _currentAttempts.asStateFlow()

    private val _hintsUsed = MutableStateFlow(0)
    val hintsUsed = _hintsUsed.asStateFlow()

    private val _errorCount = MutableStateFlow(0)
    val errorCount = _errorCount.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    // XP and progress state
    private val _earnedXP = MutableStateFlow(0)
    val earnedXP = _earnedXP.asStateFlow()

    private val _bonusXP = MutableStateFlow(0)
    val bonusXP = _bonusXP.asStateFlow()

    private val _adaptiveHints = MutableStateFlow<List<String>>(emptyList())
    val adaptiveHints = _adaptiveHints.asStateFlow()

    private val _motivationalMessage = MutableStateFlow<String?>(null)
    val motivationalMessage = _motivationalMessage.asStateFlow()

    // Computed properties
    val totalXP = combine(_earnedXP, _bonusXP) { earned, bonus -> earned + bonus }
    val currentDifficulty = _adaptiveConfig.map { it?.recommendedDifficulty ?: DifficultyLevel.MEDIUM }
    val weakAreas = _adaptiveConfig.map { it?.weakAreas ?: emptyList() }
    val shouldShowInsights = _adaptiveConfig.map { it?.shouldShowPerformanceInsights ?: false }

    /**
     * Initialize adaptive challenge for a user with optional skill focus
     */
    fun initializeAdaptiveChallenge(
        userId: String,
        skillArea: String? = null,
        requestedDifficulty: DifficultyLevel? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                supervisorScope {
                    // Get adaptive configuration
                    val configDeferred = async {
                        adaptiveOrchestrator.initializeAdaptiveChallenge(
                            userId = userId,
                            challengeTask = _currentChallenge.value ?: return@async null,
                            learningProfile = null
                        )
                    }

                    // Get user profile for context
                    val profileDeferred = async {
                        profilePreferences.getProfile() // Fixed method name
                    }

                    val config = configDeferred.await()
                    val profile = profileDeferred.await()

                    _adaptiveConfig.value = config

                    // Load appropriate challenge based on adaptive config
                    if (config != null) {
                        loadAdaptiveChallenge(userId, config, profile)
                    }

                    logger().d("AdaptiveChallenge",
                        "Initialized adaptive challenge for user $userId with difficulty " +
                                "${config?.recommendedDifficulty}")
                }
            } catch (e: Exception) {
                logger().e("AdaptiveChallenge", e, "Error initializing adaptive challenge: ${e.message}")
                // Fallback to standard challenge loading
                loadFallbackChallenge(userId, skillArea)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load challenge optimized for user's adaptive configuration
     */
    private suspend fun loadAdaptiveChallenge(
        userId: String,
        config: AdaptiveChallengeConfig,
        profile: LearningProfile?
    ) {
        // Determine optimal challenge parameters
        val skillPriority = if (config.weakAreas.isNotEmpty()) {
            // Prioritize weak areas for targeted improvement
            config.weakAreas.first().skillArea
        } else {
            // Use focus areas or user preferences
            config.focusAreas.firstOrNull() ?: profile?.skills?.firstOrNull()
        }

        // Get challenge adapted to user's level and focus areas
        val challenge = challengeRepository.getAdaptiveChallenge(
            userId = userId,
            difficulty = config.recommendedDifficulty,
            skillArea = skillPriority,
            excludeCompleted = true
        )

        challenge?.let {
            _currentChallenge.value = it
            _sessionStartTime.value = Clock.System.now().toEpochMilliseconds()

            // Pre-load adaptive hints for efficiency
            _adaptiveHints.value = config.preloadedHints ?: emptyList()


            // Calculate base XP with adaptive multiplier
            val baseXP = it.xp
            _earnedXP.value = (baseXP * config.xpMultiplier).toInt()

            logger().d("AdaptiveChallenge",
                "Loaded challenge: ${it.title}, XP: ${_earnedXP.value} (${config.xpMultiplier}x multiplier)")
        }
    }

    /**
     * Process challenge attempt with real-time adaptive feedback
     */
    fun processAttempt(
        userId: String,
        userAnswer: String,
        isCorrect: Boolean
    ) {
        viewModelScope.launch {
            val challenge = _currentChallenge.value ?: return@launch
            val sessionStart = _sessionStartTime.value ?: Clock.System.now().toEpochMilliseconds()
            val currentTime = Clock.System.now().toEpochMilliseconds()

            // Increment attempt counter
            _currentAttempts.value += 1

            if (!isCorrect) {
                _errorCount.value += 1
            }

            // Create performance metrics for this attempt
            val performanceMetrics = PerformanceMetrics(
                id = generateUUID(),
                userId = userId,
                taskId = challenge.id,
                startTime = sessionStart,
                endTime = currentTime,
                durationMillis = currentTime - sessionStart,
                attempts = _currentAttempts.value,
                hintsUsed = _hintsUsed.value,
                completed = isCorrect,
                errorCount = _errorCount.value,
                difficultyLevel = _adaptiveConfig.value?.recommendedDifficulty ?: DifficultyLevel.MEDIUM
            )

            // Process real-time performance for immediate adaptation
            val realTimeResponse = adaptiveOrchestrator.trackRealTimeProgress(
                userId = userId,
                challengeTask = challenge,
                progressData = mapOf(
                    "attempts" to _currentAttempts.value.toString(),
                    "errorCount" to _errorCount.value.toString(),
                    "hintsUsed" to _hintsUsed.value.toString(),
                    "timeSpent" to (currentTime - sessionStart).toString()
                )
            )

            _realTimeResponse.value = realTimeResponse

            // Update UI state based on real-time response
            realTimeResponse?.let { response ->
                _bonusXP.value += response.bonusXP ?: 0
                _adaptiveHints.value = response.adaptiveHints ?: emptyList()
                _motivationalMessage.value = response.motivationalMessage

                // Handle mid-session difficulty adjustment if needed
                if (response.shouldAdjustDifficulty == true) {
                    handleMidSessionAdjustment(userId, response)
                }
            }

            // If challenge completed successfully, finalize session
            if (isCorrect) {
                finalizeSuccessfulChallenge(userId, performanceMetrics)
            }

            logger().d("AdaptiveChallenge",
                "Processed attempt ${_currentAttempts.value}, Bonus XP: $realTimeResponse")
        }
    }

    /**
     * Handle hint usage with adaptive intelligence
     */
    fun useHint(userId: String) {
        viewModelScope.launch {
            _hintsUsed.value += 1

            val adaptiveHints = _adaptiveHints.value
            val currentHintIndex = _hintsUsed.value - 1

            // Provide adaptive hint if available
            if (currentHintIndex < adaptiveHints.size) {
                val hint = adaptiveHints[currentHintIndex]
                _motivationalMessage.value = "Hint: $hint"
            } else {
                // Generate contextual hint based on current challenge
                val challenge = _currentChallenge.value
                if (challenge != null) {
                    val contextualHint = generateContextualHint(challenge, _errorCount.value)
                    _motivationalMessage.value = contextualHint
                }
            }

            logger().d("AdaptiveChallenge", "Hint used: ${_hintsUsed.value}")
        }
    }

    /**
     * Get personalized coaching response
     */
    fun getPersonalizedCoaching(userMessage: String, userId: String) {
        viewModelScope.launch {
            try {
                val profile = profilePreferences.getProfile()

                val coachingResponse = adaptiveOrchestrator.getPersonalizedCoaching(
                    userId = userId,
                    userMessage = userMessage,
                    contextType = CoachingContextType.CHALLENGE_PROGRESS,
                    learningProfile = profile
                )

                _personalizedCoaching.value = coachingResponse

                logger().d("AdaptiveChallenge", "Generated personalized coaching response")
            } catch (e: Exception) {
                logger().e("AdaptiveChallenge", e, "Error generating coaching: ${e.message}")
            }
        }
    }

    /**
     * Skip current challenge and load next adaptive challenge
     */
    fun skipToNextAdaptiveChallenge(userId: String) {
        viewModelScope.launch {
            val config = _adaptiveConfig.value
            if (config != null) {
                // Reset session state
                resetSessionState()

                // Load next challenge with same adaptive configuration
                val profile = profilePreferences.getProfile()
                loadAdaptiveChallenge(userId, config, profile)
            }
        }
    }

    /**
     * Handle mid-session difficulty adjustment
     */
    private suspend fun handleMidSessionAdjustment(
        userId: String,
        realTimeResponse: RealTimeAdaptiveResponse
    ) {
        logger().d("AdaptiveChallenge", "Triggering mid-session difficulty adjustment")

        // Refresh adaptive configuration by re-initializing
        try {
            val newConfig = adaptiveOrchestrator.initializeAdaptiveChallenge(
                userId = userId,
                challengeTask = _currentChallenge.value!!,
                learningProfile = null
            )
            _adaptiveConfig.value = newConfig

            // Update XP multiplier for current session
            val currentXP = _earnedXP.value
            _earnedXP.value = (currentXP / (_adaptiveConfig.value?.xpMultiplier ?: 1.0) * (newConfig?.xpMultiplier ?: 1.0)).toInt()

            // Provide feedback about adjustment
            _motivationalMessage.value = "Adjusting difficulty to better match your learning pace!"
        } catch (e: Exception) {
            logger().e("AdaptiveChallenge", e, "Error adjusting difficulty: ${e.message}")
        }
    }

    /**
     * Finalize successful challenge completion
     */
    private suspend fun finalizeSuccessfulChallenge(
        userId: String,
        performanceMetrics: PerformanceMetrics
    ) {
        try {
            // Save challenge completion - using existing repository methods
            val totalXPAwarded = _earnedXP.value + _bonusXP.value

            challengeRepository.markTaskCompleted(
                taskId = performanceMetrics.taskId,
                xp = totalXPAwarded,
                userId = userId
            )

            // Save detailed completion record
            val completedChallenge = UserChallengeProgress(
                userId = userId,
                challengeId = performanceMetrics.taskId,
                status = ChallengeStatus.COMPLETED,
                startedAt = Instant.fromEpochMilliseconds(performanceMetrics.startTime),
                completedAt = Instant.fromEpochMilliseconds(performanceMetrics.endTime),
                xpEarned = totalXPAwarded.toLong(),
                attempts = performanceMetrics.attempts.toLong(),
                timeSpentSeconds = performanceMetrics.durationMillis,
                hintUsed = performanceMetrics.hintsUsed > 0
            )
            profileRepository.saveUserChallengeProgress(completedChallenge)
            val completedChallenge1 = CompletedChallenge(pathId = performanceMetrics.taskId,
                completedDate = Clock.System.now().toEpochMilliseconds().toString()
            )

            challengeRepository.saveCompletedChallenge(completedChallenge1)

            // Generate completion message
            _motivationalMessage.value = generateCompletionMessage(performanceMetrics, totalXPAwarded)

            logger().d("AdaptiveChallenge",
                "Challenge completed! Awarded $totalXPAwarded XP (${performanceMetrics.attempts} attempts)")
        } catch (e: Exception) {
            logger().e("AdaptiveChallenge", e, "Error finalizing challenge: ${e.message}")
        }
    }

    /**
     * Reset session state for new challenge
     */
    private fun resetSessionState() {
        _currentAttempts.value = 0
        _hintsUsed.value = 0
        _errorCount.value = 0
        _bonusXP.value = 0
        _sessionStartTime.value = null
        _realTimeResponse.value = null
        _motivationalMessage.value = null
    }

    /**
     * Fallback challenge loading for error cases
     */
    private suspend fun loadFallbackChallenge(userId: String, skillArea: String?) {
        try {
//            val challenge = challengeRepository.getRandomChallenge(skillArea)
//            challenge?.let {
//                _currentChallenge.value = it
//                _earnedXP.value = it.xp
//                _sessionStartTime.value = Clock.System.now().toEpochMilliseconds()
//            }
        } catch (e: Exception) {
            logger().e("AdaptiveChallenge", e, "Error loading fallback challenge: ${e.message}")
        }
    }

    /**
     * Generate contextual hint based on challenge and error patterns
     */
    private fun generateContextualHint(challenge: ChallengeTask, errorCount: Int): String {
        return when {
            errorCount == 0 -> "Take your time to understand the problem"
            errorCount < 3 -> "Consider breaking down the problem into smaller steps"
            errorCount < 5 -> "Review the problem requirements carefully"
            else -> "Try a different approach - sometimes stepping back helps"
        }
    }

    /**
     * Infer skill area from challenge content
     */
    private fun inferSkillArea(challenge: ChallengeTask): String {
        // Simple skill area inference - could be enhanced with ML
        return when {
            challenge.title.contains("algorithm", ignoreCase = true) -> "Algorithms"
            challenge.title.contains("data", ignoreCase = true) -> "Data Structures"
            challenge.title.contains("kotlin", ignoreCase = true) -> "Kotlin"
            challenge.title.contains("design", ignoreCase = true) -> "System Design"
            else -> "Programming"
        }
    }

    /**
     * Generate completion message based on performance
     */
    private fun generateCompletionMessage(metrics: PerformanceMetrics, totalXP: Int): String {
        return when {
            metrics.attempts == 1 -> "Perfect! First try completion! 🎯 (+${totalXP} XP)"
            metrics.attempts <= 3 -> "Excellent work! 💪 (+${totalXP} XP)"
            metrics.attempts <= 5 -> "Great persistence! 🚀 (+${totalXP} XP)"
            else -> "Outstanding determination! 🌟 (+${totalXP} XP)"
        }
    }

    /**
     * Clear current session (for navigation/cleanup)
     */
    fun clearSession() {
        _currentChallenge.value = null
        _adaptiveConfig.value = null
        resetSessionState()
    }
}
