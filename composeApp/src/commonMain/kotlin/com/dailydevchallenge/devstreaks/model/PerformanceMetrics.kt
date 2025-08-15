package com.dailydevchallenge.devstreaks.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.*

/**
 * Performance metrics for tracking user challenge performance
 */
@Serializable
data class PerformanceMetrics(
    val id: String,
    val userId: String,
    val taskId: String,
    val startTime: Long,
    val endTime: Long,
    val durationMillis: Long,
    val attempts: Int,
    val hintsUsed: Int,
    val completed: Boolean,
    val errorCount: Int,
    val difficultyLevel: DifficultyLevel,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

@Serializable
enum class DifficultyLevel(val value: Int, val multiplier: Double) {
    BEGINNER(1, 0.8),
    EASY(2, 1.0),
    MEDIUM(3, 1.2),
    HARD(4, 1.5),
    EXPERT(5, 2.0);

    companion object {
        fun fromValue(value: Int): DifficultyLevel {
            return entries.find { it.value == value } ?: MEDIUM
        }
    }
}

/**
 * User performance summary for difficulty adjustment calculations
 */
@Serializable
data class UserPerformanceSummary(
    val userId: String,
    val averageCompletionTime: Long,
    val successRate: Double,
    val averageAttempts: Double,
    val averageHintsUsed: Double,
    val averageErrorCount: Double,
    val currentDifficultyLevel: DifficultyLevel,
    val recentTaskCount: Int,
    val learningVelocity: Double = 0.0,
    val consistencyScore: Double = 0.0,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Difficulty adjustment recommendation
 */
@Serializable
data class DifficultyAdjustment(
    val currentLevel: DifficultyLevel,
    val recommendedLevel: DifficultyLevel,
    val reason: String,
    val confidence: Double // 0.0 to 1.0
)

/**
 * Baseline performance metrics for difficulty calculations
 */
@Serializable
data class BaselineMetrics(
    val averageCompletionTime: Long,
    val expectedSuccessRate: Double,
    val expectedAttempts: Double,
    val expectedHintsUsed: Double
)

/**
 * Insight types for real-time monitoring
 */
@Serializable
enum class InsightType {
    WEAKNESS_DETECTED,
    TIME_PRESSURE,
    ERROR_PATTERN,
    IMPROVEMENT_DETECTED,
    INCONSISTENCY,
    LEARNING_PLATEAU
}

/**
 * Insight severity levels
 */
@Serializable
enum class InsightSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
