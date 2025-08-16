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
 * Analyzes user performance to detect weak areas and learning patterns
 */
class WeakAreaDetectionService(
    private val challengeRepository: ChallengeRepository
) {
    private val logger = { getLogger() }

    companion object {
        private const val WEAK_AREA_THRESHOLD = 0.6 // Success rate below 60%
        private const val SEVERE_WEAK_AREA_THRESHOLD = 0.4 // Success rate below 40%
        private const val MIN_ATTEMPTS_FOR_ANALYSIS = 3
        private const val TREND_ANALYSIS_WINDOW = 5 // Last 5 attempts
    }

    /**
     * Detect and analyze user's weak areas based on performance history
     */
    suspend fun detectWeakAreas(userId: String): List<WeakArea> = withContext(Dispatchers.Default) {
        val performanceSummary = challengeRepository.calculateUserPerformanceSummary(userId, 20)

        if (performanceSummary.recentTaskCount < MIN_ATTEMPTS_FOR_ANALYSIS) {
            return@withContext emptyList()
        }

        // Analyze performance by skill categories
        val skillAnalysis = analyzeSkillPerformance(userId)
        val weakAreas = mutableListOf<WeakArea>()

        skillAnalysis.forEach { (skillArea, metrics) ->
            if (metrics.successRate < WEAK_AREA_THRESHOLD && metrics.attemptCount >= MIN_ATTEMPTS_FOR_ANALYSIS) {
                val weakArea = createWeakArea(userId, skillArea, metrics)
                weakAreas.add(weakArea)

                logger().d("WeakAreaDetection",
                    "Weak area detected: $skillArea (Success rate: ${(metrics.successRate * 100).toInt()}%)")
            }
        }

        return@withContext weakAreas.sortedByDescending { it.severityScore }
    }

    /**
     * Analyze performance by specific skill areas
     */
    private suspend fun analyzeSkillPerformance(userId: String): Map<String, SkillMetrics> {
        // This would typically query performance data grouped by skill tags
        // For now, we'll simulate analysis based on task types and common patterns

        val skillMetrics = mutableMapOf<String, SkillMetrics>()

        // Simulate skill-based analysis (in real implementation, tasks would be tagged with skills)
        val simulatedSkills = mapOf(
            "Arrays" to SkillMetrics(0.45, 4.2, 1200000, 8, TrendDirection.DECLINING),
            "Loops" to SkillMetrics(0.75, 2.1, 800000, 12, TrendDirection.IMPROVING),
            "Recursion" to SkillMetrics(0.35, 5.8, 1800000, 6, TrendDirection.STABLE),
            "String Manipulation" to SkillMetrics(0.85, 1.5, 600000, 15, TrendDirection.IMPROVING),
            "Debugging" to SkillMetrics(0.55, 3.5, 1500000, 10, TrendDirection.DECLINING)
        )

        return simulatedSkills
    }

    /**
     * Create a weak area analysis from skill metrics
     */
    private fun createWeakArea(userId: String, skillArea: String, metrics: SkillMetrics): WeakArea {
        val severityScore = calculateSeverityScore(metrics.successRate, metrics.averageAttempts)
        val commonErrors = generateCommonErrors(skillArea, metrics)
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val recommendedActions = generateRecommendedActions(skillArea, metrics)

        return WeakArea(
            id = generateUUID(),
            userId = userId,
            skillArea = skillArea,
            severityScore = severityScore,
            detectedAt = currentTime,
            lastOccurrence = currentTime,
            occurrenceCount = metrics.attemptCount,
            trend = convertToWeakAreaTrend(metrics.trend),
            recommendedActions = recommendedActions,
            frequencyCount = metrics.attemptCount,
            lastEncountered = currentTime,
            averageAttempts = metrics.averageAttempts,
            averageTimeSpent = metrics.averageTimeSpent,
            commonErrors = commonErrors,
            improvementTrend = metrics.trend,
            confidenceLevel = calculateConfidenceLevel(metrics.attemptCount, metrics.successRate)
        )
    }

    private fun convertToWeakAreaTrend(trend: TrendDirection): WeakAreaTrend {
        return when (trend) {
            TrendDirection.IMPROVING -> WeakAreaTrend.IMPROVING
            TrendDirection.DECLINING -> WeakAreaTrend.WORSENING
            TrendDirection.STABLE -> WeakAreaTrend.STABLE
            TrendDirection.UNKNOWN -> WeakAreaTrend.UNKNOWN
        }
    }

    private fun generateRecommendedActions(skillArea: String, metrics: SkillMetrics): List<String> {
        return when (skillArea.lowercase()) {
            "arrays" -> listOf(
                "Practice array manipulation problems",
                "Review indexing and bounds checking",
                "Study common array algorithms"
            )
            "recursion" -> listOf(
                "Practice identifying base cases",
                "Trace through recursive calls step by step",
                "Start with simple recursive problems"
            )
            "debugging" -> listOf(
                "Use systematic debugging approaches",
                "Practice with debugger tools",
                "Learn to read error messages effectively"
            )
            else -> listOf(
                "Focus on understanding core concepts",
                "Practice with guided examples",
                "Break down complex problems into smaller steps"
            )
        }
    }

    private fun calculateSeverityScore(successRate: Double, averageAttempts: Double): Double {
        val successComponent = 1.0 - successRate // Lower success = higher severity
        val attemptComponent = min(1.0, (averageAttempts - 1.0) / 4.0) // More attempts = higher severity
        return (successComponent * 0.7 + attemptComponent * 0.3).coerceIn(0.0, 1.0)
    }

    private fun categorizeSkill(skillArea: String): SkillCategory {
        return when {
            skillArea.contains("Array", ignoreCase = true) -> SkillCategory.DATA_STRUCTURES
            skillArea.contains("Loop", ignoreCase = true) -> SkillCategory.ALGORITHMS
            skillArea.contains("Recursion", ignoreCase = true) -> SkillCategory.ALGORITHMS
            skillArea.contains("String", ignoreCase = true) -> SkillCategory.DATA_STRUCTURES
            skillArea.contains("Debug", ignoreCase = true) -> SkillCategory.DEBUGGING
            skillArea.contains("System", ignoreCase = true) -> SkillCategory.SYSTEM_DESIGN
            else -> SkillCategory.PROBLEM_SOLVING
        }
    }

    private fun generateCommonErrors(skillArea: String, metrics: SkillMetrics): List<String> {
        return when (skillArea.lowercase()) {
            "arrays" -> listOf(
                "Index out of bounds errors",
                "Off-by-one mistakes in loops",
                "Forgetting to handle empty arrays"
            )
            "recursion" -> listOf(
                "Missing base case",
                "Stack overflow from infinite recursion",
                "Incorrect recursive call parameters"
            )
            "debugging" -> listOf(
                "Not using debugger effectively",
                "Assuming instead of verifying values",
                "Overlooking edge cases"
            )
            else -> listOf(
                "Logic errors in implementation",
                "Not considering edge cases",
                "Inefficient approach to problem"
            )
        }
    }

    private fun calculateConfidenceLevel(attemptCount: Int, successRate: Double): Double {
        val sampleConfidence = min(1.0, attemptCount / 10.0)
        val patternConfidence = if (successRate < 0.3 || successRate > 0.8) 0.9 else 0.6
        return (sampleConfidence + patternConfidence) / 2.0
    }

    /**
     * Generate improvement recommendations for weak areas
     */
    suspend fun generateImprovementRecommendations(weakArea: WeakArea): List<String> {
        val skillCategory = categorizeSkill(weakArea.skillArea)
        return when (skillCategory) {
            SkillCategory.DATA_STRUCTURES -> listOf(
                "Practice with visualization tools to understand ${weakArea.skillArea} better",
                "Start with simpler ${weakArea.skillArea} problems and gradually increase complexity",
                "Review the time/space complexity of ${weakArea.skillArea} operations"
            )
            SkillCategory.ALGORITHMS -> listOf(
                "Break down ${weakArea.skillArea} problems into smaller steps",
                "Practice tracing through ${weakArea.skillArea} execution manually",
                "Study common ${weakArea.skillArea} patterns and templates"
            )
            SkillCategory.DEBUGGING -> listOf(
                "Use debugger tools more frequently",
                "Practice reading error messages carefully",
                "Learn systematic debugging approaches"
            )
            else -> listOf(
                "Focus on understanding the core concepts of ${weakArea.skillArea}",
                "Practice similar problems with guided solutions",
                "Seek additional resources or tutorials on ${weakArea.skillArea}"
            )
        }
    }
}

/**
 * Internal data structure for skill performance analysis
 */
private data class SkillMetrics(
    val successRate: Double,
    val averageAttempts: Double,
    val averageTimeSpent: Long,
    val attemptCount: Int,
    val trend: TrendDirection
)
