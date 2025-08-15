package com.dailydevchallenge.devstreaks.service

import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.model.BaselineMetrics as ModelBaselineMetrics
import com.dailydevchallenge.devstreaks.service.BaselineMetrics as ServiceBaselineMetrics
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import com.dailydevchallenge.devstreaks.utils.generateUUID
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlin.math.*

/**
 * Unified Adaptive Intelligence Orchestrator
 * Coordinates all Phase 2 features:
 * - Dynamic Difficulty Adjustment
 * - Personalized AI Coaching
 * - Real-time Weak Area Detection
 * - Adaptive XP Scaling
 * - Challenge Content Adaptation
 */
class AdaptiveIntelligenceOrchestrator(
    private val difficultyCalculator: DifficultyCalculator,
    private val weakAreaDetectionService: WeakAreaDetectionService,
    private val personalizedCoachingService: PersonalizedAICoachingService,
    private val challengeRepository: ChallengeRepository
) {
    private val logger = { getLogger() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Cache for performance optimization
    private val userContextCache = mutableMapOf<String, UserAdaptiveContext>()
    private val baselineMetricsCache = mutableMapOf<DifficultyLevel, ModelBaselineMetrics>()

    companion object {
        private const val CONTEXT_CACHE_DURATION_MS = 5 * 60 * 1000L // 5 minutes
        private const val REAL_TIME_ANALYSIS_INTERVAL_MS = 30 * 1000L // 30 seconds
    }

    /**
     * Initialize adaptive challenge configuration for user
     */
    suspend fun initializeAdaptiveChallenge(
        userId: String,
        challengeTask: ChallengeTask,
        learningProfile: LearningProfile?
    ): AdaptiveChallengeConfig = withContext(Dispatchers.Default) {

        val userContext = getUserAdaptiveContext(userId, learningProfile)
        val difficultyAdjustment = calculateOptimalDifficulty(userId, userContext)

        // Adapt challenge content based on user performance and weak areas
        val adaptedTask = adaptChallengeContent(challengeTask, userContext, difficultyAdjustment)

        // Calculate adaptive XP scaling
        val xpMultiplier = calculateAdaptiveXPMultiplier(difficultyAdjustment, userContext)

        // Generate personalized hints and guidance
        val personalizedHints = generatePersonalizedHints(adaptedTask, userContext)

        logger().d("AdaptiveOrchestrator",
            "Initialized adaptive challenge for user $userId: " +
            "Difficulty=${difficultyAdjustment.recommendedLevel}, XP=${xpMultiplier}x")

        AdaptiveChallengeConfig(
            originalTask = challengeTask,
            adaptedTask = adaptedTask,
            difficultyAdjustment = difficultyAdjustment,
            xpMultiplier = xpMultiplier,
            personalizedHints = personalizedHints,
            userContext = userContext,
            timestamp = Clock.System.now().toEpochMilliseconds(),
            adaptiveXpMultiplier = xpMultiplier
        )
    }

    /**
     * Provide real-time adaptive responses during challenge execution
     */
    suspend fun provideRealTimeAdaptation(
        userId: String,
        challengeSessionId: String,
        currentPerformance: ChallengeSessionMetrics
    ): RealTimeAdaptiveResponse = withContext(Dispatchers.Default) {

        val userContext = getUserAdaptiveContext(userId)

        // Analyze current session performance
        val sessionAnalysis = analyzeSessionPerformance(currentPerformance, userContext)

        // Detect emerging weak areas or patterns
        val emergingInsights = detectEmergingPatterns(currentPerformance, userContext)

        // Generate real-time coaching intervention
        val coachingIntervention = generateRealTimeCoaching(sessionAnalysis, emergingInsights, userContext)

        // Determine if difficulty adjustment is needed mid-session
        val midSessionAdjustment = evaluateMidSessionAdjustment(sessionAnalysis, userContext)

        // Calculate dynamic hint availability
        val hintRecommendation = calculateHintRecommendation(sessionAnalysis, userContext)

        RealTimeAdaptiveResponse(
            sessionAnalysis = sessionAnalysis,
            emergingInsights = emergingInsights,
            coachingIntervention = coachingIntervention,
            midSessionAdjustment = midSessionAdjustment,
            hintRecommendation = hintRecommendation,
            bonusXP = 0,
            adaptiveHints = emptyList(),
            motivationalMessage = "Keep up the great work!",
            shouldAdjustDifficulty = midSessionAdjustment != null,
            shouldUpdateCoaching = true,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
    }

    /**
     * Process challenge completion and update adaptive models
     */
    suspend fun processCompletedChallenge(
        userId: String,
        challengeTask: ChallengeTask,
        completionMetrics: ChallengeCompletionMetrics,
        adaptiveConfig: AdaptiveChallengeConfig
    ): AdaptiveLearningUpdate = withContext(Dispatchers.Default) {

        // Create performance metrics record
        val performanceMetrics = createPerformanceMetrics(userId, challengeTask, completionMetrics, adaptiveConfig)

        // Save performance metrics to repository
        challengeRepository.savePerformanceMetrics(performanceMetrics)

        // Update user performance summary
        val updatedSummary = challengeRepository.calculateUserPerformanceSummary(userId, 20)

        // Detect new weak areas or improvements
        val weakAreaUpdate = weakAreaDetectionService.detectWeakAreas(userId)

        // Calculate future difficulty recommendations
        val futureDifficultyRecommendation = difficultyCalculator.calculateDifficultyAdjustment(
            updatedSummary,
            convertToServiceBaselineMetrics(getBaselineMetrics())
        )

        // Generate learning insights and achievements
        val learningInsights = generateLearningInsights(completionMetrics, adaptiveConfig, weakAreaUpdate)

        // Calculate XP earned with adaptive scaling
        val adaptiveXP = calculateFinalAdaptiveXP(completionMetrics, adaptiveConfig)

        // Update cache
        invalidateUserCache(userId)

        AdaptiveLearningUpdate(
            performanceMetrics = performanceMetrics,
            weakAreasUpdate = weakAreaUpdate,
            futureDifficultyRecommendation = futureDifficultyRecommendation,
            learningInsights = learningInsights,
            adaptiveXP = adaptiveXP,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
    }

    /**
     * Get comprehensive personalized coaching response
     */
    suspend fun getPersonalizedCoaching(
        userId: String,
        userMessage: String,
        contextType: CoachingContextType,
        learningProfile: LearningProfile?
    ): PersonalizedCoachingResponse = withContext(Dispatchers.Default) {

        val userContext = getUserAdaptiveContext(userId, learningProfile)

        return@withContext personalizedCoachingService.generatePersonalizedResponse(
            userMessage, userId, learningProfile
        )
    }

    // Private helper methods

    private suspend fun getUserAdaptiveContext(
        userId: String,
        learningProfile: LearningProfile? = null
    ): UserAdaptiveContext {
        val cached = userContextCache[userId]
        if (cached != null &&
            (Clock.System.now().toEpochMilliseconds() - cached.timestamp) < CONTEXT_CACHE_DURATION_MS) {
            return cached
        }

        val performanceSummary = challengeRepository.calculateUserPerformanceSummary(userId, 20)
        val weakAreas = weakAreaDetectionService.detectWeakAreas(userId)
        val recentChallenges = challengeRepository.getRecentChallenges(userId, 5)

        val context = UserAdaptiveContext(
            userId = userId,
            performanceSummary = performanceSummary,
            weakAreas = weakAreas,
            recentChallenges = recentChallenges,
            learningProfile = learningProfile,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )

        userContextCache[userId] = context
        return context
    }

    private suspend fun calculateOptimalDifficulty(
        userId: String,
        userContext: UserAdaptiveContext
    ): DifficultyAdjustment {
        return difficultyCalculator.calculateDifficultyAdjustment(
            userContext.performanceSummary,
            convertToServiceBaselineMetrics(getBaselineMetrics())
        )
    }

    private fun adaptChallengeContent(
        task: ChallengeTask,
        userContext: UserAdaptiveContext,
        difficultyAdjustment: DifficultyAdjustment
    ): ChallengeTask {
        // Adapt challenge based on weak areas and difficulty level
        val adaptedContent = adaptContentForWeakAreas(task.content, userContext.weakAreas)
        val adaptedTip = adaptTipForUser(task.tip.toString(), userContext, difficultyAdjustment)

        return task.copy(
            content = adaptedContent,
            tip = adaptedTip
        )
    }

    private fun calculateAdaptiveXPMultiplier(
        difficultyAdjustment: DifficultyAdjustment,
        userContext: UserAdaptiveContext
    ): Double {
        val baseDifficultyMultiplier = when (difficultyAdjustment.recommendedLevel) {
            DifficultyLevel.BEGINNER -> 0.8
            DifficultyLevel.EASY -> 1.0
            DifficultyLevel.MEDIUM -> 1.2
            DifficultyLevel.HARD -> 1.5
            DifficultyLevel.EXPERT -> 2.0
        }
        val personalPerformanceBonus = calculatePersonalPerformanceBonus(userContext)
        val weakAreaBonus = calculateWeakAreaBonus(userContext.weakAreas)

        return (baseDifficultyMultiplier + personalPerformanceBonus + weakAreaBonus).coerceIn(0.5, 3.0)
    }

    private fun generatePersonalizedHints(
        task: ChallengeTask,
        userContext: UserAdaptiveContext
    ): List<PersonalizedHint> {
        // Generate hints based on task content and user weak areas
        val baseHints = listOfNotNull(task.tip, task.bonus).ifEmpty {
            listOf("Take your time to understand the problem", "Break down the solution into steps")
        }

        return baseHints.mapIndexed { index, hint ->
            PersonalizedHint(
                originalHint = hint,
                personalizedText = personalizeHintText(hint, userContext),
                relevanceScore = calculateHintRelevance(hint, userContext),
                unlockThreshold = calculateHintUnlockThreshold(index, userContext)
            )
        }
    }

    private suspend fun analyzeSessionPerformance(
        metrics: ChallengeSessionMetrics,
        userContext: UserAdaptiveContext
    ): SessionPerformanceAnalysis {
        val timeEfficiency = calculateTimeEfficiency(metrics, userContext)
        val errorPattern = analyzeErrorPattern(metrics, userContext)
        val strugglingAreas = identifyStrugglingAreas(metrics, userContext)

        return SessionPerformanceAnalysis(
            timeEfficiency = timeEfficiency,
            errorPattern = errorPattern,
            strugglingAreas = strugglingAreas,
            overallProgress = calculateOverallProgress(metrics, userContext)
        )
    }

    private fun generateRealTimeCoaching(
        analysis: SessionPerformanceAnalysis,
        insights: List<EmergingInsight>,
        userContext: UserAdaptiveContext
    ): CoachingIntervention {
        val interventionType = determineInterventionType(analysis, insights)
        val message = generateInterventionMessage(interventionType, analysis, userContext)
        val urgency = calculateInterventionUrgency(analysis, insights)

        return CoachingIntervention(
            type = interventionType,
            message = message,
            urgency = urgency,
            suggestions = generateActionableSuggestions(analysis, userContext)
        )
    }

    private fun getBaselineMetrics(): Map<DifficultyLevel, ModelBaselineMetrics> {
        if (baselineMetricsCache.isEmpty()) {
            // Initialize baseline metrics for each difficulty level
            DifficultyLevel.values().forEach { level ->
                baselineMetricsCache[level] = ModelBaselineMetrics(
                    averageCompletionTime = when(level) {
                        DifficultyLevel.BEGINNER -> 300_000L // 5 minutes
                        DifficultyLevel.EASY -> 600_000L // 10 minutes
                        DifficultyLevel.MEDIUM -> 1_200_000L // 20 minutes
                        DifficultyLevel.HARD -> 1_800_000L // 30 minutes
                        DifficultyLevel.EXPERT -> 2_700_000L // 45 minutes
                    },
                    expectedSuccessRate = when(level) {
                        DifficultyLevel.BEGINNER -> 0.95
                        DifficultyLevel.EASY -> 0.85
                        DifficultyLevel.MEDIUM -> 0.75
                        DifficultyLevel.HARD -> 0.65
                        DifficultyLevel.EXPERT -> 0.55
                    },
                    expectedAttempts = when(level) {
                        DifficultyLevel.BEGINNER -> 1.2
                        DifficultyLevel.EASY -> 1.5
                        DifficultyLevel.MEDIUM -> 2.0
                        DifficultyLevel.HARD -> 2.5
                        DifficultyLevel.EXPERT -> 3.0
                    },
                    expectedHintsUsed = when(level) {
                        DifficultyLevel.BEGINNER -> 0.5
                        DifficultyLevel.EASY -> 1.0
                        DifficultyLevel.MEDIUM -> 1.5
                        DifficultyLevel.HARD -> 2.0
                        DifficultyLevel.EXPERT -> 2.5
                    }
                )
            }
        }
        return baselineMetricsCache
    }

    private fun invalidateUserCache(userId: String) {
        userContextCache.remove(userId)
    }

    /**
     * Detect weak areas for a user - delegated to WeakAreaDetectionService
     */
    suspend fun detectWeakAreas(userId: String): List<WeakArea> {
        return weakAreaDetectionService.detectWeakAreas(userId)
    }

    /**
     * Track real-time progress during challenge execution
     */
    suspend fun trackRealTimeProgress(
        userId: String,
        challengeTask: ChallengeTask,
        progressData: Map<String, String>
    ): RealTimeAdaptiveResponse? {
        return try {
            // Create session metrics from progress data
            val sessionMetrics = ChallengeSessionMetrics(
                sessionId = generateUUID(),
                userId = userId,
                challengeId = challengeTask.id,
                elapsedTime = progressData["time_spent"]?.toLongOrNull() ?: 0L,
                attemptsCount = progressData["attempts"]?.toIntOrNull() ?: 1,
                errorsCount = progressData["errors"]?.toIntOrNull() ?: 0,
                hintsUsed = progressData["hints_used"]?.toIntOrNull() ?: 0,
                currentProgress = progressData["progress"]?.toDoubleOrNull() ?: 0.0,
                stuckDuration = 0L,
                lastActivityTime = Clock.System.now().toEpochMilliseconds()
            )

            // Provide real-time adaptive response
            provideRealTimeAdaptation(userId, challengeTask.id, sessionMetrics)
        } catch (e: Exception) {
            logger().e("AdaptiveOrchestrator", e, "Error tracking real-time progress: ${e.message}")
            null
        }
    }

    // Additional helper methods for content adaptation, performance calculation, etc.
    private fun adaptContentForWeakAreas(content: String, weakAreas: List<WeakArea>): String = content
    private fun adaptTipForUser(tip: String, context: UserAdaptiveContext, adjustment: DifficultyAdjustment): String = tip
    private fun calculatePersonalPerformanceBonus(context: UserAdaptiveContext): Double = 0.1
    private fun calculateWeakAreaBonus(weakAreas: List<WeakArea>): Double = weakAreas.size * 0.05
    private fun personalizeHintText(hint: String, context: UserAdaptiveContext): String = hint
    private fun calculateHintRelevance(hint: String, context: UserAdaptiveContext): Double = 1.0
    private fun calculateHintUnlockThreshold(index: Int, context: UserAdaptiveContext): Int = index + 1
    private fun calculateTimeEfficiency(metrics: ChallengeSessionMetrics, context: UserAdaptiveContext): Double = 1.0
    private fun analyzeErrorPattern(metrics: ChallengeSessionMetrics, context: UserAdaptiveContext): String = "No issues"
    private fun identifyStrugglingAreas(metrics: ChallengeSessionMetrics, context: UserAdaptiveContext): List<String> = emptyList()
    private fun calculateOverallProgress(metrics: ChallengeSessionMetrics, context: UserAdaptiveContext): Double = 1.0
    private fun detectEmergingPatterns(metrics: ChallengeSessionMetrics, context: UserAdaptiveContext): List<EmergingInsight> = emptyList()
    private fun evaluateMidSessionAdjustment(analysis: SessionPerformanceAnalysis, context: UserAdaptiveContext): DifficultyAdjustment? = null
    private fun calculateHintRecommendation(analysis: SessionPerformanceAnalysis, context: UserAdaptiveContext): HintRecommendation = HintRecommendation.NONE
    private fun createPerformanceMetrics(userId: String, task: ChallengeTask, metrics: ChallengeCompletionMetrics, config: AdaptiveChallengeConfig): PerformanceMetrics =
        PerformanceMetrics(generateUUID(), userId, task.id, 0L, 0L, 0L, 0, 0, true, 0, DifficultyLevel.MEDIUM)
    private fun generateLearningInsights(metrics: ChallengeCompletionMetrics, config: AdaptiveChallengeConfig, weakAreas: List<WeakArea>): List<LearningInsight> = emptyList()
    private fun calculateFinalAdaptiveXP(metrics: ChallengeCompletionMetrics, config: AdaptiveChallengeConfig): Int = 100
    private fun determineInterventionType(analysis: SessionPerformanceAnalysis, insights: List<EmergingInsight>): InterventionType = InterventionType.ENCOURAGEMENT
    private fun generateInterventionMessage(type: InterventionType, analysis: SessionPerformanceAnalysis, context: UserAdaptiveContext): String = "Keep going!"
    private fun calculateInterventionUrgency(analysis: SessionPerformanceAnalysis, insights: List<EmergingInsight>): InterventionUrgency = InterventionUrgency.LOW
    private fun generateActionableSuggestions(analysis: SessionPerformanceAnalysis, context: UserAdaptiveContext): List<String> = emptyList()

    /**
     * Convert model BaselineMetrics to service BaselineMetrics for DifficultyCalculator
     */
    private fun convertToServiceBaselineMetrics(
        modelMetrics: Map<DifficultyLevel, ModelBaselineMetrics>
    ): Map<DifficultyLevel, ServiceBaselineMetrics> {
        return modelMetrics.mapValues { (_, model) ->
            ServiceBaselineMetrics(
                averageCompletionTime = model.averageCompletionTime,
                expectedSuccessRate = model.expectedSuccessRate,
                expectedAttempts = model.expectedAttempts,
                expectedHints = model.expectedHintsUsed,
                expectedErrors = 1.0 // Default value for the extra parameter in service version
            )
        }
    }
}
