package com.dailydevchallenge.devstreaks.service

import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.utils.generateUUID
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.math.*

/**
 * Real-time monitoring service for detecting weak area patterns during active challenge sessions
 */
class RealTimeWeakAreaMonitoringService(
    private val challengeRepository: ChallengeRepository
) {
    private val logger = { getLogger() }

    companion object {
        private const val STRUGGLE_THRESHOLD = 3 // attempts before considering struggle
        private const val TIME_PRESSURE_THRESHOLD = 600000L // 10 minutes
        private const val ERROR_BURST_THRESHOLD = 5 // errors in short time
        private const val IMPROVEMENT_THRESHOLD = 0.2 // 20% improvement to detect positive trend
    }

    /**
     * Process real-time performance and generate immediate insights
     */
    suspend fun processRealTimePerformance(
        userId: String,
        taskId: String,
        currentMetrics: PerformanceMetrics
    ): List<WeakAreaInsight> = withContext(Dispatchers.Default) {

        val insights = mutableListOf<WeakAreaInsight>()

        try {
            // Get recent performance for context
            val recentMetrics = challengeRepository.getRecentPerformanceMetrics(userId, 5)

            // Detect immediate struggle patterns
            detectImmediateStruggles(userId, taskId, currentMetrics)?.let { insights.add(it) }

            // Detect time pressure patterns
            detectTimePressurePatterns(userId, taskId, currentMetrics)?.let { insights.add(it) }

            // Detect error burst patterns
            detectErrorBurstPatterns(userId, taskId, currentMetrics)?.let { insights.add(it) }

            // Detect improvement trends
            detectImprovementTrends(userId, taskId, currentMetrics, recentMetrics)?.let { insights.add(it) }

            // Detect consistency issues
            detectConsistencyIssues(userId, taskId, currentMetrics, recentMetrics)?.let { insights.add(it) }

            logger().d("RealTimeMonitoring", "Generated ${insights.size} insights for user $userId")

        } catch (e: Exception) {
            logger().e("RealTimeMonitoring", e,"Error processing real-time performance: ${e
                .message}")
        }

        return@withContext insights
    }

    /**
     * Detect when user is struggling with current challenge
     */
    private fun detectImmediateStruggles(
        userId: String,
        taskId: String,
        metrics: PerformanceMetrics
    ): WeakAreaInsight? {
        return if (metrics.attempts >= STRUGGLE_THRESHOLD && !metrics.completed) {
            WeakAreaInsight(
                id = generateUUID(),
                userId = userId,
                skillArea = inferSkillAreaFromMetrics(metrics),
                insightType = InsightType.WEAKNESS_DETECTED,
                message = "User is struggling with current challenge",
                severity = InsightSeverity.MEDIUM,
                actionable = true,
                suggestedAction = "Consider breaking down the problem into smaller steps",
                triggerEvent = "Multiple attempts without completion",
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        } else null
    }

    /**
     * Detect time pressure patterns
     */
    private fun detectTimePressurePatterns(
        userId: String,
        taskId: String,
        metrics: PerformanceMetrics
    ): WeakAreaInsight? {
        return if (metrics.durationMillis > TIME_PRESSURE_THRESHOLD && !metrics.completed) {
            WeakAreaInsight(
                id = generateUUID(),
                userId = userId,
                skillArea = inferSkillAreaFromMetrics(metrics),
                insightType = InsightType.TIME_PRESSURE,
                message = "User is taking longer than expected",
                severity = InsightSeverity.MEDIUM,
                actionable = true,
                suggestedAction = "Focus on accuracy over speed, take breaks if needed",
                triggerEvent = "Extended time on challenge",
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        } else null
    }

    /**
     * Detect error burst patterns
     */
    private fun detectErrorBurstPatterns(
        userId: String,
        taskId: String,
        metrics: PerformanceMetrics
    ): WeakAreaInsight? {
        return if (metrics.errorCount >= ERROR_BURST_THRESHOLD) {
            WeakAreaInsight(
                id = generateUUID(),
                userId = userId,
                skillArea = inferSkillAreaFromMetrics(metrics),
                insightType = InsightType.ERROR_PATTERN,
                message = "High error count detected",
                severity = InsightSeverity.HIGH,
                actionable = true,
                suggestedAction = "Review fundamentals, check syntax and logic carefully",
                triggerEvent = "Multiple errors in succession",
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        } else null
    }

    /**
     * Detect improvement trends from recent performance
     */
    private fun detectImprovementTrends(
        userId: String,
        taskId: String,
        currentMetrics: PerformanceMetrics,
        recentMetrics: List<PerformanceMetrics>
    ): WeakAreaInsight? {
        if (recentMetrics.size < 3) return null

        val recentSuccessRate = recentMetrics.takeLast(3).count { it.completed }.toDouble() / 3
        val olderSuccessRate = recentMetrics.take(recentMetrics.size - 3).let { older ->
            if (older.isEmpty()) 0.0 else older.count { it.completed }.toDouble() / older.size
        }

        val improvement = recentSuccessRate - olderSuccessRate

        return if (improvement >= IMPROVEMENT_THRESHOLD) {
            WeakAreaInsight(
                id = generateUUID(),
                userId = userId,
                skillArea = inferSkillAreaFromMetrics(currentMetrics),
                insightType = InsightType.IMPROVEMENT_DETECTED,
                message = "Great progress detected!",
                severity = InsightSeverity.LOW,
                actionable = true,
                suggestedAction = "Great progress! Consider increasing difficulty level",
                triggerEvent = "Consistent improvement pattern",
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        } else null
    }

    /**
     * Detect consistency issues
     */
    private fun detectConsistencyIssues(
        userId: String,
        taskId: String,
        currentMetrics: PerformanceMetrics,
        recentMetrics: List<PerformanceMetrics>
    ): WeakAreaInsight? {
        if (recentMetrics.size < 5) return null

        val attempts = recentMetrics.map { it.attempts } + currentMetrics.attempts
        val mean = attempts.average()
        val variance = attempts.map { (it - mean).pow(2) }.average()
        val standardDeviation = sqrt(variance)

        val inconsistencyScore = standardDeviation / mean

        return if (inconsistencyScore > 0.5) { // High inconsistency
            WeakAreaInsight(
                id = generateUUID(),
                userId = userId,
                skillArea = inferSkillAreaFromMetrics(currentMetrics),
                insightType = InsightType.INCONSISTENCY,
                message = "Inconsistent performance pattern detected",
                severity = InsightSeverity.MEDIUM,
                actionable = true,
                suggestedAction = "Focus on consistent problem-solving approach",
                triggerEvent = "High performance variance",
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        } else null
    }

    /**
     * Detect learning plateau patterns
     */
    suspend fun detectLearningPlateau(
        userId: String,
        lookbackDays: Int = 7
    ): WeakAreaInsight? = withContext(Dispatchers.Default) {

        val recentMetrics = challengeRepository.getRecentPerformanceMetrics(userId, 20)
        if (recentMetrics.size < 10) return@withContext null

        // Check if performance has stagnated
        val firstHalf = recentMetrics.take(recentMetrics.size / 2)
        val secondHalf = recentMetrics.drop(recentMetrics.size / 2)

        val firstHalfSuccess = firstHalf.count { it.completed }.toDouble() / firstHalf.size
        val secondHalfSuccess = secondHalf.count { it.completed }.toDouble() / secondHalf.size

        val improvement = secondHalfSuccess - firstHalfSuccess

        return@withContext if (improvement < -0.1) { // Performance declining
            WeakAreaInsight(
                id = generateUUID(),
                userId = userId,
                skillArea = "General",
                insightType = InsightType.LEARNING_PLATEAU,
                message = "Learning plateau detected - performance has stagnated",
                severity = InsightSeverity.MEDIUM,
                actionable = true,
                suggestedAction = "Try different challenge types or take a learning break",
                triggerEvent = "Declining performance trend",
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
        } else null
    }

    /**
     * Get adaptive difficulty recommendation based on real-time performance
     */
    fun getAdaptiveDifficultyRecommendation(
        currentDifficulty: DifficultyLevel,
        insights: List<WeakAreaInsight>
    ): DifficultyLevel {
        val struggleInsights = insights.filter {
            it.insightType in listOf(InsightType.WEAKNESS_DETECTED, InsightType.ERROR_PATTERN, InsightType.TIME_PRESSURE)
        }
        val improvementInsights = insights.filter { it.insightType == InsightType.IMPROVEMENT_DETECTED }

        return when {
            struggleInsights.isNotEmpty() -> {
                // Struggle detected, recommend easier difficulty
                when (currentDifficulty) {
                    DifficultyLevel.EXPERT -> DifficultyLevel.HARD
                    DifficultyLevel.HARD -> DifficultyLevel.MEDIUM
                    DifficultyLevel.MEDIUM -> DifficultyLevel.EASY
                    DifficultyLevel.EASY -> DifficultyLevel.BEGINNER
                    DifficultyLevel.BEGINNER -> DifficultyLevel.BEGINNER
                }
            }
            improvementInsights.isNotEmpty() -> {
                // Improvement detected, can increase difficulty
                when (currentDifficulty) {
                    DifficultyLevel.BEGINNER -> DifficultyLevel.EASY
                    DifficultyLevel.EASY -> DifficultyLevel.MEDIUM
                    DifficultyLevel.MEDIUM -> DifficultyLevel.HARD
                    DifficultyLevel.HARD -> DifficultyLevel.EXPERT
                    DifficultyLevel.EXPERT -> DifficultyLevel.EXPERT
                }
            }
            else -> currentDifficulty // No significant change needed
        }
    }

    /**
     * Generate contextual hints based on current performance patterns
     */
    fun generateContextualHints(
        insights: List<WeakAreaInsight>,
        currentAttempts: Int
    ): List<String> {
        val hints = mutableListOf<String>()

        insights.forEach { insight ->
            when (insight.insightType) {
                InsightType.WEAKNESS_DETECTED -> {
                    hints.add("Break the problem into smaller, manageable steps")
                    hints.add("Review similar problems you've solved before")
                }
                InsightType.TIME_PRESSURE -> {
                    hints.add("Focus on understanding rather than speed")
                    hints.add("Take short breaks to maintain focus")
                }
                InsightType.ERROR_PATTERN -> {
                    hints.add("Double-check your syntax and logic")
                    hints.add("Test with simple inputs first")
                }
                InsightType.INCONSISTENCY -> {
                    hints.add("Develop a consistent problem-solving approach")
                    hints.add("Practice similar problem types regularly")
                }
                InsightType.IMPROVEMENT_DETECTED -> {
                    hints.add("Great progress! Keep up the momentum")
                    hints.add("Consider tackling more challenging problems")
                }
                else -> {
                    hints.add("Stay focused and think step by step")
                }
            }
        }

        // Add general hints based on attempt count
        when {
            currentAttempts == 1 -> hints.add("Take time to understand the problem thoroughly")
            currentAttempts in 2..3 -> hints.add("Review your approach and consider alternatives")
            currentAttempts > 3 -> hints.add("Step back and try a completely different approach")
        }

        return hints.take(3) // Limit to 3 most relevant hints
    }

    /**
     * Infer skill area from performance metrics (could be enhanced with ML)
     */
    private fun inferSkillAreaFromMetrics(metrics: PerformanceMetrics): String {
        // Simple inference based on difficulty and performance patterns
        return when {
            metrics.errorCount > 5 -> "Debugging"
            metrics.attempts > 5 -> "Problem Solving"
            metrics.durationMillis > TIME_PRESSURE_THRESHOLD -> "Time Management"
            metrics.hintsUsed > 3 -> "Conceptual Understanding"
            else -> "General Programming"
        }
    }
}
