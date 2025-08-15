package com.dailydevchallenge.devstreaks.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.serialization.Contextual

/**
 * Supporting data models for the Adaptive Intelligence system
 */

@Serializable
data class AdaptiveChallengeConfig(
    val originalTask: ChallengeTask,
    val adaptedTask: ChallengeTask,
    val difficultyAdjustment: DifficultyAdjustment,
    val xpMultiplier: Double,
    val personalizedHints: List<PersonalizedHint>,
    val userContext: UserAdaptiveContext,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val recommendedDifficulty: DifficultyLevel = DifficultyLevel.MEDIUM,
    val shouldShowPerformanceInsights: Boolean = false,
    val focusAreas: List<String> = emptyList(),
    val weakAreas: List<WeakArea> = emptyList(),
    val bonusXP: Int = 0,
    val adaptiveXpMultiplier: Double,
    val preloadedHints: List<String> = emptyList()
)
@Serializable
data class UserAdaptiveContext(
    val userId: String,
    val performanceSummary: UserPerformanceSummary,
    val weakAreas: List<WeakArea>,
    val recentChallenges: List<ChallengeTask>,
    @Contextual val learningProfile: com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile?,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

@Serializable
data class PersonalizedHint(
    val originalHint: String,
    val personalizedText: String,
    val relevanceScore: Double,
    val unlockThreshold: Int
)


data class RealTimeAdaptiveResponse(
    val sessionAnalysis: SessionPerformanceAnalysis,
    val emergingInsights: List<EmergingInsight>,
    val coachingIntervention: CoachingIntervention,
    val midSessionAdjustment: DifficultyAdjustment?,
    val hintRecommendation: HintRecommendation,
    val bonusXP: Int,
    val adaptiveHints: List<String>,
    val motivationalMessage: String?,
    val shouldAdjustDifficulty: Boolean,
    val shouldUpdateCoaching: Boolean,
    val timestamp: Long
)

@Serializable
data class ChallengeSessionMetrics(
    val sessionId: String,
    val userId: String,
    val challengeId: String,
    val elapsedTime: Long,
    val attemptsCount: Int,
    val errorsCount: Int,
    val hintsUsed: Int,
    val currentProgress: Double, // 0.0 to 1.0
    val stuckDuration: Long, // Time spent without progress
    val lastActivityTime: Long,
    val isCompleted: Boolean = false
)

@Serializable
data class SessionPerformanceAnalysis(
    val timeEfficiency: Double, // Compared to expected time
    val errorPattern: String,
    val strugglingAreas: List<String>,
    val overallProgress: Double
)

@Serializable
data class EmergingInsight(
    val type: InsightType,
    val severity: InsightSeverity,
    val description: String,
    val recommendedAction: String,
    val confidence: Double
)

@Serializable
data class CoachingIntervention(
    val type: InterventionType,
    val message: String,
    val urgency: InterventionUrgency,
    val suggestions: List<String>
)

@Serializable
enum class InterventionType {
    ENCOURAGEMENT,
    HINT_SUGGESTION,
    DIFFICULTY_ADJUSTMENT,
    BREAK_REMINDER,
    STRATEGY_GUIDANCE,
    WEAK_AREA_FOCUS
}

@Serializable
enum class InterventionUrgency {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serializable
enum class HintRecommendation {
    NONE,
    GENTLE_NUDGE,
    SPECIFIC_HINT,
    STRATEGIC_GUIDANCE,
    DIRECT_HELP
}

@Serializable
enum class CoachingContextType {
    CHALLENGE_START,
    CHALLENGE_PROGRESS,
    CHALLENGE_STUCK,
    CHALLENGE_COMPLETE,
    GENERAL_QUERY,
    WEAK_AREA_HELP,
    MOTIVATION_BOOST,
    PROGRESS_REVIEW,
    CHALLENGE_GUIDANCE,
    GENERAL_GUIDANCE
}

@Serializable
data class ChallengeCompletionMetrics(
    val challengeId: String,
    val userId: String,
    val sessionId: String,
    val totalTime: Long,
    val attemptsCount: Int,
    val hintsUsed: Int,
    val errorsCount: Int,
    val completed: Boolean,
    val finalScore: Double,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

@Serializable
data class AdaptiveLearningUpdate(
    val performanceMetrics: PerformanceMetrics,
    val weakAreasUpdate: List<WeakArea>,
    val futureDifficultyRecommendation: DifficultyAdjustment,
    val learningInsights: List<LearningInsight>,
    val adaptiveXP: Int,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

@Serializable
data class LearningInsight(
    val type: LearningInsightType,
    val title: String,
    val description: String,
    val actionable: Boolean,
    val priority: Int, // 1-5, higher is more important
    val relatedWeakAreas: List<String>
)

@Serializable
enum class LearningInsightType {
    IMPROVEMENT_DETECTED,
    SKILL_MASTERY,
    WEAKNESS_ADDRESSED,
    LEARNING_PATTERN,
    MOTIVATION_BOOST,
    STRATEGY_RECOMMENDATION
}

@Serializable
data class WeakArea(
    val id: String,
    val userId: String,
    val skillArea: String,
    val severityScore: Double, // 0.0 to 1.0
    val detectedAt: Long,
    val lastOccurrence: Long,
    val occurrenceCount: Int,
    val trend: WeakAreaTrend,
    val recommendedActions: List<String>,
    val frequencyCount: Int = occurrenceCount,
    val lastEncountered: Long = lastOccurrence,
    val averageAttempts: Double = 2.0,
    val averageTimeSpent: Long = 300000L,
    val commonErrors: List<String> = emptyList(),
    val improvementTrend: TrendDirection = TrendDirection.UNKNOWN,
    val confidenceLevel: Double = 0.5
)

@Serializable
enum class WeakAreaTrend {
    IMPROVING,
    STABLE,
    WORSENING,
    NEW,
    DECLINING,
    UNKNOWN
}
