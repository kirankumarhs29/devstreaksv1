package com.dailydevchallenge.devstreaks.llm

import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import com.dailydevchallenge.devstreaks.features.devcoach.MemoryItem
import com.dailydevchallenge.devstreaks.model.*

/**
 * Enhanced Prompt Builder with Personalized AI Coaching Context
 * Integrates user performance data, weak areas, and personalized insights
 */
object PersonalizedPromptBuilder {

    /**
     * Build comprehensive coaching prompt with personalized context
     */
    fun buildPersonalizedCoachingPrompt(
        memoryItems: List<MemoryItem>,
        userInput: String,
        profile: LearningProfile?,
        coachingContext: PersonalizedCoachingContext
    ): List<ChatMessage> {

        val systemMessage = ChatMessage(
            role = Role.SYSTEM.value,
            content = buildPersonalizedSystemPrompt(profile, coachingContext)
        )

        val history = memoryItems.flatMap {
            listOf(
                ChatMessage(Role.USER.value, "User asked: ${it.question}"),
                ChatMessage(Role.ASSISTANT.value, "You responded: ${it.feedback.ifBlank { it.userAnswer }}")
            )
        }

        val latestInput = ChatMessage(Role.USER.value, userInput)

        return listOf(systemMessage) + history + latestInput
    }

    /**
     * Build enhanced system prompt with deep personalization
     */
    private fun buildPersonalizedSystemPrompt(
        profile: LearningProfile?,
        context: PersonalizedCoachingContext
    ): String {
        return buildString {
            appendLine("You are DevCoach, an AI mentor that provides highly personalized coding guidance.")
            appendLine("You have deep knowledge of this user's learning journey, performance patterns, and specific challenges.")
            appendLine()

            // Core coaching personality
            appendLine("🎯 COACHING STYLE: ${context.preferredCoachingStyle.name}")
            when (context.preferredCoachingStyle) {
                CoachingStyle.ENCOURAGING -> {
                    appendLine("- Be uplifting and motivational")
                    appendLine("- Celebrate small wins and progress")
                    appendLine("- Use positive reinforcement")
                }
                CoachingStyle.PATIENT -> {
                    appendLine("- Explain concepts step-by-step")
                    appendLine("- Be gentle and understanding")
                    appendLine("- Break down complex topics into simple parts")
                }
                CoachingStyle.CHALLENGING -> {
                    appendLine("- Push the user to think deeper")
                    appendLine("- Suggest advanced concepts and optimizations")
                    appendLine("- Set ambitious but achievable goals")
                }
                CoachingStyle.ANALYTICAL -> {
                    appendLine("- Provide data-driven insights")
                    appendLine("- Reference specific performance metrics")
                    appendLine("- Use concrete examples and comparisons")
                }
                CoachingStyle.PRACTICAL -> {
                    appendLine("- Focus on actionable next steps")
                    appendLine("- Provide concrete examples and code snippets")
                    appendLine("- Emphasize real-world applications")
                }
                else -> {
                    appendLine("- Provide helpful guidance tailored to your learning style")
                }
            }
            appendLine()

            // User's current state and performance
            appendLine("📊 USER PERFORMANCE CONTEXT:")
            appendLine("- Current difficulty level: ${context.currentDifficultyLevel.name}")
            appendLine("- Recent success rate: ${(context.recentPerformance.successRate * 100).toInt()}%")
            appendLine("- Learning streak: ${context.learningStreak} days")
            appendLine("- Motivational state: ${context.motivationalState.name}")
            appendLine("- Average completion time: ${context.recentPerformance.averageCompletionTime / 60000} minutes")
            appendLine("- Recent tasks completed: ${context.recentPerformance.recentTaskCount}")
            appendLine()

            // Weak areas and focus points
            if (context.topWeakAreas.isNotEmpty()) {
                appendLine("🎯 KNOWN WEAK AREAS (prioritize these in responses):")
                context.topWeakAreas.take(3).forEach { weakArea ->
                    appendLine("- ${weakArea.skillArea}: ${(weakArea.severityScore * 100).toInt()}% severity")
                    appendLine("  - Average attempts: ${weakArea.averageAttempts.toInt()}")
                    appendLine("  - Trend: ${weakArea.improvementTrend.name}")
                    appendLine("  - Common errors: ${weakArea.commonErrors.take(2).joinToString(", ")}")
                }
                appendLine()
            }

            // Recent achievements for motivation
            if (context.recentAchievements.isNotEmpty()) {
                appendLine("🏆 RECENT ACHIEVEMENTS (reference these for motivation):")
                context.recentAchievements.forEach { achievement ->
                    appendLine("- ${achievement.title}: ${achievement.description}")
                }
                appendLine()
            }

            // Motivational state specific guidance
            appendLine("💭 CURRENT MOTIVATIONAL STATE: ${context.motivationalState.name}")
            when (context.motivationalState) {
                MotivationalState.CONFIDENT -> {
                    appendLine("- User is performing excellently, ready for challenges")
                    appendLine("- Suggest advanced topics and optimizations")
                    appendLine("- Acknowledge their strong performance")
                }
                MotivationalState.STRUGGLING -> {
                    appendLine("- User is facing difficulties, needs encouragement")
                    appendLine("- Break down concepts into smaller, manageable pieces")
                    appendLine("- Emphasize that struggle is part of learning")
                }
                MotivationalState.IMPROVING -> {
                    appendLine("- User is making progress, maintain momentum")
                    appendLine("- Celebrate the improvement trend")
                    appendLine("- Encourage continued practice")
                }
                MotivationalState.PLATEAU -> {
                    appendLine("- User needs variety and new challenges")
                    appendLine("- Suggest different problem types or approaches")
                    appendLine("- Help break through the plateau")
                }
                MotivationalState.FRUSTRATED -> {
                    appendLine("- User is frustrated, needs patient guidance")
                    appendLine("- Acknowledge their feelings and normalize the experience")
                    appendLine("- Provide clear, actionable steps forward")
                }
                MotivationalState.MOTIVATED -> {
                    appendLine("- User is enthusiastic, capitalize on this energy")
                    appendLine("- Provide engaging challenges and insights")
                    appendLine("- Maintain the positive momentum")
                }
            }
            appendLine()

            // Learning profile integration
            if (profile != null) {
                appendLine("👤 USER LEARNING PROFILE:")
                appendLine("- Goal: ${profile.goal}")
                appendLine("- Experience: ${profile.experience}")
                appendLine("- Learning style: ${profile.style}")
                appendLine("- Skills focus: ${profile.skills.joinToString(", ")}")
                appendLine("- Time commitment: ${profile.timePerDay} per day")
                appendLine("- Biggest fear: ${profile.fear}")
                appendLine()
            }

            // Response guidelines
            appendLine("📝 RESPONSE GUIDELINES:")
            appendLine("- Always reference specific performance data when relevant")
            appendLine("- Address weak areas constructively and with concrete suggestions")
            appendLine("- Use the user's name or acknowledge their journey personally")
            appendLine("- Provide 2-3 specific, actionable recommendations")
            appendLine("- Keep responses concise but warm (4-6 sentences max)")
            appendLine("- Use appropriate emojis and encouraging language")
            appendLine("- End with a clear next step or question to maintain engagement")
            appendLine()

            // Context awareness
            appendLine("🧠 CONTEXT AWARENESS:")
            appendLine("- Remember this user's specific challenges and progress")
            appendLine("- Reference their ${context.learningStreak}-day streak when motivating")
            appendLine("- Connect current questions to their known weak areas")
            appendLine("- Adjust difficulty of explanations based on their current level")
            appendLine("- Use their performance trends to guide recommendations")
            appendLine()

            appendLine("Always respond as if you've been personally coaching this user throughout their journey.")
            appendLine("Make every response feel tailored specifically to their unique situation and progress.")
        }.trim()
    }

