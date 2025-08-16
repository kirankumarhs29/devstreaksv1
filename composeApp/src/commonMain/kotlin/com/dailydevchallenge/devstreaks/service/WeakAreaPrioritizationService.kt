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
 * Advanced Weak Area Prioritization Service
 * Provides intelligent prioritization and progressive improvement tracking
 */
class WeakAreaPrioritizationService(
    private val challengeRepository: ChallengeRepository,
    private val weakAreaDetectionService: WeakAreaDetectionService
) {
    private val logger = { getLogger() }

    companion object {
        private const val CRITICAL_PRIORITY_THRESHOLD = 0.8
        private const val HIGH_PRIORITY_THRESHOLD = 0.6
        private const val MEDIUM_PRIORITY_THRESHOLD = 0.4
        private const val IMPROVEMENT_THRESHOLD = 0.15 // 15% improvement
        private const val STAGNATION_DAYS = 7 // Days without improvement
    }

    /**
     * Get prioritized weak areas with intelligent ranking
     */
    suspend fun getPrioritizedWeakAreas(userId: String): List<PrioritizedWeakArea> = withContext(Dispatchers.Default) {
        val weakAreas = weakAreaDetectionService.detectWeakAreas(userId)
        val userPerformance = challengeRepository.calculateUserPerformanceSummary(userId)

        val prioritizedAreas = weakAreas.map { weakArea ->
            val priority = calculatePriority(weakArea, userPerformance)
            val impact = calculateLearningImpact(weakArea, userPerformance)
            val urgency = calculateUrgency(weakArea)
            val difficulty = calculateImprovementDifficulty(weakArea)

            PrioritizedWeakArea(
                weakArea = weakArea,
                priority = priority,
                priorityReason = generatePriorityReason(weakArea, priority),
                learningImpact = impact,
                urgencyLevel = urgency,
                improvementDifficulty = difficulty,
                recommendedActions = generateRecommendedActions(weakArea, priority),
                estimatedImprovementTime = estimateImprovementTime(weakArea, difficulty),
                nextMilestone = generateNextMilestone(weakArea)
            )
        }

        // Sort by priority score (highest first)
        val sorted = prioritizedAreas.sortedWith(
            compareByDescending<PrioritizedWeakArea> { it.priority.score }
                .thenByDescending { it.urgencyLevel.value }
                .thenBy { it.improvementDifficulty.score }
        )

        logger().d("WeakAreaPrioritization",
            "Prioritized ${sorted.size} weak areas for user $userId")

        return@withContext sorted
    }

    /**
     * Calculate priority score based on multiple factors
     */
    private fun calculatePriority(
        weakArea: WeakArea,
        userPerformance: UserPerformanceSummary
    ): Priority {
        // Base priority from severity
        var score = weakArea.severityScore

        // Frequency impact (more frequent = higher priority)
        val frequencyMultiplier = min(2.0, weakArea.frequencyCount / 5.0)
        score += frequencyMultiplier * 0.2

        // Trend impact (declining = higher priority)
        val trendMultiplier = when (weakArea.improvementTrend) {
            TrendDirection.DECLINING -> 0.3
            TrendDirection.STABLE -> 0.1
            TrendDirection.IMPROVING -> -0.1
            TrendDirection.UNKNOWN -> 0.0
        }
        score += trendMultiplier

        // User difficulty level impact
        val difficultyMultiplier = when (userPerformance.currentDifficultyLevel) {
            DifficultyLevel.BEGINNER -> 0.2 // More forgiving for beginners
            DifficultyLevel.EASY -> 0.1
            DifficultyLevel.MEDIUM -> 0.0
            DifficultyLevel.HARD -> -0.1
            DifficultyLevel.EXPERT -> -0.2 // Experts should handle more difficulty
        }
        score += difficultyMultiplier

        // Confidence multiplier
        score *= weakArea.confidenceLevel

        val finalScore = score.coerceIn(0.0, 1.0)

        val level = when {
            finalScore >= CRITICAL_PRIORITY_THRESHOLD -> PriorityLevel.CRITICAL
            finalScore >= HIGH_PRIORITY_THRESHOLD -> PriorityLevel.HIGH
            finalScore >= MEDIUM_PRIORITY_THRESHOLD -> PriorityLevel.MEDIUM
            else -> PriorityLevel.LOW
        }

        return Priority(level, finalScore)
    }

    /**
     * Calculate potential learning impact of improving this weak area
     */
    private fun calculateLearningImpact(
        weakArea: WeakArea,
        userPerformance: UserPerformanceSummary
    ): LearningImpact {
        // Foundation skills have higher impact
        val skillCategory = categorizeSkill(weakArea.skillArea)
        val foundationMultiplier = when (skillCategory) {
            SkillCategory.DATA_STRUCTURES -> 1.5
            SkillCategory.ALGORITHMS -> 1.4
            SkillCategory.LOGIC -> 1.3
            SkillCategory.SYNTAX -> 1.2
            SkillCategory.DEBUGGING -> 1.1
            else -> 1.0
        }

        // Frequency indicates how often this skill is needed
        val frequencyImpact = min(1.0, weakArea.frequencyCount / 10.0)

        // Severity indicates how much improvement is possible
        val improvementPotential = weakArea.severityScore

        val impactScore = (foundationMultiplier * frequencyImpact * improvementPotential).coerceIn(0.0, 1.0)

        val level = when {
            impactScore >= 0.8 -> ImpactLevel.TRANSFORMATIVE
            impactScore >= 0.6 -> ImpactLevel.HIGH
            impactScore >= 0.4 -> ImpactLevel.MEDIUM
            else -> ImpactLevel.LOW
        }

        return LearningImpact(level, impactScore)
    }

    /**
     * Helper function to categorize skills based on skill area name
     */
    private fun categorizeSkill(skillArea: String): SkillCategory {
        return when {
            skillArea.contains("Array", ignoreCase = true) ||
            skillArea.contains("List", ignoreCase = true) ||
            skillArea.contains("Stack", ignoreCase = true) ||
            skillArea.contains("Queue", ignoreCase = true) -> SkillCategory.DATA_STRUCTURES

            skillArea.contains("Loop", ignoreCase = true) ||
            skillArea.contains("Recursion", ignoreCase = true) ||
            skillArea.contains("Sort", ignoreCase = true) ||
            skillArea.contains("Search", ignoreCase = true) -> SkillCategory.ALGORITHMS

            skillArea.contains("Debug", ignoreCase = true) ||
            skillArea.contains("Error", ignoreCase = true) -> SkillCategory.DEBUGGING

            skillArea.contains("System", ignoreCase = true) ||
            skillArea.contains("Design", ignoreCase = true) -> SkillCategory.SYSTEM_DESIGN

            skillArea.contains("Syntax", ignoreCase = true) ||
            skillArea.contains("Grammar", ignoreCase = true) -> SkillCategory.SYNTAX

            skillArea.contains("Logic", ignoreCase = true) ||
            skillArea.contains("Condition", ignoreCase = true) -> SkillCategory.LOGIC

            skillArea.contains("Optimization", ignoreCase = true) ||
            skillArea.contains("Performance", ignoreCase = true) -> SkillCategory.CODE_OPTIMIZATION

            else -> SkillCategory.PROBLEM_SOLVING
        }
    }

    /**
     * Calculate urgency based on time factors
     */
    private fun calculateUrgency(weakArea: WeakArea): UrgencyLevel {
        val daysSinceDetection = (Clock.System.now().toEpochMilliseconds() - weakArea.lastEncountered) / (24 * 60 * 60 * 1000)

        return when {
            weakArea.improvementTrend == TrendDirection.DECLINING && daysSinceDetection <= 3 -> UrgencyLevel.IMMEDIATE
            weakArea.severityScore >= 0.8 && daysSinceDetection <= 7 -> UrgencyLevel.HIGH
            daysSinceDetection <= 14 -> UrgencyLevel.MEDIUM
            else -> UrgencyLevel.LOW
        }
    }

    /**
     * Estimate difficulty of improving this weak area
     */
    private fun calculateImprovementDifficulty(weakArea: WeakArea): ImprovementDifficulty {
        // Complex categories are harder to improve
        val skillCategory = categorizeSkill(weakArea.skillArea)
        val complexityMultiplier = when (skillCategory) {
            SkillCategory.SYSTEM_DESIGN -> 1.5
            SkillCategory.ALGORITHMS -> 1.3
            SkillCategory.CODE_OPTIMIZATION -> 1.2
            SkillCategory.DEBUGGING -> 1.1
            SkillCategory.DATA_STRUCTURES -> 1.0
            else -> 0.9
        }

        // Higher severity means more work needed
        val severityMultiplier = weakArea.severityScore

        // Declining trend indicates stubborn problem
        val trendMultiplier = when (weakArea.improvementTrend) {
            TrendDirection.DECLINING -> 1.3
            TrendDirection.STABLE -> 1.1
            TrendDirection.IMPROVING -> 0.8
            TrendDirection.UNKNOWN -> 1.0
        }

        val difficultyScore = (complexityMultiplier * severityMultiplier * trendMultiplier).coerceIn(0.5, 2.0)

        val level = when {
            difficultyScore >= 1.6 -> DifficultyLevel.EXPERT
            difficultyScore >= 1.3 -> DifficultyLevel.HARD
            difficultyScore >= 1.0 -> DifficultyLevel.MEDIUM
            difficultyScore >= 0.8 -> DifficultyLevel.EASY
            else -> DifficultyLevel.BEGINNER
        }

        return ImprovementDifficulty(level, difficultyScore)
    }

    /**
     * Generate specific recommended actions for improvement
     */
    private suspend fun generateRecommendedActions(
        weakArea: WeakArea,
        priority: Priority
    ): List<RecommendedAction> {
        val actions = mutableListOf<RecommendedAction>()

        // Priority-based actions
        when (priority.level) {
            PriorityLevel.CRITICAL -> {
                actions.add(RecommendedAction(
                    id = generateUUID(),
                    type = ActionType.INTENSIVE_PRACTICE,
                    title = "Intensive ${weakArea.skillArea} Practice",
                    description = "Dedicate 30 minutes daily to ${weakArea.skillArea} focused practice",
                    estimatedTime = 30,
                    priority = 1
                ))
            }
            PriorityLevel.HIGH -> {
                actions.add(RecommendedAction(
                    id = generateUUID(),
                    type = ActionType.TARGETED_PRACTICE,
                    title = "Targeted ${weakArea.skillArea} Challenges",
                    description = "Complete 5 ${weakArea.skillArea} challenges this week",
                    estimatedTime = 20,
                    priority = 2
                ))
            }
            else -> {
                actions.add(RecommendedAction(
                    id = generateUUID(),
                    type = ActionType.GRADUAL_IMPROVEMENT,
                    title = "Gradual ${weakArea.skillArea} Practice",
                    description = "Include ${weakArea.skillArea} practice in your regular routine",
                    estimatedTime = 15,
                    priority = 3
                ))
            }
        }

        // Category-specific actions
        val skillCategory = categorizeSkill(weakArea.skillArea)
        when (skillCategory) {
            SkillCategory.DATA_STRUCTURES -> {
                actions.add(RecommendedAction(
                    id = generateUUID(),
                    type = ActionType.CONCEPTUAL_REVIEW,
                    title = "Data Structure Fundamentals Review",
                    description = "Review the theoretical foundations of ${weakArea.skillArea}",
                    estimatedTime = 25,
                    priority = 2
                ))
            }
            SkillCategory.DEBUGGING -> {
                actions.add(RecommendedAction(
                    id = generateUUID(),
                    type = ActionType.TOOL_PRACTICE,
                    title = "Debugging Tools Practice",
                    description = "Practice using debugging tools and techniques",
                    estimatedTime = 20,
                    priority = 2
                ))
            }
            else -> {
                actions.add(RecommendedAction(
                    id = generateUUID(),
                    type = ActionType.PRACTICE_PROBLEMS,
                    title = "${weakArea.skillArea} Practice Problems",
                    description = "Solve problems specifically targeting ${weakArea.skillArea}",
                    estimatedTime = 15,
                    priority = 3
                ))
            }
        }

        return actions.sortedBy { it.priority }
    }

    /**
     * Generate priority explanation
     */
    private fun generatePriorityReason(weakArea: WeakArea, priority: Priority): String {
        return when (priority.level) {
            PriorityLevel.CRITICAL -> "Critical: ${weakArea.skillArea} is severely impacting your progress with ${(weakArea.severityScore * 100).toInt()}% severity score"
            PriorityLevel.HIGH -> "High Priority: ${weakArea.skillArea} needs attention - you've struggled with it ${weakArea.frequencyCount} times recently"
            PriorityLevel.MEDIUM -> "Medium Priority: ${weakArea.skillArea} shows room for improvement with ${weakArea.improvementTrend.name.lowercase()} trend"
            PriorityLevel.LOW -> "Low Priority: ${weakArea.skillArea} can be improved gradually as part of regular practice"
        }
    }

    /**
     * Estimate time needed for improvement
     */
    private fun estimateImprovementTime(
        weakArea: WeakArea,
        difficulty: ImprovementDifficulty
    ): EstimatedTime {
        val skillCategory = categorizeSkill(weakArea.skillArea)
        val baseDays = when (skillCategory) {
            SkillCategory.SYNTAX -> 3
            SkillCategory.LOGIC -> 7
            SkillCategory.DATA_STRUCTURES -> 14
            SkillCategory.ALGORITHMS -> 21
            SkillCategory.DEBUGGING -> 10
            SkillCategory.SYSTEM_DESIGN -> 30
            else -> 14
        }

        val adjustedDays = (baseDays * difficulty.score * weakArea.severityScore).roundToInt()
        val practiceHours = adjustedDays * 0.5 // 30 min per day average

        return EstimatedTime(adjustedDays, practiceHours.toInt())
    }

    /**
     * Generate next milestone for improvement
     */
    private fun generateNextMilestone(weakArea: WeakArea): ImprovementMilestone {
        val targetImprovement = when {
            weakArea.severityScore >= 0.8 -> 0.3 // 30% improvement for severe cases
            weakArea.severityScore >= 0.6 -> 0.25 // 25% improvement
            else -> 0.2 // 20% improvement
        }

        val targetSuccessRate = minOf(0.9, weakArea.averageAttempts / weakArea.averageAttempts + targetImprovement)

        return ImprovementMilestone(
            id = generateUUID(),
            skillArea = weakArea.skillArea,
            currentSuccessRate = 1.0 - weakArea.severityScore,
            targetSuccessRate = targetSuccessRate,
            targetDate = Clock.System.now().toEpochMilliseconds() + (14 * 24 * 60 * 60 * 1000), // 2 weeks
            description = "Achieve ${(targetSuccessRate * 100).toInt()}% success rate in ${weakArea.skillArea}",
            isAchieved = false
        )
    }

    /**
     * Get skill-based challenge recommendations
     */
    suspend fun getSkillBasedChallengeRecommendations(
        userId: String,
        limit: Int = 5
    ): List<SkillBasedRecommendation> = withContext(Dispatchers.Default) {
        val prioritizedAreas = getPrioritizedWeakAreas(userId)
        val recommendations = mutableListOf<SkillBasedRecommendation>()

        prioritizedAreas.take(3).forEach { prioritizedArea ->
            val weakArea = prioritizedArea.weakArea

            // Generate difficulty-appropriate challenges
            val recommendedDifficulty = when (prioritizedArea.priority.level) {
                PriorityLevel.CRITICAL -> DifficultyLevel.EASY // Start easier for critical areas
                PriorityLevel.HIGH -> DifficultyLevel.MEDIUM
                else -> DifficultyLevel.MEDIUM
            }

            recommendations.add(SkillBasedRecommendation(
                id = generateUUID(),
                skillArea = weakArea.skillArea,
                category = categorizeSkill(weakArea.skillArea),
                recommendedDifficulty = recommendedDifficulty,
                priority = prioritizedArea.priority.level,
                reason = "Focus on ${weakArea.skillArea} to address ${prioritizedArea.priority.level.name.lowercase()} priority weakness",
                estimatedImprovementImpact = prioritizedArea.learningImpact.score,
                challengeCount = when (prioritizedArea.priority.level) {
                    PriorityLevel.CRITICAL -> 3
                    PriorityLevel.HIGH -> 2
                    else -> 1
                }
            ))
        }

        return@withContext recommendations.take(limit)
    }

    /**
     * Track improvement progress
     */
    suspend fun trackImprovementProgress(userId: String): ImprovementProgressReport = withContext(Dispatchers.Default) {
        val currentWeakAreas = weakAreaDetectionService.detectWeakAreas(userId)
        val historicalData = getHistoricalWeakAreaData(userId) // Would need to be implemented

        val improvements = mutableListOf<SkillImprovement>()
        val stagnantAreas = mutableListOf<WeakArea>()
        val newWeakAreas = mutableListOf<WeakArea>()

        // Analyze improvements (simplified for now)
        currentWeakAreas.forEach { weakArea ->
            when (weakArea.improvementTrend) {
                TrendDirection.IMPROVING -> {
                    improvements.add(SkillImprovement(
                        skillArea = weakArea.skillArea,
                        improvementRate = 0.15, // Would calculate from historical data
                        previousSeverity = weakArea.severityScore + 0.15,
                        currentSeverity = weakArea.severityScore,
                        daysToImprove = 7
                    ))
                }
                TrendDirection.STABLE -> stagnantAreas.add(weakArea)
                TrendDirection.DECLINING -> newWeakAreas.add(weakArea)
                else -> {}
            }
        }

        return@withContext ImprovementProgressReport(
            userId = userId,
            reportDate = Clock.System.now().toEpochMilliseconds(),
            totalWeakAreas = currentWeakAreas.size,
            improvements = improvements,
            stagnantAreas = stagnantAreas,
            newWeakAreas = newWeakAreas,
            overallProgressScore = calculateOverallProgress(improvements, stagnantAreas, newWeakAreas),
            nextRecommendations = getSkillBasedChallengeRecommendations(userId, 3)
        )
    }

    private fun calculateOverallProgress(
        improvements: List<SkillImprovement>,
        stagnantAreas: List<WeakArea>,
        newWeakAreas: List<WeakArea>
    ): Double {
        val improvementScore = improvements.size * 0.3
        val stagnationPenalty = stagnantAreas.size * 0.1
        val regressionPenalty = newWeakAreas.size * 0.2

        return (improvementScore - stagnationPenalty - regressionPenalty).coerceIn(-1.0, 1.0)
    }

    // Placeholder for historical data - would be implemented with database queries
    private suspend fun getHistoricalWeakAreaData(userId: String): List<WeakArea> {
        // TODO: Implement historical weak area data retrieval
        return emptyList()
    }
}
