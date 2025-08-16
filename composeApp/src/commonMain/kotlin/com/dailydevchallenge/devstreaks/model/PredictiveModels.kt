package com.dailydevchallenge.devstreaks.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

/**
 * Predictive analytics models for learning predictions and recommendations
 */

/**
 * Comprehensive learning predictions for a user
 */
@Serializable
data class LearningPredictions(
    val userId: String,
    val performanceTrajectory: PerformanceTrajectory,
    val difficultyProgression: DifficultyProgression,
    val plateauRisk: PlateauRisk,
    val recommendations: List<LearningRecommendation>,
    val skillMasteryTimeline: Map<String, SkillMasteryPrediction>,
    val confidenceScore: Double,
    val createdAt: Instant,
    val validUntil: Instant
)

/**
 * Performance trajectory prediction
 */
@Serializable
data class PerformanceTrajectory(
    val trend: TrendDirection,
    val confidenceScore: Double,
    val expectedGrowthRate: Double
)

/**
 * Difficulty progression details
 */
@Serializable
data class DifficultyProgression(
    val currentOptimal: DifficultyLevel,
    val nextMilestone: DifficultyLevel,
    val progressionTimelineDays: Int
)

/**
 * Plateau risk assessment
 */
@Serializable
data class PlateauRisk(
    val riskLevel: RiskLevel,
    val timeToPlateauDays: Int,
    val preventionStrategies: List<String>
)

/**
 * Learning recommendation
 */
@Serializable
data class LearningRecommendation(
    val type: RecommendationType,
    val priority: RecommendationPriority,
    val description: String,
    val expectedImpact: String
)

/**
 * Recommendation type
 */
@Serializable
enum class RecommendationType {
    CHALLENGE_TYPE,
    DIFFICULTY_ADJUSTMENT,
    SCHEDULE_CHANGE,
    FOCUS_AREA,
    LEARNING_METHOD
}

/**
 * Recommendation priority
 */
@Serializable
enum class RecommendationPriority {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Predictive insights for user dashboard
 */
@Serializable
data class PredictiveInsights(
    val userId: String,
    val insights: List<PredictiveLearningInsight>,
    val recommendations: List<ActionableRecommendation>,
    val warnings: List<LearningWarning>,
    val opportunities: List<LearningOpportunity>,
    val confidence: Float,
    val generatedAt: Instant
)

/**
 * Individual predictive learning insight
 */
@Serializable
data class PredictiveLearningInsight(
    val id: String,
    val type: PredictiveInsightType,
    val title: String,
    val description: String,
    val impact: InsightImpact,
    val actionable: Boolean,
    val dataPoints: List<String>
)

@Serializable
enum class PredictiveInsightType {
    PERFORMANCE_PATTERN,
    LEARNING_EFFICIENCY,
    ENGAGEMENT_TREND,
    SKILL_PROGRESSION,
    TIME_OPTIMIZATION,
    KNOWLEDGE_GAP
}

@Serializable
enum class InsightImpact {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Actionable recommendation based on predictions
 */
@Serializable
data class ActionableRecommendation(
    val id: String,
    val title: String,
    val description: String,
    val actionType: RecommendationAction,
    val priority: Int, // 1-10
    val estimatedImpact: String,
    val timeToImplement: Int, // minutes
    val category: RecommendationCategory
)

@Serializable
enum class RecommendationAction {
    ADJUST_DIFFICULTY,
    CHANGE_SCHEDULE,
    FOCUS_WEAK_AREA,
    TAKE_BREAK,
    JOIN_STUDY_GROUP,
    REVIEW_CONCEPT,
    PRACTICE_MORE,
    TRY_NEW_FORMAT
}

@Serializable
enum class RecommendationCategory {
    IMMEDIATE,
    THIS_WEEK,
    THIS_MONTH,
    LONG_TERM
}

/**
 * Learning warning for potential issues
 */
@Serializable
data class LearningWarning(
    val id: String,
    val type: WarningType,
    val severity: WarningSeverity,
    val message: String,
    val suggestedAction: String,
    val predictedTimeframe: String
)

@Serializable
enum class WarningType {
    BURNOUT_RISK,
    DIFFICULTY_TOO_HIGH,
    ENGAGEMENT_DROPPING,
    KNOWLEDGE_GAP_WIDENING,
    SCHEDULE_UNSUSTAINABLE
}

@Serializable
enum class WarningSeverity {
    INFO, WARNING, URGENT, CRITICAL
}

/**
 * Learning opportunity identification
 */
@Serializable
data class LearningOpportunity(
    val id: String,
    val title: String,
    val description: String,
    val type: OpportunityType,
    val potentialBenefit: String,
    val timeRequirement: Int, // minutes
    val confidence: Float
)

@Serializable
enum class OpportunityType {
    SKILL_BREAKTHROUGH,
    EFFICIENCY_IMPROVEMENT,
    MOTIVATION_BOOST,
    SOCIAL_LEARNING,
    KNOWLEDGE_CONNECTION,
    MASTERY_ACCELERATION
}

/**
 * Performance prediction model
 */
@Serializable
data class PerformancePrediction(
    val userId: String,
    val challengeId: String,
    val predictedScore: Float,
    val predictedAttempts: Int,
    val predictedDuration: Int, // minutes
    val confidenceLevel: Float,
    val riskFactors: List<String>,
    val successFactors: List<String>,
    val generatedAt: Instant
)

@Serializable
enum class SessionPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

/**
 * Challenge prediction with detailed analysis - Missing model needed by ViewModel
 */
@Serializable
data class ChallengePrediction(
    val challengeType: String,
    val difficulty: DifficultyLevel,
    val skillFocus: List<String>,
    val estimatedDuration: Int, // minutes
    val successProbability: Double,
    val learningObjectives: List<String>,
    val reasoning: String = "",
    val confidence: Double = 0.0
)

/**
 * Performance trends analysis - Missing model needed by ViewModel
 */
@Serializable
data class PerformanceTrends(
    val userId: String,
    val overallTrend: TrendDirection,
    val predictedScoreChange: Double,
    val plateauRisk: Double,
    val confidenceLevel: Double,
    val trendFactors: List<String> = emptyList(),
    val createdAt: Instant
)


/**
 * Learning schedule with weekly breakdown - Enhanced version needed by ViewModel
 */
@Serializable
data class LearningSchedule(
    val userId: String,
    val weeklySchedule: List<DaySchedule>,
    val priorityAreas: List<String>,
    val difficultyProgression: String,
    val createdAt: Instant
)

/**
 * Daily schedule breakdown
 */
@Serializable
data class DaySchedule(
    val day: String,
    val sessions: List<LearningSession>
)

/**
 * Individual learning session
 */
@Serializable
data class LearningSession(
    val startTime: String,
    val duration: Int,
    val challengeType: String,
    val difficulty: DifficultyLevel
)

/**
 * Skill mastery prediction - Missing model needed by ViewModel
 */
@Serializable
data class SkillMasteryPrediction(
    val skillArea: String,
    val currentLevel: MasteryLevel,
    val predictedMasteryDate: Instant,
    val confidenceScore: Double,
    val recommendedActions: List<String>,
    val estimatedHoursToMastery: Int,
    val createdAt: Instant
)

/**
 * Learning context for predictions
 */
@Serializable
data class LearningContext(
    val currentFocus: String,
    val recentActivity: String,
    val timeOfDay: String,
    val availableTime: Int = 30
)

/**
 * Risk level enumeration
 */
@Serializable
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}
