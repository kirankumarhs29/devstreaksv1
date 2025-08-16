package com.dailydevchallenge.devstreaks.service

import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*

/**
 * Dynamic Difficulty Calculator
 * Analyzes user performance to recommend optimal challenge difficulty
 */
class DifficultyCalculator {
    private val logger = { getLogger() }

    // Performance thresholds for difficulty adjustment
    companion object {
        private const val HIGH_SUCCESS_THRESHOLD = 0.85
        private const val LOW_SUCCESS_THRESHOLD = 0.60
        private const val FAST_COMPLETION_THRESHOLD = 0.7 // 70% of average time
        private const val SLOW_COMPLETION_THRESHOLD = 1.5 // 150% of average time
        private const val MIN_SAMPLES_FOR_ADJUSTMENT = 3
        private const val CONFIDENCE_THRESHOLD = 0.75
    }

    /**
     * Calculate difficulty adjustment based on user performance
     */
    suspend fun calculateDifficultyAdjustment(
        performanceSummary: UserPerformanceSummary,
        baselineMetrics: Map<DifficultyLevel, BaselineMetrics>
    ): DifficultyAdjustment = withContext(Dispatchers.Default) {

        if (performanceSummary.recentTaskCount < MIN_SAMPLES_FOR_ADJUSTMENT) {
            return@withContext DifficultyAdjustment(
                currentLevel = performanceSummary.currentDifficultyLevel,
                recommendedLevel = performanceSummary.currentDifficultyLevel,
                reason = "Insufficient data for adjustment (need at least $MIN_SAMPLES_FOR_ADJUSTMENT completed tasks)",
                confidence = 0.0
            )
        }

        val currentLevel = performanceSummary.currentDifficultyLevel
        val baseline = baselineMetrics[currentLevel] ?: getDefaultBaseline(currentLevel)

        // Calculate performance scores
        val successScore = calculateSuccessScore(performanceSummary.successRate)
        val speedScore = calculateSpeedScore(performanceSummary.averageCompletionTime, baseline.averageCompletionTime)
        val efficiencyScore = calculateEfficiencyScore(
            performanceSummary.averageAttempts,
            performanceSummary.averageHintsUsed,
            performanceSummary.averageErrorCount
        )

        // Weighted composite score
        val compositeScore = (successScore * 0.4) + (speedScore * 0.3) + (efficiencyScore * 0.3)

        val recommendedLevel = determineRecommendedLevel(currentLevel, compositeScore, performanceSummary)
        val confidence = calculateConfidence(performanceSummary, compositeScore)
        val reason = generateAdjustmentReason(currentLevel, recommendedLevel, performanceSummary, compositeScore)

        logger().d("DifficultyCalculator", "User performance analysis: Success=$successScore, Speed=$speedScore, Efficiency=$efficiencyScore, Composite=$compositeScore")

        DifficultyAdjustment(
            currentLevel = currentLevel,
            recommendedLevel = recommendedLevel,
            reason = reason,
            confidence = confidence
        )
    }

    private fun calculateSuccessScore(successRate: Double): Double {
        return when {
            successRate >= HIGH_SUCCESS_THRESHOLD -> 1.0
            successRate >= LOW_SUCCESS_THRESHOLD -> 0.5
            else -> 0.0
        }
    }

    private fun calculateSpeedScore(userTime: Long, baselineTime: Long): Double {
        val ratio = userTime.toDouble() / baselineTime.toDouble()
        return when {
            ratio <= FAST_COMPLETION_THRESHOLD -> 1.0
            ratio <= 1.0 -> 0.7
            ratio <= SLOW_COMPLETION_THRESHOLD -> 0.3
            else -> 0.0
        }
    }

