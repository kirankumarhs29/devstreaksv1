package com.dailydevchallenge.devstreaks.ai

import com.dailydevchallenge.devstreaks.llm.*
import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.features.home.UserStatsManager
import com.dailydevchallenge.devstreaks.utils.getLogger

/**
 * Unified AI Coach service that consolidates all AI interactions
 * Provides consistent personality, context awareness, and error handling
 */
class UnifiedAICoachService(
    private val llmService: LLMService,
    private val feedbackService: AIFeedbackService,
    private val userStatsManager: UserStatsManager,
    private val userContextManager: UserContextManager
) {
    private val logger = getLogger()

    // Chat & General Coaching
    suspend fun sendMessage(
        message: String,
        context: CoachingContext = CoachingContext.GENERAL
    ): Result<String> {
        return try {
            val contextualPrompt = userContextManager.buildContextualPrompt(
                feature = AIFeature.DEVCOACH_CHAT,
                basePrompt = message
            )
            val chatMessages = listOf(ChatMessage("user", contextualPrompt))
            val response = llmService.generateResponse(chatMessages)

            Result.success(response)
        } catch (e: Exception) {
            logger.e("Failed to send message", e)
            Result.failure(e)
        }
    }

    // Resume Analysis
    suspend fun analyzeResume(
        resumeText: String,
        targetRole: String,
        includePersonalizedTips: Boolean = true
    ): Result<ResumeAnalysis> {
        return try {
            val contextualPrompt = userContextManager.buildContextualPrompt(
                feature = AIFeature.RESUME_ANALYSIS,
                basePrompt = "Analyze this resume for the role of $targetRole:\n\n$resumeText"
            )

            val analysis = llmService.analyzeResume(resumeText, targetRole)
            Result.success(analysis)
        } catch (e: Exception) {
            logger.e("Failed to analyze resume", e)
            Result.failure(e)
        }
    }

    // Interview Preparation
    suspend fun startInterviewSession(
        role: String,
        resumeSummary: String,
        skills: List<String>,
        difficulty: InterviewDifficulty = InterviewDifficulty.MEDIUM
    ): Result<InterviewStepResult> {
        return try {
            val contextualPrompt = userContextManager.buildContextualPrompt(
                feature = AIFeature.MOCK_INTERVIEW,
                basePrompt = "Start interview session for $role role with skills: ${skills.joinToString()}"
            )

            val result = llmService.startInterviewSession(role, resumeSummary, skills)
            Result.success(result)
        } catch (e: Exception) {
            logger.e("Failed to start interview session", e)
            Result.failure(e)
        }
    }

    suspend fun submitInterviewAnswer(
        answer: String,
        previousQuestion: InterviewQuestion,
        sessionContext: InterviewSessionContext
    ): Result<InterviewStepResult> {
        return try {
            val contextualPrompt = userContextManager.buildContextualPrompt(
                feature = AIFeature.MOCK_INTERVIEW,
                basePrompt = "Evaluate answer: $answer to question: ${previousQuestion.question}"
            )

            val result = llmService.submitInterviewAnswer(answer, previousQuestion, sessionContext)
            Result.success(result)
        } catch (e: Exception) {
            logger.e("Failed to submit interview answer", e)
            Result.failure(e)
        }
    }

    // Challenge Feedback with unified context
    suspend fun reviewChallengeSubmission(
        challenge: Challenge,
        userResponse: String,
        isCorrect: Boolean,
        userLevel: Int
    ): Result<ChallengeFeedback> {
        return try {
            val contextualPrompt = userContextManager.buildContextualPrompt(
                feature = AIFeature.CHALLENGE_FEEDBACK,
                basePrompt = "Review challenge submission: $userResponse for challenge: ${getChallengeTitle(challenge)}"
            )

            val feedback = feedbackService.generateChallengeFeedback(
                challenge = challenge,
                userResponse = userResponse,
                isCorrect = isCorrect,
                completionTime = null
            )

            feedback
        } catch (e: Exception) {
            logger.e("Failed to review challenge submission", e)
            Result.failure(e)
        }
    }

    private fun getChallengeTitle(challenge: Challenge): String {
        return when (challenge) {
            is CodeChallenge -> challenge.title
            is QuizChallenge -> challenge.title
            is FlashcardChallenge -> challenge.title
        }
    }

    // Proactive Coaching
    suspend fun generateDailyInsights(
        userStats: UserStats
    ): Result<List<CoachingInsight>> {
        return try {
            userContextManager.updateUserProgress(userStats)

            val insights = mutableListOf<CoachingInsight>()

            // Generate insights based on user progress
            if (userStats.currentStreak > 0) {
                insights.add(
                    CoachingInsight(
                        type = InsightType.MOTIVATION,
                        title = "Streak Power! 🔥",
                        message = "You're on a ${userStats.currentStreak}-day streak! Keep the momentum going!",
                        actionable = true,
                        priority = Priority.HIGH
                    )
                )
            }

            if (userStats.skillsProgress.isNotEmpty()) {
                val strongestSkill = userStats.skillsProgress.maxByOrNull { it.value }
                strongestSkill?.let { (skill, progress) ->
                    insights.add(
                        CoachingInsight(
                            type = InsightType.SKILL_PROGRESS,
                            title = "Skill Spotlight 🌟",
                            message = "You're excelling in $skill with $progress% progress!",
                            actionable = false,
                            priority = Priority.MEDIUM
                        )
                    )
                }
            }

            Result.success(insights)
        } catch (e: Exception) {
            logger.e("Failed to generate daily insights", e)
            Result.failure(e)
        }
    }

    // Learning Path Optimization
    suspend fun suggestNextSteps(
        completedChallenges: List<String>,
        weakAreas: List<String>
    ): Result<LearningRecommendations> {
        return try {
            val recommendations = LearningRecommendations(
                nextChallenges = suggestChallengesForWeakAreas(weakAreas),
                skillFocus = weakAreas.take(3),
                estimatedTimeToImprove = calculateImprovementTime(weakAreas),
                motivationalMessage = generateMotivationalMessage(completedChallenges.size)
            )

            Result.success(recommendations)
        } catch (e: Exception) {
            logger.e("Failed to suggest next steps", e)
            Result.failure(e)
        }
    }

    private fun suggestChallengesForWeakAreas(weakAreas: List<String>): List<String> {
        return weakAreas.flatMap { area ->
            when (area.lowercase()) {
                "kotlin" -> listOf("Kotlin Basics", "Object-Oriented Programming", "Coroutines")
                "dsa" -> listOf("Arrays & Lists", "Binary Search", "Dynamic Programming")
                "react" -> listOf("Components", "State Management", "Hooks")
                else -> listOf("$area Fundamentals", "$area Practice")
            }
        }
    }

    private fun calculateImprovementTime(weakAreas: List<String>): String {
        val weeks = weakAreas.size * 2 // 2 weeks per skill area
        return if (weeks <= 4) "$weeks weeks" else "${weeks / 4} months"
    }

    private fun generateMotivationalMessage(completedCount: Int): String {
        return when {
            completedCount < 5 -> "You're just getting started! Every expert was once a beginner. 🌱"
            completedCount < 20 -> "Great progress! You're building solid foundations. 🏗️"
            completedCount < 50 -> "Impressive dedication! You're becoming a skilled developer. 🚀"
            else -> "Outstanding! You're a coding champion! Keep inspiring others. 🏆"
        }
    }
}

enum class CoachingContext {
    GENERAL, DEBUGGING, CAREER_PLANNING, SKILL_BUILDING, INTERVIEW_PREP
}

enum class InterviewDifficulty {
    ENTRY_LEVEL, MEDIUM, SENIOR, EXPERT
}

data class CoachingInsight(
    val type: InsightType,
    val title: String,
    val message: String,
    val actionable: Boolean,
    val priority: Priority
)

data class LearningRecommendations(
    val nextChallenges: List<String>,
    val skillFocus: List<String>,
    val estimatedTimeToImprove: String,
    val motivationalMessage: String
)

enum class InsightType {
    MOTIVATION, SKILL_PROGRESS, LEARNING_TIP, CAREER_ADVICE
}

enum class Priority {
    LOW, MEDIUM, HIGH
}
