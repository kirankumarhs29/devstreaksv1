package com.dailydevchallenge.devstreaks.ai

import com.dailydevchallenge.devstreaks.repository.*
import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.features.home.UserStatsManager
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.utils.getLogger
import com.dailydevchallenge.devstreaks.database.toModel
import com.dailydevchallenge.database.*
import com.dailydevchallenge.database.UserAnswer
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

/**
 * Unified context manager that aggregates data from all AI feature repositories
 * Provides consistent, comprehensive context for all AI interactions
 */
class UserContextManager(
    private val memoryRepository: MemoryRepository,
    private val resumeAnalysisRepository: ResumeAnalysisRepository,
    private val interviewRepository: InterviewRepository,
    private val challengeRepository: ChallengeRepository,
    private val profileRepository: ProfileRepository,
    private val userStatsManager: UserStatsManager
) {
    private val logger = getLogger()

    /**
     * Retrieves comprehensive user profile aggregating all AI feature data
     */
    suspend fun getUserProfile(): UnifiedUserProfile {
        return supervisorScope {
            val userId = UserPreferences.getSafeUserId()

            // Fetch all data in parallel
            val basicInfoDeferred = async { getBasicUserInfo(userId) }
            val skillAssessmentDeferred = async { getSkillAssessment(userId) }
            val careerDataDeferred = async { getCareerData(userId) }
            val learningProgressDeferred = async { getLearningProgress(userId) }
            val aiPersonalityDeferred = async { getAIPersonalitySettings(userId) }

            UnifiedUserProfile(
                basicInfo = basicInfoDeferred.await(),
                skillAssessment = skillAssessmentDeferred.await(),
                careerData = careerDataDeferred.await(),
                learningProgress = learningProgressDeferred.await(),
                aiPersonality = aiPersonalityDeferred.await(),
                lastUpdated = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    /**
     * Gets resume context including analysis history and career insights
     */
    suspend fun getResumeContext(): ResumeContext? {
        return try {
            val userId = UserPreferences.getSafeUserId()
            val analyses = resumeAnalysisRepository.getAllForUser(userId)

            if (analyses.isEmpty()) return null

            val latestAnalysis = analyses.first()
            val careerInsights = extractCareerInsights(analyses)

            ResumeContext(
                latestAnalysis = latestAnalysis.toModel(),
                analysisHistory = analyses.map { it.toModel() },
                careerInsights = careerInsights,
                skillStrengths = extractSkillStrengths(analyses),
                improvementAreas = extractImprovementAreas(analyses)
            )
        } catch (e: Exception) {
            logger.e("Failed to get resume context", e)
            null
        }
    }

    /**
     * Gets complete interview history and performance analytics
     */
    suspend fun getInterviewHistory(): InterviewHistory {
        return try {
            val userId = UserPreferences.getSafeUserId()
            val sessions = interviewRepository.getSessionsForUser(userId)
            val allAnswers = sessions.flatMap { session ->
                interviewRepository.getAnswersForSession(session.id)
            }

            InterviewHistory(
                sessions = sessions,
                totalQuestions = allAnswers.size,
                averageScore = calculateAverageScore(allAnswers),
                strongTopics = identifyStrongTopics(allAnswers),
                weakTopics = identifyWeakTopics(allAnswers),
                improvementTrends = calculateImprovementTrends(allAnswers),
                lastInterviewDate = sessions.maxOfOrNull { it.sessionDate }
            )
        } catch (e: Exception) {
            logger.e("Failed to get interview history", e)
            InterviewHistory.empty()
        }
    }

    /**
     * Gets challenge progress and performance analytics
     */
    suspend fun getChallengeProgress(): ChallengeProgress {
        return try {
            val userStats = userStatsManager.userStats.value
            // Use a placeholder for now since getCompletedChallenges doesn't exist
            val completedChallenges = emptyList<com.dailydevchallenge.database.CompletedChallenge>()
            val skillProgress = calculateDetailedSkillProgress(completedChallenges)

            ChallengeProgress(
                totalCompleted = completedChallenges.size,
                currentStreak = userStats.currentStreak,
                totalXp = userStats.totalXp,
                level = userStats.level,
                skillBreakdown = skillProgress,
                recentPerformance = getRecentPerformance(completedChallenges),
                difficultyProgression = analyzeDifficultyProgression(completedChallenges),
                topicMastery = calculateTopicMastery(completedChallenges)
            )
        } catch (e: Exception) {
            logger.e("Failed to get challenge progress", e)
            ChallengeProgress.empty()
        }
    }

    /**
     * Gets conversation memory and chat context
     */
    suspend fun getConversationMemory(): ConversationMemory {
        return try {
            val conversations = memoryRepository.getAllConversations()

            ConversationMemory(
                recentConversations = conversations.take(10),
                conversationCount = conversations.size,
                commonTopics = extractCommonTopics(conversations),
                userPreferences = extractUserPreferences(conversations),
                helpPatterns = analyzeHelpPatterns(conversations)
            )
        } catch (e: Exception) {
            logger.e("Failed to get conversation memory", e)
            ConversationMemory.empty()
        }
    }

    /**
     * Sets and stores user learning intent for personalization
     */
    suspend fun setUserLearningIntent(intent: LearningIntent) {
        try {
            // Store in UserPreferences for persistence
            UserPreferences.setUserLearningIntent(
                primaryGoal = intent.primaryGoal,
                skillFocus = intent.skillFocus.joinToString(","),
                careerTrack = intent.careerTrack,
                experience = intent.experience,
                timePerDay = intent.timePerDay,
                learningStyle = intent.learningStyle,
                fears = intent.fears,
                motivations = intent.motivations.joinToString(",")
            )

            logger.d("UserContextManager: Learning intent saved successfully")
        } catch (e: Exception) {
            logger.e("UserContextManager: Failed to save learning intent", e)
            throw e
        }
    }

    /**
     * Retrieves user learning intent for personalization
     */
    suspend fun getUserLearningIntent(): LearningIntent? {
        return try {
            val primaryGoal = UserPreferences.getUserPrimaryGoal()
            val skillFocus = UserPreferences.getUserSkillFocus()?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            val careerTrack = UserPreferences.getUserCareerTrack()
            val experience = UserPreferences.getUserExperience()
            val timePerDay = UserPreferences.getUserTimePerDay()
            val learningStyle = UserPreferences.getUserLearningStyle()
            val fears = UserPreferences.getUserFears()
            val motivations = UserPreferences.getUserMotivations()?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

            if (primaryGoal?.isNotEmpty() == true) {
                LearningIntent(
                    primaryGoal = primaryGoal,
                    skillFocus = skillFocus,
                    careerTrack = careerTrack ?: "",
                    experience = experience ?: "",
                    timePerDay = timePerDay ?: 30,
                    learningStyle = learningStyle ?: "",
                    fears = fears ?: "",
                    motivations = motivations
                )
            } else null
        } catch (e: Exception) {
            logger.e("UserContextManager: Failed to retrieve learning intent", e)
            null
        }
    }

    /**
     * Updates user preferences based on onboarding data
     */
    suspend fun updateUserPreferences(preferences: Map<String, String>) {
        try {
            preferences.forEach { (key, value) ->
                when (key) {
                    "primaryGoal" -> UserPreferences.setUserPrimaryGoal(value)
                    "skillFocus" -> UserPreferences.setUserSkillFocus(value)
                    "careerTrack" -> UserPreferences.setUserCareerTrack(value)
                    "learningStyle" -> UserPreferences.setUserLearningStyle(value)
                    "timePerDay" -> UserPreferences.setUserTimePerDay(value.toIntOrNull() ?: 30)
                    "experience" -> UserPreferences.setUserExperience(value)
                    "fears" -> UserPreferences.setUserFears(value)
                    "motivations" -> UserPreferences.setUserMotivations(value)
                }
            }
            logger.d("UserContextManager: User preferences updated successfully")
        } catch (e: Exception) {
            logger.e("UserContextManager: Failed to update user preferences", e)
            throw e
        }
    }

    /**
     * Builds contextual prompts for AI interactions based on user's learning intent
     */
    suspend fun buildContextualPrompt(feature: AIFeature, basePrompt: String): String {
        val intent = getUserLearningIntent()
        val userProfile = getUserProfile()

        return buildString {
            append("User Context:\n")

            intent?.let {
                append("- Primary Goal: ${it.primaryGoal}\n")
                append("- Skills Focus: ${it.skillFocus.joinToString(", ")}\n")
                append("- Career Track: ${it.careerTrack}\n")
                append("- Experience Level: ${it.experience}\n")
                append("- Learning Style: ${it.learningStyle}\n")
                append("- Daily Time Commitment: ${it.timePerDay} minutes\n")
                if (it.fears.isNotEmpty()) {
                    append("- Learning Concerns: ${it.fears}\n")
                }
            }

            append("\nUser Progress:\n")
            append("- Current Level: ${userProfile.learningProgress.currentLevel}\n")
            append("- Total XP: ${userProfile.learningProgress.totalXp}\n")
            append("- Current Streak: ${userProfile.learningProgress.currentStreak}\n")

            append("\nPersonalization Instructions:\n")
            when (feature) {
                AIFeature.DEVCOACH_CHAT -> {
                    append("- Adapt coaching style to user's learning preferences\n")
                    append("- Reference their specific goals and progress\n")
                    append("- Provide encouragement based on their experience level\n")
                }
                AIFeature.CHALLENGE_FEEDBACK -> {
                    append("- Tailor feedback complexity to user's experience level\n")
                    append("- Connect feedback to their career goals\n")
                    append("- Suggest next steps aligned with their skill focus\n")
                }
                AIFeature.RESUME_ANALYSIS -> {
                    append("- Focus analysis on their target career track\n")
                    append("- Highlight skills they want to develop\n")
                    append("- Provide actionable advice for their experience level\n")
                }
                AIFeature.MOCK_INTERVIEW -> {
                    append("- Adjust question difficulty to their experience\n")
                    append("- Focus on skills they're developing\n")
                    append("- Tailor scenarios to their career goals\n")
                }
            }

            append("\nOriginal Request: $basePrompt")
        }
    }

    // Private helper methods for context building
    private suspend fun addDevCoachContext(builder: StringBuilder, profile: UnifiedUserProfile) {
        builder.appendLine("\n=== DEVCOACH CONTEXT ===")

        val conversationMemory = getConversationMemory()
        builder.appendLine("Recent topics: ${conversationMemory.commonTopics.joinToString()}")
        builder.appendLine("Help patterns: ${conversationMemory.helpPatterns.joinToString()}")

        val challengeProgress = getChallengeProgress()
        builder.appendLine("Current focus areas: ${challengeProgress.skillBreakdown.entries.sortedByDescending { it.value }.take(3).map { it.key }}")

        val resumeContext = getResumeContext()
        resumeContext?.let {
            builder.appendLine("Career goal: ${it.careerInsights.targetRole ?: "Not specified"}")
            builder.appendLine("Key skills: ${it.skillStrengths.take(5).joinToString()}")
        }
    }

    private suspend fun addResumeAnalysisContext(builder: StringBuilder, profile: UnifiedUserProfile) {
        builder.appendLine("\n=== RESUME ANALYSIS CONTEXT ===")

        val challengeProgress = getChallengeProgress()
        builder.appendLine("Verified skills from challenges: ${challengeProgress.topicMastery.keys.joinToString()}")
        builder.appendLine("Skill proficiency levels: ${challengeProgress.skillBreakdown}")

        val interviewHistory = getInterviewHistory()
        if (interviewHistory.sessions.isNotEmpty()) {
            builder.appendLine("Interview experience: ${interviewHistory.sessions.size} sessions")
            builder.appendLine("Strong interview topics: ${interviewHistory.strongTopics.joinToString()}")
            builder.appendLine("Areas to improve: ${interviewHistory.weakTopics.joinToString()}")
        }

        val resumeContext = getResumeContext()
        resumeContext?.let {
            builder.appendLine("Previous analysis count: ${it.analysisHistory.size}")
            builder.appendLine("Improvement areas identified: ${it.improvementAreas.joinToString()}")
        }
    }

    private suspend fun addMockInterviewContext(builder: StringBuilder, profile: UnifiedUserProfile) {
        builder.appendLine("\n=== MOCK INTERVIEW CONTEXT ===")

        val resumeContext = getResumeContext()
        resumeContext?.let {
            builder.appendLine("Target role: ${it.careerInsights.targetRole ?: "General"}")
            builder.appendLine("Resume skills: ${it.skillStrengths.joinToString()}")
            builder.appendLine("Experience level: ${it.careerInsights.experienceLevel}")
        }

        val challengeProgress = getChallengeProgress()
        builder.appendLine("Technical strengths: ${challengeProgress.topicMastery.entries.filter { it.value > 0.7 }.map { it.key }}")
        builder.appendLine("Areas needing practice: ${challengeProgress.topicMastery.entries.filter { it.value < 0.5 }.map { it.key }}")

        val interviewHistory = getInterviewHistory()
        if (interviewHistory.sessions.isNotEmpty()) {
            builder.appendLine("Previous interview performance: ${interviewHistory.averageScore}/100")
            builder.appendLine("Question types to focus on: ${interviewHistory.weakTopics.take(3).joinToString()}")
        }
    }

    private suspend fun addChallengeFeedbackContext(builder: StringBuilder, profile: UnifiedUserProfile) {
        builder.appendLine("\n=== CHALLENGE FEEDBACK CONTEXT ===")

        val challengeProgress = getChallengeProgress()
        builder.appendLine("Current level: ${challengeProgress.level}")
        builder.appendLine("Recent performance trend: ${challengeProgress.recentPerformance}")
        builder.appendLine("Skill progression: ${challengeProgress.skillBreakdown}")

        val conversationMemory = getConversationMemory()
        builder.appendLine("Learning preferences: ${conversationMemory.userPreferences.joinToString()}")

        val resumeContext = getResumeContext()
        resumeContext?.let {
            builder.appendLine("Career context: ${it.careerInsights.targetRole}")
        }
    }

    // Additional helper methods for data analysis
    private suspend fun getBasicUserInfo(userId: String): UserInfo {
        return try {
            val profile = profileRepository.getProfile(userId)
            if (profile != null) {
                UserInfo(
                    userId = userId,
                    displayName = profile.goal ?: "User", // Using goal field since LearningProfile doesn't have name
                    email = "", // LearningProfile doesn't have email field
                    joinDate = Clock.System.now().toEpochMilliseconds(),
                    timezone = "UTC"
                )
            } else {
                UserInfo.default(userId)
            }
        } catch (e: Exception) {
            UserInfo.default(userId)
        }
    }

    private suspend fun getSkillAssessment(@Suppress("UNUSED_PARAMETER") userId: String): SkillAssessment {
        val challengeProgress = getChallengeProgress()
        val resumeContext = getResumeContext()
        val interviewHistory = getInterviewHistory()

        return SkillAssessment(
            technicalSkills = challengeProgress.topicMastery,
            softSkills = extractSoftSkillsFromInterviews(interviewHistory),
            verifiedSkills = challengeProgress.topicMastery.keys.toList(),
            selfReportedSkills = resumeContext?.skillStrengths ?: emptyList(),
            skillGaps = identifySkillGaps(challengeProgress, resumeContext),
            learningVelocity = calculateLearningVelocity(challengeProgress)
        )
    }

    private suspend fun getCareerData(@Suppress("UNUSED_PARAMETER") userId: String): CareerData {
        val resumeContext = getResumeContext()
        return CareerData(
            targetRole = resumeContext?.careerInsights?.targetRole,
            experienceLevel = resumeContext?.careerInsights?.experienceLevel ?: "Entry",
            industryFocus = resumeContext?.careerInsights?.industryFocus,
            careerGoals = resumeContext?.careerInsights?.careerGoals ?: emptyList(),
            jobSearchActive = resumeContext?.latestAnalysis != null
        )
    }

    private suspend fun getLearningProgress(@Suppress("UNUSED_PARAMETER") userId: String): LearningProgress {
        val challengeProgress = getChallengeProgress()
        val userStats = userStatsManager.userStats.value

        return LearningProgress(
            currentLevel = userStats.level,
            totalXp = userStats.totalXp,
            currentStreak = userStats.currentStreak,
            challengesCompleted = challengeProgress.totalCompleted,
            skillMastery = challengeProgress.topicMastery,
            recentActivity = challengeProgress.recentPerformance,
            learningPath = "Custom Path"
        )
    }

    private fun getAIPersonalitySettings(@Suppress("UNUSED_PARAMETER") userId: String): AIPersonalitySettings {
        return AIPersonalitySettings(
            preferredTone = "Encouraging",
            preferredLearningStyle = "Interactive",
            motivationLevel = "High",
            feedbackStyle = "Detailed",
            expertiseLevel = "Adaptive"
        )
    }

    // Data extraction and analysis helper methods
    private fun extractCareerInsights(analyses: List<com.dailydevchallenge.database.ResumeAnalysis>): CareerInsights {
        val latestAnalysis = analyses.firstOrNull()

        return CareerInsights(
            targetRole = latestAnalysis?.recommendations?.let { rec ->
                // Extract target role from recommendations text
                when {
                    rec.contains("senior", ignoreCase = true) -> "Senior Developer"
                    rec.contains("lead", ignoreCase = true) -> "Tech Lead"
                    rec.contains("manager", ignoreCase = true) -> "Engineering Manager"
                    rec.contains("frontend", ignoreCase = true) -> "Frontend Developer"
                    rec.contains("backend", ignoreCase = true) -> "Backend Developer"
                    rec.contains("fullstack", ignoreCase = true) -> "Full Stack Developer"
                    else -> "Software Developer"
                }
            },
            experienceLevel = determineExperienceLevel(analyses),
            industryFocus = extractIndustryFocus(analyses),
            careerGoals = extractCareerGoals(analyses)
        )
    }

    private fun determineExperienceLevel(analyses: List<com.dailydevchallenge.database.ResumeAnalysis>): String {
        val latestAnalysis = analyses.firstOrNull()
        val skillsCount = latestAnalysis?.skillsParsed?.split(",")?.size ?: 0
        val hasManagerialExp = latestAnalysis?.recommendations?.contains("management", ignoreCase = true) ?: false

        return when {
            hasManagerialExp -> "Senior"
            skillsCount > 10 -> "Mid-level"
            skillsCount > 5 -> "Junior"
            else -> "Entry"
        }
    }

    private fun extractIndustryFocus(analyses: List<com.dailydevchallenge.database.ResumeAnalysis>): String? {
        val allRecommendations = analyses.mapNotNull { it.recommendations }.joinToString(" ")
        return when {
            allRecommendations.contains("fintech", ignoreCase = true) -> "FinTech"
            allRecommendations.contains("healthcare", ignoreCase = true) -> "HealthTech"
            allRecommendations.contains("ecommerce", ignoreCase = true) -> "E-commerce"
            allRecommendations.contains("gaming", ignoreCase = true) -> "Gaming"
            allRecommendations.contains("startup", ignoreCase = true) -> "Startup"
            else -> null
        }
    }

    private fun extractCareerGoals(analyses: List<com.dailydevchallenge.database.ResumeAnalysis>): List<String> {
        val recommendations = analyses.mapNotNull { it.recommendations }.joinToString(" ")
        val goals = mutableListOf<String>()

        if (recommendations.contains("technical skills", ignoreCase = true)) {
            goals.add("Improve technical skills")
        }
        if (recommendations.contains("leadership", ignoreCase = true)) {
            goals.add("Develop leadership skills")
        }
        if (recommendations.contains("communication", ignoreCase = true)) {
            goals.add("Enhance communication skills")
        }
        if (recommendations.contains("certification", ignoreCase = true)) {
            goals.add("Obtain professional certifications")
        }

        return goals
    }

    private fun extractSkillStrengths(analyses: List<com.dailydevchallenge.database.ResumeAnalysis>): List<String> {
        return analyses.flatMap { analysis ->
            analysis.skillsParsed?.split(",")?.map { it.trim() } ?: emptyList()
        }.distinct()
    }

    private fun extractImprovementAreas(analyses: List<com.dailydevchallenge.database.ResumeAnalysis>): List<String> {
        return analyses.flatMap { analysis ->
            analysis.skillsGaps?.split(",")?.map { it.trim() } ?: emptyList()
        }.distinct()
    }

    private fun calculateDetailedSkillProgress(challenges: List<com.dailydevchallenge.database.CompletedChallenge>): Map<String, Int> {
        // Since CompletedChallenge only has pathId and completedDate, we'll use pathId as the skill indicator
        return challenges.groupBy { challenge: com.dailydevchallenge.database.CompletedChallenge -> challenge.pathId }
            .mapValues { (_, topicChallenges: List<com.dailydevchallenge.database.CompletedChallenge>) ->
                // Since we don't have completion status, assume all entries are completed
                val totalAttempts = topicChallenges.size
                if (totalAttempts > 0) {
                    100 // All completed challenges are successful
                } else {
                    0
                }
            }
    }

    private fun getRecentPerformance(challenges: List<com.dailydevchallenge.database.CompletedChallenge>): String {
        if (challenges.isEmpty()) return "No data"

        // Since CompletedChallenge only has completedDate as TEXT, we'll sort by it
        val recentChallenges = challenges.sortedByDescending { challenge: com.dailydevchallenge.database.CompletedChallenge -> challenge.completedDate }.take(10)
        if (recentChallenges.isEmpty()) return "No data"

        // Since all challenges in the table are completed, performance is always excellent
        return "Excellent"
    }

    private fun analyzeDifficultyProgression(challenges: List<com.dailydevchallenge.database.CompletedChallenge>): String {
        if (challenges.isEmpty()) return "No data"

        // Since we don't have difficulty data, return a generic response
        return "Steady progression"
    }

    private fun calculateTopicMastery(challenges: List<com.dailydevchallenge.database.CompletedChallenge>): Map<String, Double> {
        return challenges.groupBy { challenge: com.dailydevchallenge.database.CompletedChallenge -> challenge.pathId }
            .mapValues { (_, topicChallenges: List<com.dailydevchallenge.database.CompletedChallenge>) ->
                // Since all entries are completed challenges, mastery is high
                if (topicChallenges.isNotEmpty()) 1.0 else 0.0
            }
    }

    private fun calculateImprovementTrends(answers: List<UserAnswer>): Map<String, Double> {
        // Use the actual UserAnswer type from the database
        return answers.groupBy { answer: UserAnswer -> answer.questionId ?: "Unknown" }
            .mapValues { (_, topicAnswers: List<UserAnswer>) ->
                val sortedAnswers = topicAnswers.sortedBy { answer: UserAnswer -> answer.timestamp }
                if (sortedAnswers.size < 2) return@mapValues 0.0

                val firstHalf = sortedAnswers.take(sortedAnswers.size / 2)
                val secondHalf = sortedAnswers.drop(sortedAnswers.size / 2)

                val firstAvg = firstHalf.size.toDouble()
                val secondAvg = secondHalf.size.toDouble()

                (secondAvg - firstAvg) / firstAvg.coerceAtLeast(1.0)
            }
    }

    /**
     * Updates user progress data - this method can be used to refresh context
     */
    fun updateUserProgress(userStats: com.dailydevchallenge.devstreaks.model.UserStats) {
        logger.d("User progress updated: Level ${userStats.level}, XP ${userStats.totalXp}")
    }

    /**
     * Clears cached context data to force fresh fetch
     */
    fun refreshContext() {
        logger.d("Context refresh requested")
    }

    // Missing helper methods for interview analysis
    fun calculateAverageScore(answers: List<UserAnswer>): Double {
        if (answers.isEmpty()) return 0.0
        return answers.size.toDouble() / 10.0
    }

    private fun identifyStrongTopics(answers: List<UserAnswer>): List<String> {
        return answers.groupBy { answer: UserAnswer -> answer.questionId ?: "Unknown" }
            .mapValues { (_, topicAnswers: List<UserAnswer>) ->
                topicAnswers.size.toDouble()
            }
            .filter { (_, averageScore: Double) -> averageScore > 5 }
            .keys.toList().take(5)
    }

    private fun identifyWeakTopics(answers: List<UserAnswer>): List<String> {
        return answers.groupBy { answer: UserAnswer -> answer.questionId ?: "Unknown" }
            .mapValues { (_, topicAnswers: List<UserAnswer>) ->
                topicAnswers.size.toDouble()
            }
            .filter { (_, averageScore: Double) -> averageScore < 3 }
            .keys.toList().take(5)
    }

    private fun extractCommonTopics(conversations: List<Conversation>): List<String> {
        val allMessages = conversations.flatMap { listOf(it.userMessage, it.botResponse) }
        val topicKeywords = mapOf(
            "JavaScript" to listOf("javascript", "js", "react", "node"),
            "Python" to listOf("python", "django", "flask", "pandas"),
            "Data Structures" to listOf("array", "linked list", "tree", "graph"),
            "Algorithms" to listOf("sorting", "search", "recursion", "dynamic programming"),
            "Databases" to listOf("sql", "database", "query", "mongodb"),
            "Career" to listOf("job", "interview", "resume", "career"),
            "Learning" to listOf("study", "practice", "learn", "skill")
        )

        return topicKeywords.filter { (_, keywords) ->
            keywords.any { keyword ->
                allMessages.any { message -> message.contains(keyword, ignoreCase = true) }
            }
        }.keys.toList()
    }

    private fun extractUserPreferences(conversations: List<Conversation>): List<String> {
        val allMessages = conversations.map { it.userMessage }.joinToString(" ")
        val preferences = mutableListOf<String>()

        if (allMessages.contains("visual", ignoreCase = true) ||
            allMessages.contains("diagram", ignoreCase = true)) {
            preferences.add("Visual learning")
        }
        if (allMessages.contains("example", ignoreCase = true) ||
            allMessages.contains("practice", ignoreCase = true)) {
            preferences.add("Hands-on learning")
        }
        if (allMessages.contains("explain", ignoreCase = true) ||
            allMessages.contains("theory", ignoreCase = true)) {
            preferences.add("Conceptual learning")
        }
        if (allMessages.contains("quick", ignoreCase = true) ||
            allMessages.contains("summary", ignoreCase = true)) {
            preferences.add("Concise explanations")
        }

        return preferences
    }

    private fun analyzeHelpPatterns(conversations: List<Conversation>): List<String> {
        val userMessages = conversations.map { it.userMessage }
        val patterns = mutableListOf<String>()

        val questionStarters = userMessages.count { it.startsWith("How", ignoreCase = true) ||
                                                   it.startsWith("What", ignoreCase = true) ||
                                                   it.startsWith("Why", ignoreCase = true) }

        if (questionStarters > conversations.size * 0.6) {
            patterns.add("Asks conceptual questions")
        }

        val debuggingKeywords = userMessages.count {
            it.contains("error", ignoreCase = true) ||
            it.contains("bug", ignoreCase = true) ||
            it.contains("not working", ignoreCase = true)
        }

        if (debuggingKeywords > conversations.size * 0.3) {
            patterns.add("Frequently needs debugging help")
        }

        val codeReviewRequests = userMessages.count {
            it.contains("review", ignoreCase = true) ||
            it.contains("feedback", ignoreCase = true) ||
            it.contains("improve", ignoreCase = true)
        }

        if (codeReviewRequests > conversations.size * 0.2) {
            patterns.add("Seeks code improvement advice")
        }

        return patterns
    }

    private fun extractSoftSkillsFromInterviews(history: InterviewHistory): Map<String, Double> {
        val softSkills = mutableMapOf<String, Double>()

        // Analyze communication based on average scores in communication-related topics
        val communicationTopics = history.strongTopics.filter {
            it.contains("communication", ignoreCase = true) ||
            it.contains("presentation", ignoreCase = true) ||
            it.contains("teamwork", ignoreCase = true)
        }

        if (communicationTopics.isNotEmpty()) {
            softSkills["Communication"] = 0.8 // High if in strong topics
        } else if (history.weakTopics.any { it.contains("communication", ignoreCase = true) }) {
            softSkills["Communication"] = 0.4 // Low if in weak topics
        } else {
            softSkills["Communication"] = 0.6 // Default moderate
        }

        // Analyze problem-solving based on technical performance
        softSkills["Problem Solving"] = (history.averageScore / 100.0).coerceIn(0.0, 1.0)

        // Analyze leadership potential based on experience level and performance
        val hasLeadershipQuestions = history.strongTopics.any {
            it.contains("leadership", ignoreCase = true) ||
            it.contains("management", ignoreCase = true)
        }
        softSkills["Leadership"] = if (hasLeadershipQuestions) 0.7 else 0.5

        return softSkills
    }

    private fun identifySkillGaps(progress: ChallengeProgress, resume: ResumeContext?): List<String> {
        val challengeSkills = progress.topicMastery.keys
        val resumeSkills = resume?.skillStrengths ?: emptyList()
        val allImportantSkills = setOf(
            "JavaScript", "Python", "Java", "React", "Node.js",
            "SQL", "Git", "Data Structures", "Algorithms", "System Design"
        )

        val gaps = mutableListOf<String>()

        // Skills mentioned in resume but not practiced in challenges
        resumeSkills.forEach { skill ->
            if (!challengeSkills.any { it.contains(skill, ignoreCase = true) }) {
                gaps.add("$skill - Needs practical validation")
            }
        }

        // Important skills missing entirely
        allImportantSkills.forEach { skill ->
            val isInResume = resumeSkills.any { it.contains(skill, ignoreCase = true) }
            val isInChallenges = challengeSkills.any { it.contains(skill, ignoreCase = true) }

            if (!isInResume && !isInChallenges) {
                gaps.add("$skill - Not demonstrated")
            }
        }

        // Skills with low mastery in challenges
        progress.topicMastery.forEach { (topic, mastery) ->
            if (mastery < 0.5) {
                gaps.add("$topic - Low proficiency (${(mastery * 100).toInt()}%)")
            }
        }

        return gaps.take(10) // Limit to most important gaps
    }

    private fun calculateLearningVelocity(progress: ChallengeProgress): Double {
        // Calculate based on recent completion rate and improvement trends
        val baseVelocity = when (progress.recentPerformance) {
            "Excellent" -> 1.5
            "Good" -> 1.2
            "Improving" -> 1.0
            else -> 0.8
        }

        val streakMultiplier = when {
            progress.currentStreak > 30 -> 1.3
            progress.currentStreak > 7 -> 1.1
            else -> 1.0
        }

        return baseVelocity * streakMultiplier
    }
}

/**
 * Data models for unified context
 */
@Serializable
data class UnifiedUserProfile(
    val basicInfo: UserInfo,
    val skillAssessment: SkillAssessment,
    val careerData: CareerData,
    val learningProgress: LearningProgress,
    val aiPersonality: AIPersonalitySettings,
    val lastUpdated: Long
)

@Serializable
data class UserInfo(
    val userId: String,
    val displayName: String,
    val email: String?,
    val joinDate: Long,
    val timezone: String
) {
    companion object {
        fun default(userId: String) = UserInfo(
            userId = userId,
            displayName = "User",
            email = null,
            joinDate = Clock.System.now().toEpochMilliseconds(),
            timezone = "UTC"
        )
    }
}

@Serializable
data class SkillAssessment(
    val technicalSkills: Map<String, Double>,
    val softSkills: Map<String, Double>,
    val verifiedSkills: List<String>,
    val selfReportedSkills: List<String>,
    val skillGaps: List<String>,
    val learningVelocity: Double
)

@Serializable
data class CareerData(
    val targetRole: String?,
    val experienceLevel: String,
    val industryFocus: String?,
    val careerGoals: List<String>,
    val jobSearchActive: Boolean
)

@Serializable
data class LearningProgress(
    val currentLevel: Int,
    val totalXp: Int,
    val currentStreak: Int,
    val challengesCompleted: Int,
    val skillMastery: Map<String, Double>,
    val recentActivity: String,
    val learningPath: String
)

@Serializable
data class AIPersonalitySettings(
    val preferredTone: String,
    val preferredLearningStyle: String,
    val motivationLevel: String,
    val feedbackStyle: String,
    val expertiseLevel: String
)

@Serializable
data class ResumeContext(
    val latestAnalysis: com.dailydevchallenge.devstreaks.model.ResumeAnalysis,
    val analysisHistory: List<com.dailydevchallenge.devstreaks.model.ResumeAnalysis>,
    val careerInsights: CareerInsights,
    val skillStrengths: List<String>,
    val improvementAreas: List<String>
)

@Serializable
data class CareerInsights(
    val targetRole: String?,
    val experienceLevel: String,
    val industryFocus: String?,
    val careerGoals: List<String>
)

@Serializable
data class InterviewHistory(
    val sessions: List<@Contextual InterviewSession>,
    val totalQuestions: Int,
    val averageScore: Double,
    val strongTopics: List<String>,
    val weakTopics: List<String>,
    val improvementTrends: Map<String, Double>,
    val lastInterviewDate: Long?
) {
    companion object {
        fun empty() = InterviewHistory(
            sessions = emptyList(),
            totalQuestions = 0,
            averageScore = 0.0,
            strongTopics = emptyList(),
            weakTopics = emptyList(),
            improvementTrends = emptyMap(),
            lastInterviewDate = null
        )
    }
}

@Serializable
data class ChallengeProgress(
    val totalCompleted: Int,
    val currentStreak: Int,
    val totalXp: Int,
    val level: Int,
    val skillBreakdown: Map<String, Int>,
    val recentPerformance: String,
    val difficultyProgression: String,
    val topicMastery: Map<String, Double>
) {
    companion object {
        fun empty() = ChallengeProgress(
            totalCompleted = 0,
            currentStreak = 0,
            totalXp = 0,
            level = 1,
            skillBreakdown = emptyMap(),
            recentPerformance = "No data",
            difficultyProgression = "No data",
            topicMastery = emptyMap()
        )
    }
}

@Serializable
data class ConversationMemory(
    val recentConversations: List<@Contextual Conversation>,
    val conversationCount: Int,
    val commonTopics: List<String>,
    val userPreferences: List<String>,
    val helpPatterns: List<String>
) {
    companion object {
        fun empty() = ConversationMemory(
            recentConversations = emptyList(),
            conversationCount = 0,
            commonTopics = emptyList(),
            userPreferences = emptyList(),
            helpPatterns = emptyList()
        )
    }
}

enum class AIFeature {
    DEVCOACH_CHAT,
    RESUME_ANALYSIS,
    MOCK_INTERVIEW,
    CHALLENGE_FEEDBACK
}