    private fun calculateEfficiencyScore(avgAttempts: Double, avgHints: Double, avgErrors: Double): Double {
        // Lower values are better for efficiency
        val attemptScore = max(0.0, 1.0 - (avgAttempts - 1.0) / 3.0) // Penalize multiple attempts
        val hintScore = max(0.0, 1.0 - avgHints / 3.0) // Penalize hint usage
        val errorScore = max(0.0, 1.0 - avgErrors / 5.0) // Penalize errors

        return (attemptScore + hintScore + errorScore) / 3.0
    }

    private fun determineRecommendedLevel(
        currentLevel: DifficultyLevel,
        compositeScore: Double,
        performance: UserPerformanceSummary
    ): DifficultyLevel {
        return when {
            // User is performing excellently - increase difficulty
            compositeScore >= 0.8 && performance.successRate >= HIGH_SUCCESS_THRESHOLD -> {
                val nextLevel = currentLevel.ordinal + 1
                if (nextLevel < DifficultyLevel.values().size) {
                    DifficultyLevel.values()[nextLevel]
                } else currentLevel
            }
            // User is struggling - decrease difficulty
            compositeScore <= 0.3 || performance.successRate < LOW_SUCCESS_THRESHOLD -> {
                val prevLevel = currentLevel.ordinal - 1
                if (prevLevel >= 0) {
                    DifficultyLevel.values()[prevLevel]
                } else currentLevel
            }
            // User is performing adequately - maintain current level
            else -> currentLevel
        }
    }

    private fun calculateConfidence(performance: UserPerformanceSummary, compositeScore: Double): Double {
        // Confidence increases with more data and clear performance patterns
        val sampleConfidence = min(1.0, performance.recentTaskCount / 10.0)
        val patternConfidence = when {
            compositeScore >= 0.8 || compositeScore <= 0.3 -> 1.0 // Clear pattern
            compositeScore >= 0.6 || compositeScore <= 0.5 -> 0.7 // Moderate pattern
            else -> 0.4 // Unclear pattern
        }

        return (sampleConfidence + patternConfidence) / 2.0
    }

    private fun generateAdjustmentReason(
        currentLevel: DifficultyLevel,
        recommendedLevel: DifficultyLevel,
        performance: UserPerformanceSummary,
        compositeScore: Double
    ): String {
        return when {
            recommendedLevel.ordinal > currentLevel.ordinal -> {
                "Excellent performance detected! Success rate: ${(performance.successRate * 100).toInt()}%, " +
                "completing challenges efficiently. Ready for more challenging content."
            }
            recommendedLevel.ordinal < currentLevel.ordinal -> {
                "Performance indicates difficulty adjustment needed. Success rate: ${(performance.successRate * 100).toInt()}%, " +
                "average attempts: ${performance.averageAttempts.toInt()}. Lowering difficulty to build confidence."
            }
            else -> {
                "Performance is optimal for current difficulty level. Success rate: ${(performance.successRate * 100).toInt()}%, " +
                "maintaining current challenge level."
            }
        }
    }

    private fun getDefaultBaseline(level: DifficultyLevel): BaselineMetrics {
        return when (level) {
            DifficultyLevel.BEGINNER -> BaselineMetrics(300000, 0.9, 1.5, 2.0, 1.0) // 5 min
            DifficultyLevel.EASY -> BaselineMetrics(600000, 0.8, 1.8, 1.5, 2.0) // 10 min
            DifficultyLevel.MEDIUM -> BaselineMetrics(900000, 0.75, 2.0, 1.0, 3.0) // 15 min
            DifficultyLevel.HARD -> BaselineMetrics(1200000, 0.7, 2.5, 0.5, 4.0) // 20 min
            DifficultyLevel.EXPERT -> BaselineMetrics(1800000, 0.65, 3.0, 0.2, 5.0) // 30 min
        }
    }
}

/**
 * Baseline metrics for each difficulty level
 */
data class BaselineMetrics(
    val averageCompletionTime: Long,
    val expectedSuccessRate: Double,
    val expectedAttempts: Double,
    val expectedHints: Double,
    val expectedErrors: Double
)
