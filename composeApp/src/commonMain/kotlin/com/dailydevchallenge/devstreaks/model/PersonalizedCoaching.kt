package com.dailydevchallenge.devstreaks.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.*

/**
 * Personalized AI coaching models
 * Note: WeakArea is defined in AdaptiveModels.kt to avoid redeclaration
 */

@Serializable
enum class SkillCategory {
    DATA_STRUCTURES,
    ALGORITHMS,
    SYSTEM_DESIGN,
    DEBUGGING,
    CODE_OPTIMIZATION,
    PROBLEM_SOLVING,
    SYNTAX,
    LOGIC,
    TESTING
}

@Serializable
enum class TrendDirection {
    IMPROVING,
    DECLINING,
    STABLE,
    UNKNOWN
}

/**
 * Personalized coaching context that combines performance data with user history
 */
@Serializable
data class PersonalizedCoachingContext(
    val userId: String,
    val currentDifficultyLevel: DifficultyLevel,
    val recentPerformance: UserPerformanceSummary,
    val topWeakAreas: List<WeakArea>,
    val recentAchievements: List<Achievement>,
    val learningStreak: Int,
    val motivationalState: MotivationalState,
    val preferredCoachingStyle: CoachingStyle,
    val lastInteractionTime: Long,
    val contextSummary: String
)

@Serializable
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val achievedDate: Long,
    val category: String,
    val xpEarned: Int
)

@Serializable
enum class MotivationalState {
    CONFIDENT, // High success rate, fast progress
    STRUGGLING, // Low success rate, many failures
    IMPROVING, // Recent positive trend
    PLATEAU, // Stable but not progressing
    FRUSTRATED, // Multiple failed attempts recently
    MOTIVATED // Recent achievements or milestones
}

@Serializable
enum class CoachingStyle {
    ENCOURAGING, // Focus on motivation and positive reinforcement
    ANALYTICAL, // Data-driven insights and detailed analysis
    PRACTICAL, // Action-oriented with specific next steps
    PATIENT, // Gentle guidance for struggling learners
    CHALLENGING, // Push harder for confident learners
    MOTIVATIONAL, // High-energy motivation
    STRATEGIC, // Long-term strategy focused
    SUPPORTIVE // Emotional support focused
}

/**
 * AI coaching recommendation with personalized insights
 */
@Serializable
data class PersonalizedCoachingResponse(
    val responseText: String,
    val coachingStyle: CoachingStyle,
    val personalizedRecommendations: List<String>,
    val relevantWeakAreas: List<String>,
    val confidenceLevel: Double,
    val contextAwareness: String,
    val motivationalBoost: String? = null,
    val response: String = responseText, // Alias for backward compatibility
    val recommendations: List<String> = personalizedRecommendations, // Alias for backward compatibility
    val weakAreaFocus: List<String> = relevantWeakAreas, // Alias for backward compatibility
    val nextChallengeHint: String? = null,
    val personalizedInsight: String? = null,
    val learningTips: List<String> = emptyList(),
    val motivationalMessage: String? = motivationalBoost,
    val strategicGuidance: List<String> = personalizedRecommendations
)
