package com.dailydevchallenge.devstreaks.service

import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import com.dailydevchallenge.devstreaks.utils.generateUUID
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Enhanced AI Coaching Service with deep personalization based on user performance,
 * weak areas, and learning patterns
 */
class PersonalizedAICoachingService(
    private val challengeRepository: ChallengeRepository,
    private val weakAreaDetectionService: WeakAreaDetectionService
) {
    private val logger = { getLogger() }

    /**
     * Generate personalized coaching response based on comprehensive user context
     */
    suspend fun generatePersonalizedResponse(
        userMessage: String,
        userId: String,
        learningProfile: LearningProfile?
    ): PersonalizedCoachingResponse = withContext(Dispatchers.Default) {

        // Build comprehensive coaching context
        val context = buildPersonalizedContext(userId, learningProfile)

        // Determine optimal coaching style based on user state
        val coachingStyle = determineOptimalCoachingStyle(context)

        // Generate context-aware response
        val response = generateContextAwareResponse(userMessage, context, coachingStyle)

        // Extract actionable recommendations
        val recommendations = generatePersonalizedRecommendations(context)

        // Create motivational boost based on current state
        val motivationalBoost = generateMotivationalBoost(context)

        // Provide next challenge hint if appropriate
        val nextChallengeHint = generateNextChallengeHint(context)

        // Generate personalized insight
        val personalizedInsight = generatePersonalizedInsight(context)

        logger().d("PersonalizedAICoaching",
            "Generated response for user $userId: Style=$coachingStyle, " +
            "Weak areas=${context.topWeakAreas.size}, State=${context.motivationalState}")

        PersonalizedCoachingResponse(
            responseText = response,
            coachingStyle = coachingStyle,
            personalizedRecommendations = recommendations,
            relevantWeakAreas = context.topWeakAreas.map { it.skillArea },
            confidenceLevel = 0.85,
            contextAwareness = "Based on ${context.topWeakAreas.size} identified weak areas and ${context.motivationalState} state",
            motivationalBoost = motivationalBoost,
            nextChallengeHint = nextChallengeHint,
            personalizedInsight = personalizedInsight
        )
    }

    /**
     * Build comprehensive personalized coaching context
     */
    suspend fun buildPersonalizedContext(
        userId: String,
        learningProfile: LearningProfile?
    ): PersonalizedCoachingContext {
        val performanceSummary = challengeRepository.calculateUserPerformanceSummary(userId)
        val weakAreas = weakAreaDetectionService.detectWeakAreas(userId).take(3) // Top 3 weak areas
        val difficultyAdjustment = challengeRepository.getDifficultyAdjustment(userId)
        val (xp, streak) = challengeRepository.getXPAndStreak()

        // Determine motivational state based on recent performance
        val motivationalState = determineMotivationalState(performanceSummary, weakAreas)

        // Determine preferred coaching style from profile or infer from performance
        val coachingStyle = learningProfile?.let {
            inferCoachingStyleFromProfile(it, motivationalState)
        } ?: inferCoachingStyleFromPerformance(performanceSummary, motivationalState)

        // Generate recent achievements
        val recentAchievements = generateRecentAchievements(performanceSummary, streak, xp)

        // Create context summary
        val contextSummary = generateContextSummary(performanceSummary, weakAreas, streak)

        return PersonalizedCoachingContext(
            userId = userId,
            currentDifficultyLevel = difficultyAdjustment.recommendedLevel,
            recentPerformance = performanceSummary,
            topWeakAreas = weakAreas,
            recentAchievements = recentAchievements,
            learningStreak = streak,
            motivationalState = motivationalState,
            preferredCoachingStyle = coachingStyle,
            lastInteractionTime = Clock.System.now().toEpochMilliseconds(),
            contextSummary = contextSummary
        )
    }

    /**
     * Determine user's current motivational state based on performance patterns
     */
    private fun determineMotivationalState(
        performance: UserPerformanceSummary,
        weakAreas: List<WeakArea>
    ): MotivationalState {
        return when {
            performance.successRate >= 0.85 && performance.averageAttempts <= 1.5 -> MotivationalState.CONFIDENT
            performance.successRate < 0.5 && weakAreas.size >= 2 -> MotivationalState.STRUGGLING
            performance.successRate >= 0.7 && weakAreas.any { it.trend == WeakAreaTrend.IMPROVING } -> MotivationalState.IMPROVING
            performance.successRate in 0.6..0.75 && performance.recentTaskCount >= 5 -> MotivationalState.PLATEAU
            performance.averageAttempts >= 3.0 && performance.successRate < 0.6 -> MotivationalState.FRUSTRATED
            else -> MotivationalState.MOTIVATED
        }
    }

    /**
     * Determine optimal coaching style for current user state
     */
    private fun determineOptimalCoachingStyle(context: PersonalizedCoachingContext): CoachingStyle {
        return when (context.motivationalState) {
            MotivationalState.CONFIDENT -> CoachingStyle.CHALLENGING
            MotivationalState.STRUGGLING, MotivationalState.FRUSTRATED -> CoachingStyle.PATIENT
            MotivationalState.IMPROVING, MotivationalState.MOTIVATED -> CoachingStyle.ENCOURAGING
            MotivationalState.PLATEAU -> CoachingStyle.ANALYTICAL
        }
    }

    /**
     * Generate context-aware response that references user's specific situation
     */
    private fun generateContextAwareResponse(
        userMessage: String,
        context: PersonalizedCoachingContext,
        style: CoachingStyle
    ): String {
        val weakAreaContext = if (context.topWeakAreas.isNotEmpty()) {
            "I've noticed you've been working on ${context.topWeakAreas.first().skillArea} recently. "
        } else ""

        val performanceContext = when (context.motivationalState) {
            MotivationalState.CONFIDENT -> "You've been crushing your challenges with a ${(context.recentPerformance.successRate * 100).toInt()}% success rate! "
            MotivationalState.STRUGGLING -> "I see you've been facing some challenges lately. That's completely normal in your learning journey. "
            MotivationalState.IMPROVING -> "Great progress! I can see your performance trending upward. "
            MotivationalState.PLATEAU -> "You've been consistent with your ${context.learningStreak}-day streak. "
            MotivationalState.FRUSTRATED -> "I understand coding can be frustrating sometimes. Let's work through this together. "
            MotivationalState.MOTIVATED -> "Your ${context.learningStreak}-day streak shows real dedication! "
        }

        val styleBasedResponse = when (style) {
            CoachingStyle.ENCOURAGING -> {
                "🌟 $performanceContext$weakAreaContext$userMessage - you're doing great! Keep building on this momentum."
            }
            CoachingStyle.PATIENT -> {
                "💙 $performanceContext$weakAreaContext Remember, every expert was once a beginner. Let's tackle $userMessage step by step."
            }
            CoachingStyle.CHALLENGING -> {
                "🎯 $performanceContext$weakAreaContext Ready for the next level? Let's dive deeper into $userMessage and push your skills further."
            }
            CoachingStyle.ANALYTICAL -> {
                "📊 $performanceContext$weakAreaContext Let me analyze your question about $userMessage and provide data-driven insights."
            }
            CoachingStyle.PRACTICAL -> {
                "🔧 $performanceContext$weakAreaContext Here's a practical approach to $userMessage based on your current progress."
            }
            CoachingStyle.MOTIVATIONAL -> {
                "🚀 $performanceContext$weakAreaContext Let's supercharge your learning with $userMessage! You've got this!"
            }
            CoachingStyle.STRATEGIC -> {
                "🎯 $performanceContext$weakAreaContext Here's a strategic approach to mastering $userMessage based on your learning path."
            }
            CoachingStyle.SUPPORTIVE -> {
                "🤝 $performanceContext$weakAreaContext I'm here to support you through $userMessage. We'll take it one step at a time."
            }
        }

        return styleBasedResponse
    }

    /**
     * Generate personalized action recommendations
     */
    private suspend fun generatePersonalizedRecommendations(
        context: PersonalizedCoachingContext
    ): List<String> {
        val recommendations = mutableListOf<String>()

        // Add weak area specific recommendations
        context.topWeakAreas.take(2).forEach { weakArea ->
            val areaRecommendations = weakAreaDetectionService.generateImprovementRecommendations(weakArea)
            recommendations.addAll(areaRecommendations.take(1))
        }

        // Add difficulty-based recommendations
        when (context.currentDifficultyLevel) {
            DifficultyLevel.BEGINNER -> recommendations.add("Focus on understanding basic concepts before moving to complex problems")
            DifficultyLevel.EXPERT -> recommendations.add("Challenge yourself with optimization problems and edge cases")
            else -> recommendations.add("Continue practicing at your current pace to build consistency")
        }

        // Add motivational state specific recommendations
        when (context.motivationalState) {
            MotivationalState.PLATEAU -> recommendations.add("Try a different type of challenge to break through the plateau")
            MotivationalState.FRUSTRATED -> recommendations.add("Take a short break and come back with fresh perspective")
            MotivationalState.CONFIDENT -> recommendations.add("Consider mentoring others or tackling advanced topics")
            else -> recommendations.add("Keep up the consistent practice routine")
        }

        return recommendations.take(3)
    }

    /**
     * Generate motivational boost based on user's current state
     */
    private fun generateMotivationalBoost(context: PersonalizedCoachingContext): String? {
        return when (context.motivationalState) {
            MotivationalState.STRUGGLING ->
                "💪 Remember: every bug you fix makes you stronger. Your ${context.learningStreak}-day commitment shows real determination!"
            MotivationalState.IMPROVING ->
                "🚀 Your improvement trajectory is impressive! Keep this momentum going."
            MotivationalState.CONFIDENT ->
                "🏆 Your ${(context.recentPerformance.successRate * 100).toInt()}% success rate puts you in the top tier. You're ready for bigger challenges!"
            MotivationalState.FRUSTRATED ->
                "🌈 Frustration is a sign you're pushing your boundaries. That's where real growth happens."
            else -> null
        }
    }

    /**
     * Generate hint for next challenge based on weak areas
     */
    private fun generateNextChallengeHint(context: PersonalizedCoachingContext): String? {
        val primaryWeakArea = context.topWeakAreas.firstOrNull()
        return primaryWeakArea?.let {
            "💡 Your next challenge will focus on ${it.skillArea}. Review the fundamentals first for better success!"
        }
    }

    /**
     * Generate personalized insight based on user's data
     */
    private fun generatePersonalizedInsight(context: PersonalizedCoachingContext): String {
        val avgTime = context.recentPerformance.averageCompletionTime / 60000 // Convert to minutes
        val insight = when {
            context.recentPerformance.averageAttempts > 3 ->
                "I notice you typically need ${context.recentPerformance.averageAttempts.toInt()} attempts per challenge. Consider slowing down to plan your approach first."
            avgTime > 20 ->
                "You spend about ${avgTime.toInt()} minutes per challenge. This thorough approach often leads to deeper understanding."
            context.recentPerformance.successRate > 0.8 ->
                "Your ${(context.recentPerformance.successRate * 100).toInt()}% success rate suggests you're ready for more advanced challenges."
            else ->
                "Your learning pattern shows steady progress. Consistency is key to mastering programming concepts."
        }
        return insight
    }

    private fun inferCoachingStyleFromProfile(
        profile: LearningProfile,
        state: MotivationalState
    ): CoachingStyle {
        return when (profile.style.lowercase()) {
            "visual" -> CoachingStyle.ANALYTICAL
            "hands-on" -> CoachingStyle.PRACTICAL
            "step-by-step" -> CoachingStyle.PATIENT
            else -> when (state) {
                MotivationalState.CONFIDENT -> CoachingStyle.CHALLENGING
                MotivationalState.STRUGGLING -> CoachingStyle.ENCOURAGING
                else -> CoachingStyle.PRACTICAL
            }
        }
    }

    private fun inferCoachingStyleFromPerformance(
        performance: UserPerformanceSummary,
        state: MotivationalState
    ): CoachingStyle {
        return when {
            performance.successRate >= 0.8 -> CoachingStyle.CHALLENGING
            performance.successRate <= 0.5 -> CoachingStyle.PATIENT
            performance.averageAttempts >= 3 -> CoachingStyle.ENCOURAGING
            else -> CoachingStyle.PRACTICAL
        }
    }

    private fun generateRecentAchievements(
        performance: UserPerformanceSummary,
        streak: Int,
        xp: Int
    ): List<Achievement> {
        val achievements = mutableListOf<Achievement>()

        if (streak >= 7) {
            achievements.add(Achievement(
                generateUUID(),
                "Week Warrior",
                "Maintained a 7-day learning streak",
                Clock.System.now().toEpochMilliseconds(),
                "Consistency",
                50
            ))
        }

        if (performance.successRate >= 0.9) {
            achievements.add(Achievement(
                generateUUID(),
                "Accuracy Master",
                "Achieved 90%+ success rate",
                Clock.System.now().toEpochMilliseconds(),
                "Performance",
                75
            ))
        }

        return achievements
    }

    private fun generateContextSummary(
        performance: UserPerformanceSummary,
        weakAreas: List<WeakArea>,
        streak: Int
    ): String {
        return buildString {
            append("User has ${performance.recentTaskCount} recent completions ")
            append("with ${(performance.successRate * 100).toInt()}% success rate. ")
            if (weakAreas.isNotEmpty()) {
                append("Primary focus areas: ${weakAreas.take(2).joinToString { it.skillArea }}. ")
            }
            append("Current streak: $streak days.")
        }
    }
}
