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
            overallProgress = calculateOverallProgress(metrics, userContext),
            errorCount = metrics.errorsCount,
            hintsUsed = metrics.hintsUsed,
            stuckDuration = metrics.stuckDuration
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
    private fun adaptContentForWeakAreas(content: String, weakAreas: List<WeakArea>): String {
        if (weakAreas.isEmpty()) return content
        
        val primaryWeakArea = weakAreas.first()
        return when (primaryWeakArea.skillArea.lowercase()) {
            "arrays" -> "$content\n\n💡 Focus on array indexing and bounds checking."
            "recursion" -> "$content\n\n💡 Remember to identify the base case first."
            "debugging" -> "$content\n\n💡 Use systematic debugging: check inputs, trace execution, verify outputs."
            else -> "$content\n\n💡 Break this problem into smaller, manageable steps."
        }
    }
    
    private fun adaptTipForUser(tip: String, context: UserAdaptiveContext, adjustment: DifficultyAdjustment): String {
        val difficultyPrefix = when (adjustment.recommendedLevel) {
            DifficultyLevel.BEGINNER -> "🌱 Beginner tip: "
            DifficultyLevel.EASY -> "🚀 Easy insight: "
            DifficultyLevel.MEDIUM -> "🚀 Intermediate insight: "
            DifficultyLevel.HARD -> "🎯 Advanced strategy: "
            DifficultyLevel.EXPERT -> "🏆 Expert technique: "
            else -> "💡 "
        }
        return "$difficultyPrefix$tip"
    }
    
    private fun calculatePersonalPerformanceBonus(context: UserAdaptiveContext): Double {
        val baseBonus = 0.1
        val streakBonus = minOf(0.2, context.learningStreak * 0.01)
        val performanceBonus = if (context.recentPerformance.successRate > 0.8) 0.15 else 0.0
        return baseBonus + streakBonus + performanceBonus
    }
    
    private fun calculateWeakAreaBonus(weakAreas: List<WeakArea>): Double {
        return weakAreas.sumOf { weakArea ->
            when (weakArea.severityScore) {
                in 0.8..1.0 -> 0.15 // Severe weak area
                in 0.6..0.8 -> 0.10 // Moderate weak area
                else -> 0.05 // Minor weak area
            }
        }
    }
    
    private fun personalizeHintText(hint: String, context: UserAdaptiveContext): String {
        val personalizedPrefix = when {
            context.recentPerformance.averageAttempts > 3 -> "Since you like to explore different approaches: "
            context.recentPerformance.successRate > 0.9 -> "For someone with your skill level: "
            context.topWeakAreas.isNotEmpty() -> "Given your focus on ${context.topWeakAreas.first().skillArea}: "
            else -> ""
        }
        return "$personalizedPrefix$hint"
    }
    
    private fun calculateHintRelevance(hint: String, context: UserAdaptiveContext): Double {
        var relevance = 1.0
        
        // Increase relevance if hint relates to user's weak areas
        context.topWeakAreas.forEach { weakArea ->
            if (hint.contains(weakArea.skillArea, ignoreCase = true)) {
                relevance += 0.3
            }
        }
        
        // Adjust based on user's current difficulty level
        if (hint.contains("beginner", ignoreCase = true) && context.currentDifficultyLevel == DifficultyLevel.EXPERT) {
            relevance -= 0.2
        }
        
        return relevance.coerceIn(0.1, 2.0)
    }
    
    private fun calculateHintUnlockThreshold(index: Int, context: UserAdaptiveContext): Int {
        val baseThreshold = index + 1
        return when (context.currentDifficultyLevel) {
            DifficultyLevel.BEGINNER -> maxOf(1, baseThreshold - 1)
            DifficultyLevel.EXPERT -> baseThreshold + 2
            else -> baseThreshold
        }
    }
    
    private fun calculateTimeEfficiency(metrics: ChallengeSessionMetrics, context: UserAdaptiveContext): Double {
        val expectedTime = when (context.currentDifficultyLevel) {
            DifficultyLevel.BEGINNER -> 600000L // 10 minutes
            DifficultyLevel.EASY -> 900000L // 15 minutes
            DifficultyLevel.MEDIUM -> 1200000L // 20 minutes
            DifficultyLevel.HARD -> 1500000L // 25 minutes
            DifficultyLevel.EXPERT -> 1800000L // 30 minutes
        }
        
        return if (metrics.elapsedTime > 0) {
            (expectedTime.toDouble() / metrics.elapsedTime).coerceIn(0.1, 2.0)
        } else 1.0
    }
    
    private fun analyzeErrorPattern(metrics: ChallengeSessionMetrics, context: UserAdaptiveContext): String {
        return when {
            metrics.errorsCount == 0 -> "Excellent! No errors detected."
            metrics.errorsCount <= 2 -> "Minor issues encountered - good recovery."
            metrics.errorsCount <= 5 -> "Several errors suggest reviewing fundamentals."
            else -> "High error count indicates need for additional support."
        }
    }
    
    private fun identifyStrugglingAreas(metrics: ChallengeSessionMetrics, context: UserAdaptiveContext): List<String> {
        val strugglingAreas = mutableListOf<String>()
        
        if (metrics.attemptsCount > 3) strugglingAreas.add("Problem approach")
        if (metrics.errorsCount > 2) strugglingAreas.add("Implementation accuracy")
        if (metrics.hintsUsed > 2) strugglingAreas.add("Conceptual understanding")
        if (metrics.elapsedTime > 1800000) strugglingAreas.add("Time management")
        
        return strugglingAreas
    }
    
    private fun calculateOverallProgress(metrics: ChallengeSessionMetrics, context: UserAdaptiveContext): Double {
        val completionScore = metrics.currentProgress
        val efficiencyScore = calculateTimeEfficiency(metrics, context)
        val accuracyScore = if (metrics.attemptsCount > 0) 1.0 / metrics.attemptsCount else 1.0
        
        return (completionScore * 0.5 + efficiencyScore * 0.3 + accuracyScore * 0.2).coerceIn(0.0, 1.0)
    }
    
    private fun detectEmergingPatterns(metrics: ChallengeSessionMetrics, context: UserAdaptiveContext): List<EmergingInsight> {
        val insights = mutableListOf<EmergingInsight>()
        
        if (metrics.stuckDuration > 300000) { // 5 minutes stuck
            insights.add(EmergingInsight(
                type = InsightType.STUCK_PATTERN,
                severity = InsightSeverity.MEDIUM,
                description = "User appears stuck on current problem",
                recommendedAction = "Offer a targeted hint",
                confidence = 0.8
            ))
        }
        
        if (metrics.hintsUsed == 0 && metrics.errorsCount > 3) {
            insights.add(EmergingInsight(
                type = InsightType.HINT_AVOIDANCE,
                severity = InsightSeverity.LOW,
                description = "User avoiding hints despite errors",
                recommendedAction = "Suggest using hints",
                confidence = 0.7
            ))
        }
        
        return insights
    }
    
    private fun evaluateMidSessionAdjustment(analysis: SessionPerformanceAnalysis, context: UserAdaptiveContext): DifficultyAdjustment? {
        return when {
            analysis.timeEfficiency < 0.3 && analysis.errorCount > 5 -> {
                DifficultyAdjustment(
                    currentLevel = context.currentDifficultyLevel,
                    recommendedLevel = DifficultyLevel.BEGINNER,
                    reason = "High error rate suggests need for easier content",
                    confidence = 0.8
                )
            }
            analysis.timeEfficiency > 1.5 && analysis.errorCount == 0 -> {
                DifficultyAdjustment(
                    currentLevel = context.currentDifficultyLevel,
                    recommendedLevel = DifficultyLevel.HARD,
                    reason = "Excellent performance suggests readiness for harder content",
                    confidence = 0.7
                )
            }
            else -> null
        }
    }
    
    private fun calculateHintRecommendation(analysis: SessionPerformanceAnalysis, context: UserAdaptiveContext): HintRecommendation {
        return when {
            analysis.stuckDuration > 300000 -> HintRecommendation.IMMEDIATE
            analysis.errorCount > 3 && analysis.hintsUsed == 0 -> HintRecommendation.SUGGESTED
            analysis.timeEfficiency < 0.5 -> HintRecommendation.AVAILABLE
            else -> HintRecommendation.NONE
        }
    }
    
    private fun createPerformanceMetrics(userId: String, task: ChallengeTask, metrics: ChallengeCompletionMetrics, config: AdaptiveChallengeConfig): PerformanceMetrics {
        val end = Clock.System.now().toEpochMilliseconds()
        val start = end - metrics.totalTime
        return PerformanceMetrics(
            id = generateUUID(),
            userId = userId,
            taskId = task.id,
            startTime = start,
            endTime = end,
            durationMillis = metrics.totalTime,
            attempts = metrics.attemptsCount,
            hintsUsed = metrics.hintsUsed,
            completed = metrics.completed,
            errorCount = metrics.errorsCount,
            difficultyLevel = config.recommendedDifficulty,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )
    }
    
    private fun generateLearningInsights(metrics: ChallengeCompletionMetrics, config: AdaptiveChallengeConfig, weakAreas: List<WeakArea>): List<LearningInsight> {
        val insights = mutableListOf<LearningInsight>()
        
        if (metrics.completed && metrics.attemptsCount == 1) {
            insights.add(
                LearningInsight(
                    type = LearningInsightType.IMPROVEMENT_DETECTED,
                    title = "First Try Success",
                    description = "Excellent! Solved on first attempt.",
                    actionable = false,
                    priority = 1,
                    relatedWeakAreas = emptyList()
                )
            )
        }
        
        if (metrics.hintsUsed > 0) {
            insights.add(
                LearningInsight(
                    type = LearningInsightType.STRATEGY_RECOMMENDATION,
                    title = "Effective Hint Usage",
                    description = "Good use of hints to guide your learning.",
                    actionable = true,
                    priority = 3,
                    relatedWeakAreas = emptyList()
                )
            )
        }
        
        return insights
    }
    
    private fun calculateFinalAdaptiveXP(metrics: ChallengeCompletionMetrics, config: AdaptiveChallengeConfig): Int {
        val baseXP = 100
        val difficultyMultiplier = when (config.recommendedDifficulty) {
            DifficultyLevel.BEGINNER -> 0.8
            DifficultyLevel.EASY -> 1.0
            DifficultyLevel.MEDIUM -> 1.2
            DifficultyLevel.HARD -> 1.5
            DifficultyLevel.EXPERT -> 1.6
            else -> 1.0
        }
        
        val performanceMultiplier = when {
            metrics.attemptsCount == 1 -> 1.2
            metrics.attemptsCount <= 2 -> 1.1
            metrics.attemptsCount <= 3 -> 1.0
            else -> 0.9
        }
        
        return (baseXP * difficultyMultiplier * performanceMultiplier * config.adaptiveXpMultiplier).toInt()
    }
    
    private fun determineInterventionType(analysis: SessionPerformanceAnalysis, insights: List<EmergingInsight>): InterventionType {
        return when {
            insights.any { it.type == InsightType.STUCK_PATTERN } -> InterventionType.HINT_SUGGESTION
            analysis.errorCount > 5 -> InterventionType.CONCEPT_REVIEW
            analysis.timeEfficiency < 0.3 -> InterventionType.BREAK_SUGGESTION
            analysis.overallProgress > 0.8 -> InterventionType.ENCOURAGEMENT
            else -> InterventionType.PROGRESS_CHECK
        }
    }
    
    private fun generateInterventionMessage(type: InterventionType, analysis: SessionPerformanceAnalysis, context: UserAdaptiveContext): String {
        return when (type) {
            InterventionType.HINT_SUGGESTION -> "💡 It looks like you might be stuck. Would you like a hint to help you move forward?"
            InterventionType.CONCEPT_REVIEW -> "📚 Consider reviewing the core concepts for this topic before continuing."
            InterventionType.BREAK_SUGGESTION -> "☕ You've been working hard! Consider taking a short break to refresh your mind."
            InterventionType.ENCOURAGEMENT -> "🚀 You're doing great! Keep up the excellent progress."
            InterventionType.PROGRESS_CHECK -> "📊 How are you feeling about this challenge? Need any support?"
            else -> "Keep going! You've got this! 💪"
        }
    }
    
    private fun calculateInterventionUrgency(analysis: SessionPerformanceAnalysis, insights: List<EmergingInsight>): InterventionUrgency {
        return when {
            analysis.stuckDuration > 600000 -> InterventionUrgency.HIGH // 10 minutes stuck
            analysis.errorCount > 8 -> InterventionUrgency.HIGH
            insights.any { it.confidence > 0.8 } -> InterventionUrgency.MEDIUM
            analysis.overallProgress < 0.2 -> InterventionUrgency.MEDIUM
            else -> InterventionUrgency.LOW
        }
    }
    
    private fun generateActionableSuggestions(analysis: SessionPerformanceAnalysis, context: UserAdaptiveContext): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (analysis.errorCount > 3) {
            suggestions.add("Review the problem statement carefully")
            suggestions.add("Test your solution with simple examples first")
        }
        
        if (analysis.timeEfficiency < 0.5) {
            suggestions.add("Break the problem into smaller steps")
            suggestions.add("Plan your approach before coding")
        }
        
        if (analysis.hintsUsed == 0 && analysis.stuckDuration > 300000) {
            suggestions.add("Consider using available hints for guidance")
        }
        
        return suggestions.take(3)
    }

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
