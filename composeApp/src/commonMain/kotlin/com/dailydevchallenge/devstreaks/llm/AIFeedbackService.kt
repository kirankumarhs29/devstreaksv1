package com.dailydevchallenge.devstreaks.llm

import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.ai.UserContextManager
import com.dailydevchallenge.devstreaks.ai.AIFeature
import com.dailydevchallenge.devstreaks.ai.ChallengeProgress
import com.dailydevchallenge.devstreaks.ai.UnifiedUserProfile
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ChallengeFeedback(
    val score: Int, // 0-100
    val strengths: List<String>,
    val improvements: List<String>,
    val personalizedTips: List<String>,
    val nextSteps: List<String>,
    val encouragement: String
)

@Serializable
data class ProgressAnalysis(
    val currentLevel: String,
    val skillGaps: List<String>,
    val recommendedPaths: List<String>,
    val strengths: List<String>,
    val weeklyGoals: List<String>
)

class AIFeedbackService(
    private val llmService: LLMService,
    private val userContextManager: UserContextManager
) {
    private val logger = getLogger()

    suspend fun generateChallengeFeedback(
        challenge: Challenge,
        userResponse: String,
        isCorrect: Boolean,
        completionTime: Long? = null
    ): Result<ChallengeFeedback> {
        return try {
            // Use UserContextManager to build contextual prompt
            val contextualPrompt = userContextManager.buildContextualPrompt(
                feature = AIFeature.CHALLENGE_FEEDBACK,
                basePrompt = buildFeedbackPrompt(challenge, userResponse, isCorrect, completionTime)
            )

            val messages = listOf(
                ChatMessage("user", contextualPrompt)
            )

            val response = llmService.generateResponse(messages)
            val feedback = parseFeedbackResponse(response)
            Result.success(feedback)
        } catch (e: Exception) {
            logger.e("Failed to generate challenge feedback", e)
            Result.failure(e)
        }
    }

    suspend fun analyzeProgressAndSuggestNextSteps(userId: String): Result<ProgressAnalysis> {
        return try {
            // Get comprehensive user profile from UserContextManager
            val userProfile = userContextManager.getUserProfile()
            val challengeProgress = userContextManager.getChallengeProgress()
            val resumeContext = userContextManager.getResumeContext()

            val contextualPrompt = userContextManager.buildContextualPrompt(
                feature = AIFeature.CHALLENGE_FEEDBACK,
                basePrompt = """
                Analyze my overall progress and suggest next steps for my learning journey.
                Focus on:
                1. Current skill level assessment
                2. Identified skill gaps
                3. Recommended learning path
                4. Weekly goals based on my career objectives
                """.trimIndent()
            )

            val messages = listOf(ChatMessage("user", contextualPrompt))
            val response = llmService.generateResponse(messages)
            val analysis = parseProgressAnalysis(response, userProfile, challengeProgress)

            Result.success(analysis)
        } catch (e: Exception) {
            logger.e("Failed to analyze progress", e)
            Result.failure(e)
        }
    }

    private fun buildFeedbackPrompt(
        challenge: Challenge,
        userResponse: String,
        isCorrect: Boolean,
        completionTime: Long?
    ): String {
        val challengeType = when (challenge) {
            is CodeChallenge -> "coding"
            is QuizChallenge -> "quiz"
            is FlashcardChallenge -> "flashcard"
        }

        val timeContext = completionTime?.let {
            "completed in ${it}ms"
        } ?: "completion time not tracked"

        return """
        You are an expert programming mentor providing personalized feedback to a developer.
        
        Challenge Type: $challengeType
        Challenge Details: ${getSerializedChallenge(challenge)}
        User's Response: $userResponse
        Result: ${if (isCorrect) "Correct" else "Incorrect"}
        Time: $timeContext
        
        Provide constructive feedback in JSON format:
        {
            "score": <0-100 based on correctness and approach>,
            "strengths": [<list of what they did well>],
            "improvements": [<specific areas to improve>],
            "personalizedTips": [<actionable tips for this user>],
            "nextSteps": [<recommended next challenges or learning>],
            "encouragement": "<motivational message>"
        }
        
        Make the feedback encouraging, specific, and actionable. Focus on learning and growth.
        """.trimIndent()
    }

    private fun buildProgressAnalysisPrompt(
        completedChallenges: List<CompletedChallenge>,
        userProfile: User
    ): String {
        val challengesSummary = completedChallenges.takeLast(10).joinToString("\n") {
            "- Challenge completed: XP ${userProfile.xp}"
        }

        return """
        Analyze this developer's progress and provide personalized recommendations.
        
        User Profile:
        - XP: ${userProfile.xp}
        - Level: ${userProfile.level}
        - Streak: ${userProfile.dailyStreak} days
        - Total Completed: ${completedChallenges.size} challenges
        
        Recent Challenges:
        $challengesSummary
        
        Provide analysis in JSON format:
        {
            "currentLevel": "<beginner/intermediate/advanced assessment>",
            "skillGaps": [<specific areas needing improvement>],
            "recommendedPaths": [<suggested learning paths>],
            "strengths": [<user's strong areas>],
            "weeklyGoals": [<achievable goals for next week>]
        }
        
        Be specific and actionable in your recommendations.
        """.trimIndent()
    }

    private fun getSerializedChallenge(challenge: Challenge): String {
        return when (challenge) {
            is CodeChallenge -> "Title: ${challenge.title}, Description: ${challenge.description}"
            is QuizChallenge -> "Title: ${challenge.title}, Questions: ${challenge.questions.size}"
            is FlashcardChallenge -> "Title: ${challenge.title}, Cards: ${challenge.cards.size}"
        }
    }

    private fun parseFeedbackResponse(response: String): ChallengeFeedback {
        return try {
            Json.decodeFromString<ChallengeFeedback>(response)
        } catch (e: Exception) {
            // Fallback if JSON parsing fails
            ChallengeFeedback(
                score = 75,
                strengths = listOf("You attempted the challenge"),
                improvements = listOf("Keep practicing regularly"),
                personalizedTips = listOf("Review the fundamentals", "Practice similar problems"),
                nextSteps = listOf("Try the next challenge in your path"),
                encouragement = "Great effort! Keep building your coding streak! 🚀"
            )
        }
    }

    private fun parseProgressAnalysis(
        response: String,
        userProfile: UnifiedUserProfile,
        challengeProgress: ChallengeProgress
    ): ProgressAnalysis {
        return try {
            // Enhanced parsing with context from UserContextManager
            ProgressAnalysis(
                currentLevel = "Level ${userProfile.learningProgress.currentLevel} - ${userProfile.careerData.experienceLevel}",
                skillGaps = userProfile.skillAssessment.skillGaps,
                recommendedPaths = generateRecommendedPaths(userProfile),
                strengths = userProfile.skillAssessment.verifiedSkills,
                weeklyGoals = generateWeeklyGoals(userProfile, challengeProgress)
            )
        } catch (e: Exception) {
            logger.e("Failed to parse progress analysis", e)
            ProgressAnalysis(
                currentLevel = "Assessment in progress",
                skillGaps = emptyList(),
                recommendedPaths = emptyList(),
                strengths = emptyList(),
                weeklyGoals = emptyList()
            )
        }
    }

    private fun generateRecommendedPaths(userProfile: UnifiedUserProfile): List<String> {
        val paths = mutableListOf<String>()

        userProfile.careerData.targetRole?.let { role ->
            when (role.lowercase()) {
                "frontend developer" -> paths.add("Frontend Mastery Path")
                "backend developer" -> paths.add("Backend Engineering Path")
                "full stack developer" -> paths.add("Full Stack Development Path")
                "data scientist" -> paths.add("Data Science & Analytics Path")
                else -> paths.add("Software Engineering Fundamentals")
            }
        }

        // Add paths based on skill gaps
        if (userProfile.skillAssessment.skillGaps.any { it.contains("algorithm", ignoreCase = true) }) {
            paths.add("Algorithms & Data Structures Mastery")
        }
        if (userProfile.skillAssessment.skillGaps.any { it.contains("system design", ignoreCase = true) }) {
            paths.add("System Design & Architecture")
        }

        return paths
    }

    private fun generateWeeklyGoals(userProfile: UnifiedUserProfile, challengeProgress: ChallengeProgress): List<String> {
        val goals = mutableListOf<String>()

        // Goals based on current streak and performance
        when (userProfile.learningProgress.currentStreak) {
            0 -> goals.add("Start a 7-day coding streak")
            in 1..6 -> goals.add("Maintain daily practice to reach 7-day streak")
            else -> goals.add("Maintain your excellent ${userProfile.learningProgress.currentStreak}-day streak")
        }

        // Goals based on skill gaps
        userProfile.skillAssessment.skillGaps.take(2).forEach { gap ->
            goals.add("Practice $gap - Complete 3 related challenges")
        }

        // Goals based on career objectives
        userProfile.careerData.targetRole?.let { role ->
            goals.add("Focus on $role skills - Study relevant topics for 30 minutes daily")
        }

        return goals
    }
}
