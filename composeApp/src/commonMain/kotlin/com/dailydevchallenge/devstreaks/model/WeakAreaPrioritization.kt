package com.dailydevchallenge.devstreaks.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.*

/**
 * Enhanced models for advanced weak area prioritization and improvement tracking
 */

@Serializable
data class PrioritizedWeakArea(
    val weakArea: WeakArea,
    val priority: Priority,
    val priorityReason: String,
    val learningImpact: LearningImpact,
    val urgencyLevel: UrgencyLevel,
    val improvementDifficulty: ImprovementDifficulty,
    val recommendedActions: List<RecommendedAction>,
    val estimatedImprovementTime: EstimatedTime,
    val nextMilestone: ImprovementMilestone
)

@Serializable
data class Priority(
    val level: PriorityLevel,
    val score: Double // 0.0 to 1.0
)

@Serializable
enum class PriorityLevel(val value: Int, val displayName: String) {
    CRITICAL(4, "Critical"),
    HIGH(3, "High"),
    MEDIUM(2, "Medium"),
    LOW(1, "Low")
}

@Serializable
data class LearningImpact(
    val level: ImpactLevel,
    val score: Double // 0.0 to 1.0 - how much improving this will help overall
)

@Serializable
enum class ImpactLevel(val value: Int, val description: String) {
    TRANSFORMATIVE(4, "Will significantly transform your coding abilities"),
    HIGH(3, "Will noticeably improve your performance"),
    MEDIUM(2, "Will provide moderate improvement"),
    LOW(1, "Will provide minor improvement")
}

@Serializable
enum class UrgencyLevel(val value: Int, val description: String) {
    IMMEDIATE(4, "Needs immediate attention"),
    HIGH(3, "Should be addressed soon"),
    MEDIUM(2, "Can be addressed within weeks"),
    LOW(1, "Can be improved gradually")
}

@Serializable
data class ImprovementDifficulty(
    val level: DifficultyLevel,
    val score: Double // Multiplier for improvement time estimation
)

@Serializable
data class RecommendedAction(
    val id: String,
    val type: ActionType,
    val title: String,
    val description: String,
    val estimatedTime: Int, // minutes
    val priority: Int // 1 = highest
)

@Serializable
enum class ActionType {
    INTENSIVE_PRACTICE,
    TARGETED_PRACTICE,
    GRADUAL_IMPROVEMENT,
    CONCEPTUAL_REVIEW,
    TOOL_PRACTICE,
    PRACTICE_PROBLEMS,
    MENTOR_SESSION,
    VIDEO_TUTORIAL,
    READING_MATERIAL
}

@Serializable
data class EstimatedTime(
    val days: Int,
    val practiceHours: Int
)

@Serializable
data class ImprovementMilestone(
    val id: String,
    val skillArea: String,
    val currentSuccessRate: Double,
    val targetSuccessRate: Double,
    val targetDate: Long, // timestamp
    val description: String,
    val isAchieved: Boolean,
    val achievedDate: Long? = null
)

@Serializable
data class SkillBasedRecommendation(
    val id: String,
    val skillArea: String,
    val category: SkillCategory,
    val recommendedDifficulty: DifficultyLevel,
    val priority: PriorityLevel,
    val reason: String,
    val estimatedImprovementImpact: Double,
    val challengeCount: Int
)

@Serializable
data class ImprovementProgressReport(
    val userId: String,
    val reportDate: Long,
    val totalWeakAreas: Int,
    val improvements: List<SkillImprovement>,
    val stagnantAreas: List<WeakArea>,
    val newWeakAreas: List<WeakArea>,
    val overallProgressScore: Double, // -1.0 to 1.0
    val nextRecommendations: List<SkillBasedRecommendation>
)

@Serializable
data class SkillImprovement(
    val skillArea: String,
    val improvementRate: Double, // percentage improvement
    val previousSeverity: Double,
    val currentSeverity: Double,
    val daysToImprove: Int
)

/**
 * Real-time weak area insight for immediate feedback
 */
@Serializable
data class WeakAreaInsight(
    val id: String,
    val userId: String,
    val skillArea: String,
    val insightType: InsightType,
    val message: String,
    val severity: InsightSeverity,
    val actionable: Boolean,
    val suggestedAction: String?,
    val timestamp: Long,
    val triggerEvent: String // What caused this insight
)

/**
 * Progressive skill mastery tracking
 */
@Serializable
data class SkillMasteryLevel(
    val skillArea: String,
    val category: SkillCategory,
    val currentLevel: MasteryLevel,
    val progressToNext: Double, // 0.0 to 1.0
    val totalChallengesAttempted: Int,
    val totalChallengesCompleted: Int,
    val averagePerformance: Double,
    val lastAssessment: Long,
    val masteryFactors: MasteryFactors
)

@Serializable
enum class MasteryLevel(val value: Int, val title: String, val description: String) {
    NOVICE(1, "Novice", "Learning the basics"),
    BEGINNER(2, "Beginner", "Understanding fundamentals"),
    INTERMEDIATE(3, "Intermediate", "Applying knowledge confidently"),
    ADVANCED(4, "Advanced", "Solving complex problems"),
    EXPERT(5, "Expert", "Mastering advanced concepts")
}

@Serializable
data class MasteryFactors(
    val consistencyScore: Double, // How consistent performance is
    val speedScore: Double, // How quickly problems are solved
    val accuracyScore: Double, // Success rate
    val complexityHandling: Double, // Ability to handle complex problems
    val conceptualUnderstanding: Double // Deep understanding vs memorization
)