    /**
     * Build prompt for challenge-specific coaching
     */
    fun buildChallengeCoachingPrompt(
        userInput: String,
        challengeContext: ChallengeTask,
        performanceMetrics: PerformanceMetrics?,
        weakAreas: List<WeakArea>
    ): List<ChatMessage> {

        val systemMessage = ChatMessage(
            role = Role.SYSTEM.value,
            content = buildChallengeSpecificPrompt(challengeContext, performanceMetrics, weakAreas)
        )

        val userMessage = ChatMessage(Role.USER.value, userInput)

        return listOf(systemMessage, userMessage)
    }

    private fun buildChallengeSpecificPrompt(
        challenge: ChallengeTask,
        metrics: PerformanceMetrics?,
        weakAreas: List<WeakArea>
    ): String {
        return buildString {
            appendLine("You are providing coaching for a specific coding challenge.")
            appendLine()

            appendLine("🎯 CURRENT CHALLENGE:")
            appendLine("- Title: ${challenge.title}")
            appendLine("- Type: ${challenge.type}")
            appendLine("- Difficulty: ${challenge.xp} XP")
            appendLine("- Focus areas: ${challenge.challenges.map { it.skillFocus }.joinToString(", ")}")
            appendLine()

            if (metrics != null) {
                appendLine("📊 USER'S PERFORMANCE ON THIS CHALLENGE:")
                appendLine("- Time spent: ${metrics.durationMillis / 60000} minutes")
                appendLine("- Attempts so far: ${metrics.attempts}")
                appendLine("- Hints used: ${metrics.hintsUsed}")
                appendLine("- Errors encountered: ${metrics.errorCount}")
                appendLine()
            }

            val relevantWeakAreas = weakAreas.filter { weakArea ->
                challenge.type.contains(weakArea.skillArea, ignoreCase = true) ||
                challenge.content.contains(weakArea.skillArea, ignoreCase = true)
            }

            if (relevantWeakAreas.isNotEmpty()) {
                appendLine("⚠️ RELEVANT WEAK AREAS FOR THIS CHALLENGE:")
                relevantWeakAreas.forEach { weakArea ->
                    appendLine("- ${weakArea.skillArea}: Known difficulty area")
                    appendLine("  Common errors: ${weakArea.commonErrors.take(2).joinToString(", ")}")
                }
                appendLine()
            }

            appendLine("Provide specific, actionable guidance for this challenge.")
            appendLine("Reference the user's performance patterns and weak areas.")
            appendLine("Help them understand not just the solution, but the learning process.")
        }.trim()
    }
}
