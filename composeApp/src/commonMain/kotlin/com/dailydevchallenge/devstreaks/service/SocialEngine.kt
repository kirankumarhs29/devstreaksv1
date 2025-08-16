package com.dailydevchallenge.devstreaks.service

import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.llm.ChatMessage
import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.sync.FirebaseUserHelper
import com.dailydevchallenge.devstreaks.utils.generateUUID
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.time.Duration.Companion.days

/**
 * Social engine that manages community features, leaderboards, and collaborative learning
 * Integrates LLM for generating peer feedback and community challenges
 */
class SocialEngine(
    private val llmService: LLMService,
    private val firebaseUserHelper: FirebaseUserHelper
) {
    private val logger = getLogger()
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val FIREBASE_SOCIAL_PROFILES_PATH = "socialProfiles"
        private const val FIREBASE_LEADERBOARDS_PATH = "leaderboards"
        private const val FIREBASE_COLLABORATIVE_CHALLENGES_PATH = "collaborativeChallenges"
        private const val FIREBASE_CODE_REVIEWS_PATH = "codeReviews"
        private const val FIREBASE_STUDY_GROUPS_PATH = "studyGroups"
        private const val FIREBASE_SOCIAL_INTERACTIONS_PATH = "socialInteractions"
        private const val FIREBASE_COMMUNITY_CHALLENGES_PATH = "communityChallenges"
    }

    /**
     * Initialize user's social profile
     */
    suspend fun initializeSocialProfile(
        userId: String,
        displayName: String,
        bio: String = ""
    ): SocialProfile = withContext(Dispatchers.Default) {
        val profile = SocialProfile(
            userId = userId,
            displayName = displayName,
            bio = bio,
            joinedAt = Clock.System.now(),
            lastActiveAt = Clock.System.now()
        )

        saveSocialProfileToFirebase(profile)
        profile
    }

    /**
     * Update user's social profile with new achievements and XP
     */
    suspend fun updateSocialProfile(
        userId: String,
        xpGained: Int = 0,
        newBadges: List<SkillBadge> = emptyList(),
        newAchievements: List<Achievement> = emptyList()
    ): SocialProfile = withContext(Dispatchers.Default) {
        val currentProfile = getSocialProfile(userId)

        val updatedProfile = currentProfile!!.copy(
            totalXp = currentProfile.totalXp + xpGained,
            level = calculateLevel(currentProfile.totalXp + xpGained),
            skillBadges = currentProfile.skillBadges + newBadges,
            achievements = currentProfile.achievements + newAchievements.filterIsInstance<SocialAchievement>(),
            lastActiveAt = Clock.System.now()
        )

        saveSocialProfileToFirebase(updatedProfile)
        updatedProfile
    }

    /**
     * Generate leaderboard for specified type and timeframe
     */
    suspend fun generateLeaderboard(
        type: LeaderboardType,
        limit: Int = 50
    ): List<LeaderboardEntry> = withContext(Dispatchers.Default) {
        try {
            // TODO: Query Firebase for user data based on leaderboard type
            // For now, return empty list - would be implemented with real Firebase queries
            emptyList()
        } catch (e: Exception) {
            logger.e("Failed to generate leaderboard", e)
            emptyList()
        }
    }

    /**
     * Generate LLM-powered peer review feedback
     */
    suspend fun generatePeerReviewFeedback(
        codeSnippet: String,
        language: String,
        context: String,
        skillLevel: String
    ): CodeReview = withContext(Dispatchers.Default) {
        val reviewPrompt = buildPeerReviewPrompt(codeSnippet, language, context, skillLevel)

        val llmResponse = llmService.generateResponse(listOf(
            ChatMessage(
                role = "system",
                content = "You are an experienced code reviewer providing constructive, educational feedback to help developers improve."
            ),
            ChatMessage(
                role = "user",
                content = reviewPrompt
            )
        ))

        parseLLMReviewResponse(llmResponse)
    }

    /**
     * Generate LLM-powered peer feedback for code submissions
     */
    suspend fun generatePeerFeedback(
        userId: String,
        submissionId: String,
        submissionCode: String,
        submissionDescription: String
    ): Result<PeerFeedback> = withContext(Dispatchers.Default) {
        try {
            val prompt = buildPeerFeedbackPrompt(submissionCode, submissionDescription)

            val llmResponse = llmService.generateResponse(listOf(
                ChatMessage(
                    role = "system",
                    content = "You are an experienced developer providing constructive peer feedback. Focus on code quality, best practices, and learning opportunities."
                ),
                ChatMessage(
                    role = "user",
                    content = prompt
                )
            ))

            val feedback = parseFeedbackFromLLM(llmResponse, userId, submissionId)
            savePeerFeedbackToFirebase(feedback)

            Result.success(feedback)
        } catch (e: Exception) {
            logger.e("Failed to generate peer feedback", e)
            Result.failure(e)
        }
    }

    /**
     * Create collaborative challenge using LLM
     */
    suspend fun createCollaborativeChallenge(
        creatorId: String,
        theme: String,
        difficulty: DifficultyLevel,
        maxParticipants: Int = 4
    ): Result<CollaborativeChallenge> = withContext(Dispatchers.Default) {
        try {
            val prompt = buildCollaborativeChallengePrompt(theme, difficulty, maxParticipants)

            val llmResponse = llmService.generateResponse(listOf(
                ChatMessage(
                    role = "system",
                    content = "Generate engaging collaborative programming challenges that require teamwork and multiple skill sets."
                ),
                ChatMessage(
                    role = "user",
                    content = prompt
                )
            ))

            val challenge = parseCollaborativeChallengeFromLLM(llmResponse, creatorId)
            saveCollaborativeChallengeToFirebase(challenge)

            Result.success(challenge)
        } catch (e: Exception) {
            logger.e("Failed to create collaborative challenge", e)
            Result.failure(e)
        }
    }

    /**
     * Generate LLM-based community challenges from trending topics
     */
    suspend fun generateCommunityChallenge(
        trendingTopic: String,
        difficulty: DifficultyLevel,
        duration: Int = 7
    ): CommunityChallenge = withContext(Dispatchers.Default) {
        val challengePrompt = buildCommunityChallengPrompt(trendingTopic, difficulty)

        val llmResponse = llmService.generateResponse(listOf(
            ChatMessage(
                role = "system",
                content = "Generate engaging community programming challenges based on trending topics that encourage learning and collaboration."
            ),
            ChatMessage(
                role = "user",
                content = challengePrompt
            )
        ))

        val challenge = parseCommunityChallengeFromLLM(llmResponse)
        saveCommunityChallengeToFirebase(challenge)
        challenge
    }

    /**
     * Join collaborative challenge
     */
    suspend fun joinCollaborativeChallenge(
        userId: String,
        challengeId: String,
        role: ParticipantRole = ParticipantRole.PARTICIPANT
    ): Result<CollaborationParticipation> = withContext(Dispatchers.Default) {
        try {
            val challenge = getCollaborativeChallenge(challengeId)
                ?: return@withContext Result.failure(Exception("Challenge not found"))

            if (challenge.currentParticipants.size >= challenge.maxParticipants) {
                return@withContext Result.failure(Exception("Challenge is full"))
            }

            val participation = CollaborationParticipation(
                id = generateUUID(),
                userId = userId,
                challengeId = challengeId,
                role = role,
                joinedAt = Clock.System.now()
            )

            // Update challenge participant list
            val updatedChallenge = challenge.copy(
                currentParticipants = challenge.currentParticipants + userId
            )
            saveCollaborativeChallengeToFirebase(updatedChallenge)

            Result.success(participation)
        } catch (e: Exception) {
            logger.e("Failed to join collaborative challenge", e)
            Result.failure(e)
        }
    }

    /**
     * Submit peer review for code
     */
    suspend fun submitPeerReview(
        reviewerId: String,
        submissionId: String,
        authorId: String,
        codeSnippet: String,
        feedback: String,
        scores: Map<String, Float> // codeQuality, functionality, style
    ): CodeReview = withContext(Dispatchers.Default) {
        val review = CodeReview(
            id = generateUUID(),
            submissionId = submissionId,
            reviewerId = reviewerId,
            submitterId = authorId,
            codeSnippet = codeSnippet,
            overallScore = scores.values.average().toFloat(),
            feedback = feedback,
            strengths = extractStrengths(feedback),
            improvements = extractImprovements(feedback),
            codeQualityScore = scores["codeQuality"] ?: 3.0f,
            functionalityScore = scores["functionality"] ?: 3.0f,
            styleScore = scores["style"] ?: 3.0f,
            createdAt = Clock.System.now(),
            reviewedAt = Clock.System.now()
        )

        saveCodeReviewToFirebase(review)

        // Update reviewer's reputation
        updateReviewerReputation(reviewerId, review.overallScore)

        review
    }

    /**
     * Create study group
     */
    suspend fun createStudyGroup(
        creatorId: String,
        name: String,
        description: String,
        focusSkills: List<String>,
        targetLevel: DifficultyLevel,
        maxMembers: Int = 10
    ): StudyGroup = withContext(Dispatchers.Default) {
        val studyGroup = StudyGroup(
            id = generateUUID(),
            name = name,
            description = description,
            focusSkills = focusSkills,
            targetLevel = targetLevel,
            maxMembers = maxMembers,
            memberIds = listOf(creatorId),
            createdBy = creatorId,
            createdAt = Clock.System.now(),
            meetingSchedule = MeetingSchedule(
                frequency = MeetingFrequency.WEEKLY,
                timeUtc = "19:00",
                durationMinutes = 60
            )
        )

        saveStudyGroupToFirebase(studyGroup)
        studyGroup
    }

    /**
     * Record social interaction
     */
    suspend fun recordSocialInteraction(
        fromUserId: String,
        targetId: String,
        targetType: InteractionTargetType,
        interactionType: InteractionType,
        content: String? = null,
        toUserId: String? = null
    ): SocialInteraction = withContext(Dispatchers.Default) {
        val interaction = SocialInteraction(
            id = generateUUID(),
            fromUserId = fromUserId,
            toUserId = toUserId,
            targetId = targetId,
            targetType = targetType,
            type = interactionType,
            content = content,
            createdAt = Clock.System.now()
        )

        saveSocialInteractionToFirebase(interaction)

        // Update recipient's reputation if applicable
        if (toUserId != null && interactionType == InteractionType.KUDOS) {
            updateUserReputation(toUserId, 5)
        }

        interaction
    }

    /**
     * Get recent code reviews for a user
     */
    suspend fun getRecentCodeReviews(userId: String, limit: Int = 10): List<CodeReview> = withContext(Dispatchers.Default) {
        try {
            // Try to get from Firebase first - fix method name consistency
            val reviews = firebaseUserHelper.getUserProfile(userId).let { userProfile ->
                // For now, return empty list as we don't have a direct readData method
                emptyList<CodeReview>()
            }

            // Return recent reviews sorted by creation date
            reviews.sortedByDescending { review -> review.createdAt }.take(limit)
        } catch (e: Exception) {
            logger.e("Failed to get code reviews", e)
            // Return sample data for now
            generateSampleCodeReviews(userId, limit)
        }
    }

    /**
     * Get user's study groups
     */
    suspend fun getUserStudyGroups(userId: String): List<StudyGroup> = withContext(Dispatchers.Default) {
        try {
            // For now, return empty list as we need to implement proper Firebase data access
            val studyGroups = emptyList<StudyGroup>()

            // Filter groups where user is a member
            studyGroups.filter { group -> userId in group.memberIds }
        } catch (e: Exception) {
            logger.e("Failed to get study groups", e)
            emptyList()
        }
    }

    /**
     * Mark a review as helpful
     */
    suspend fun markReviewHelpful(reviewId: String, isHelpful: Boolean): Unit = withContext(Dispatchers.Default) {
        try {
            // Update Firebase with helpful count
            // For now, just log the action
            logger.d("Marked review $reviewId as helpful: $isHelpful")
        } catch (e: Exception) {
            logger.e("Failed to mark review as helpful", e)
        }
    }

    /**
     * Join a community challenge
     */
    suspend fun joinCommunityChallenge(challengeId: String): Unit = withContext(Dispatchers.Default) {
        try {
            // Update Firebase with user participation
            logger.d("User joined community challenge: $challengeId")
        } catch (e: Exception) {
            logger.e("Failed to join community challenge", e)
        }
    }

    /**
     * Leave a community challenge
     */
    suspend fun leaveCommunityChallenge(challengeId: String): Unit = withContext(Dispatchers.Default) {
        try {
            // Update Firebase to remove user participation
            logger.d("User left community challenge: $challengeId")
        } catch (e: Exception) {
            logger.e("Failed to leave community challenge", e)
        }
    }

    /**
     * Generate sample code reviews for testing
     */
    private suspend fun generateSampleCodeReviews(userId: String, limit: Int): List<CodeReview> {
        return listOf(
            CodeReview(
                id = generateUUID(),
                submissionId = generateUUID(),
                submissionTitle = "Binary Search Implementation",
                reviewerId = "ai-reviewer",
                reviewerName = "AI Code Reviewer",
                submitterId = userId,
                feedback = "Good implementation with clear variable names. Consider adding edge case handling for empty arrays.",
                overallRating = ReviewRating.GOOD,
                areasForImprovement = listOf("Edge case handling", "Performance optimization"),
                createdAt = Clock.System.now(),
                generatedByLLM = true,
                helpfulCount = 5,
                codeSnippet = """
                    fun binarySearch(arr: IntArray, target: Int): Int {
                        var left = 0
                        var right = arr.size - 1
                        
                        while (left <= right) {
                            val mid = left + (right - left) / 2
                            when {
                                arr[mid] == target -> return mid
                                arr[mid] < target -> left = mid + 1
                                else -> right = mid - 1
                            }
                        }
                        return -1
                    }
                """.trimIndent(),
                language = "kotlin",
                structuredFeedback = StructuredFeedback(
                    strengths = listOf(
                        "Clear variable naming",
                        "Correct implementation of binary search algorithm",
                        "Good use of when expression"
                    ),
                    improvements = listOf(
                        "Add null checks for input array",
                        "Consider handling overflow in mid calculation",
                        "Add documentation comments"
                    ),
                    bestPractices = listOf(
                        "Use descriptive parameter names",
                        "Consider using generic types for reusability",
                        "Add unit tests for edge cases"
                    )
                )
            ),
            CodeReview(
                id = generateUUID(),
                submissionId = generateUUID(),
                submissionTitle = "Linked List Reversal",
                reviewerId = "peer-123",
                reviewerName = "CodeMaster2024",
                submitterId = userId,
                feedback = "Nice iterative approach! The logic is sound and handles the edge cases well.",
                overallRating = ReviewRating.EXCELLENT,
                areasForImprovement = listOf("Code comments"),
                createdAt = Clock.System.now().minus(2.days),
                generatedByLLM = false,
                helpfulCount = 8,
                codeSnippet = """
                    fun reverseLinkedList(head: ListNode?): ListNode? {
                        var prev: ListNode? = null
                        var current = head

                        while (current != null) {
                            val next = current.next
                            current.next = prev
                            prev = current
                            current = next
                        }

                        return prev
                    }
                """.trimIndent(),
                language = "kotlin"
            )
        ).take(limit)
    }

    private fun buildPeerReviewPrompt(
        codeSnippet: String,
        language: String,
        context: String,
        skillLevel: String
    ): String {
        return """
        Review this $language code submission from a $skillLevel developer:
        
        Context: $context
        
        Code:
        ```$language
        $codeSnippet
        ```
        
        Provide constructive feedback covering:
        1. Code quality and best practices
        2. Functionality and correctness
        3. Style and readability
        4. Specific strengths to reinforce
        5. Areas for improvement with actionable suggestions
        
        Return JSON format:
        {
          "overallScore": 4.2,
          "feedback": "Detailed review...",
          "strengths": ["Strength 1", "Strength 2"],
          "improvements": ["Improvement 1", "Improvement 2"],
          "codeQualityScore": 4.0,
          "functionalityScore": 4.5,
          "styleScore": 4.0
        }
        """.trimIndent()
    }

    private fun buildPeerFeedbackPrompt(code: String, description: String): String {
        return """
        Provide constructive peer feedback for this code submission:
        
        Description: $description
        
        Code:
        ```
        $code
        ```
        
        Provide feedback on:
        1. Code quality and readability
        2. Best practices and conventions
        3. Potential improvements
        4. Positive aspects to reinforce
        5. Learning opportunities
        
        Return JSON with this structure:
        {
          "overallRating": 4.2,
          "strengths": ["Clean code structure", "Good variable naming"],
          "improvementAreas": ["Add error handling", "Consider edge cases"],
          "specificComments": [
            {
              "lineNumber": 15,
              "comment": "Consider using const instead of let here",
              "severity": "MINOR"
            }
          ],
          "learningResources": [
            {
              "title": "Error Handling Best Practices",
              "url": "https://example.com",
              "description": "Learn about robust error handling"
            }
          ],
          "encouragement": "Great work on the overall structure!"
        }
        """.trimIndent()
    }

    private fun buildCollaborativeChallengePrompt(
        theme: String,
        difficulty: DifficultyLevel,
        maxParticipants: Int
    ): String {
        return """
        Create a collaborative programming challenge with theme: $theme
        Difficulty: $difficulty
        Max participants: $maxParticipants
        
        Requirements:
        1. Requires collaboration between team members
        2. Different roles/skills for each participant
        3. Clear deliverables and timeline
        4. Real-world application
        
        Return JSON:
        {
          "title": "Challenge Title",
          "description": "Detailed description",
          "theme": "$theme",
          "difficulty": "$difficulty",
          "maxParticipants": $maxParticipants,
          "estimatedDurationHours": 8,
          "roles": [
            {
              "title": "Frontend Developer",
              "description": "Responsible for UI implementation",
              "skillsRequired": ["React", "CSS"]
            }
          ],
          "deliverables": ["Working application", "Documentation"],
          "evaluation": {
            "criteria": ["Functionality", "Code quality", "Teamwork"],
            "pointsDistribution": {"individual": 60, "team": 40}
          }
        }
        """.trimIndent()
    }

    private fun buildCommunityChallengPrompt(
        trendingTopic: String,
        difficulty: DifficultyLevel
    ): String {
        return """
        Create a community programming challenge based on the trending topic: "$trendingTopic"
        Difficulty level: $difficulty
        
        Requirements:
        1. Engaging and educational
        2. Clear objectives and deliverables
        3. Encourages collaboration and learning
        4. Appropriate for the specified difficulty level
        5. Can be completed in a reasonable timeframe
        
        Return JSON format:
        {
          "title": "Challenge Title",
          "description": "Detailed description with objectives",
          "requirements": ["Requirement 1", "Requirement 2"],
          "deliverables": ["Deliverable 1", "Deliverable 2"],
          "estimatedTimeMinutes": 120
        }
        """.trimIndent()
    }

    private fun parseLLMReviewResponse(response: String): CodeReview {
        return try {
            val cleanedResponse = response.substringAfter("{").substringBeforeLast("}").let { "{\n$it\n}" }
            val reviewData = json.decodeFromString<Map<String, Any>>(cleanedResponse)

            CodeReview(
                id = generateUUID(),
                submissionId = "",
                reviewerId = "llm-assistant",
                submitterId = "",
                codeSnippet = "",
                overallScore = (reviewData["overallScore"] as? Double)?.toFloat() ?: 3.0f,
                feedback = reviewData["feedback"] as? String ?: "",
                strengths = (reviewData["strengths"] as? List<String>) ?: emptyList(),
                improvements = (reviewData["improvements"] as? List<String>) ?: emptyList(),
                codeQualityScore = (reviewData["codeQualityScore"] as? Double)?.toFloat() ?: 3.0f,
                functionalityScore = (reviewData["functionalityScore"] as? Double)?.toFloat() ?: 3.0f,
                styleScore = (reviewData["styleScore"] as? Double)?.toFloat() ?: 3.0f,
                createdAt = Clock.System.now(),
                reviewedAt = Clock.System.now(),
                generatedByLLM = true
            )
        } catch (e: Exception) {
            logger.e("Failed to parse LLM review response", e)
            createFallbackReview()
        }
    }

    private suspend fun parseFeedbackFromLLM(
        llmResponse: String,
        reviewerId: String,
        submissionId: String
    ): PeerFeedback {
        return try {
            val feedbackData = json.decodeFromString<Map<String, Any>>(llmResponse)
            PeerFeedback(
                id = generateUUID(),
                submissionId = submissionId,
                reviewerId = reviewerId,
                overallRating = (feedbackData["overallRating"] as? Double) ?: 3.0,
                strengths = (feedbackData["strengths"] as? List<String>) ?: emptyList(),
                improvementAreas = (feedbackData["improvementAreas"] as? List<String>) ?: emptyList(),
                encouragement = feedbackData["encouragement"] as? String ?: "",
                createdAt = Clock.System.now()
            )
        } catch (e: Exception) {
            logger.e("Failed to parse feedback from LLM", e)
            createFallbackFeedback(reviewerId, submissionId)
        }
    }

    private suspend fun parseCollaborativeChallengeFromLLM(
        llmResponse: String,
        creatorId: String
    ): CollaborativeChallenge {
        return try {
            json.decodeFromString<CollaborativeChallenge>(llmResponse).copy(
                id = generateUUID(),
                createdBy = creatorId,
                createdAt = Clock.System.now()
            )
        } catch (e: Exception) {
            logger.e("Failed to parse collaborative challenge from LLM", e)
            createFallbackCollaborativeChallenge(creatorId)
        }
    }

    private suspend fun parseCommunityChallengeFromLLM(llmResponse: String): CommunityChallenge {
        return try {
            json.decodeFromString<CommunityChallenge>(llmResponse).copy(
                id = generateUUID(),
                createdAt = Clock.System.now()
            )
        } catch (e: Exception) {
            logger.e("Failed to parse community challenge from LLM", e)
            createFallbackCommunityChallenge()
        }
    }

    private fun createFallbackReview(): CodeReview {
        return CodeReview(
            id = generateUUID(),
            submissionId = "",
            reviewerId = "llm-assistant",
            submitterId = "",
            codeSnippet = "",
            overallScore = 3.0f,
            feedback = "Good effort! Keep practicing to improve your coding skills.",
            strengths = listOf("Shows understanding of basic concepts"),
            improvements = listOf("Consider adding more comments", "Look into optimization opportunities"),
            codeQualityScore = 3.0f,
            functionalityScore = 3.0f,
            styleScore = 3.0f,
            createdAt = Clock.System.now(),
            reviewedAt = Clock.System.now(),
            generatedByLLM = true
        )
    }

    private fun createFallbackFeedback(reviewerId: String, submissionId: String): PeerFeedback {
        return PeerFeedback(
            id = generateUUID(),
            submissionId = submissionId,
            reviewerId = reviewerId,
            overallRating = 3.5,
            strengths = listOf("Good effort", "Code structure is clear"),
            improvementAreas = listOf("Consider adding comments", "Test edge cases"),
            encouragement = "Keep up the great work!",
            createdAt = Clock.System.now()
        )
    }

    private fun createFallbackCollaborativeChallenge(creatorId: String): CollaborativeChallenge {
        val now = Clock.System.now()
        return CollaborativeChallenge(
            id = generateUUID(),
            title = "Team Web App Challenge",
            description = "Build a collaborative web application as a team",
            category = CollaborationCategory.TEAM_PROJECT,
            difficulty = DifficultyLevel.MEDIUM,
            maxParticipants = 4,
            currentParticipants = emptyList(),
            status = CollaborativeChallengeStatus.OPEN_FOR_REGISTRATION,
            createdBy = creatorId,
            createdAt = now,
            startDate = now,
            endDate = now.plus(7.days),
            requirements = listOf("Use version control", "Write unit tests"),
            deliverables = listOf("Working app", "README"),
            xpReward = 200,
            estimatedDurationHours = 8,
            theme = "Web Development",
            teamFormationStrategy = TeamStrategy.OPEN_JOIN
        )
    }

    private fun createFallbackCommunityChallenge(): CommunityChallenge {
        val now = Clock.System.now()
        return CommunityChallenge(
            id = generateUUID(),
            title = "Weekly Code Challenge",
            description = "Solve this week's programming puzzle",
            trendingTopic = "Algorithms",
            difficulty = DifficultyLevel.MEDIUM,
            skillAreas = listOf("Algorithms", "Data Structures"),
            durationDays = 7,
            estimatedTimeMinutes = 120,
            participantCount = 0,
            overallProgress = 0.0f,
            teamProgress = emptyList(),
            adaptiveTasks = emptyList(),
            startDate = now,
            endDate = now.plus(7.days),
            xpReward = 100,
            createdAt = now,
            generatedByLLM = true,
            theme = "Algorithm"
        )
    }

    private suspend fun saveSocialProfileToFirebase(profile: SocialProfile) {
        try {
            // TODO: Persist when generic save API is added to FirebaseUserHelper
            val profileJson = json.encodeToString(profile)
            logger.d("Skipping Firebase save for social profile ${'$'}{profile.userId}. JSON length=${'$'}{profileJson.length}")
        } catch (e: Exception) {
            logger.e("Failed to save social profile to Firebase", e)
        }
    }

    private suspend fun savePeerFeedbackToFirebase(feedback: PeerFeedback) {
        try {
            // TODO: Persist when generic save API is added to FirebaseUserHelper
            val feedbackJson = json.encodeToString(feedback)
            logger.d("Skipping Firebase save for peer feedback ${'$'}{feedback.id}. JSON length=${'$'}{feedbackJson.length}")
        } catch (e: Exception) {
            logger.e("Failed to save peer feedback to Firebase", e)
        }
    }

    private suspend fun saveCollaborativeChallengeToFirebase(challenge: CollaborativeChallenge) {
        try {
            // TODO: Persist when generic save API is added to FirebaseUserHelper
            val challengeJson = json.encodeToString(challenge)
            logger.d("Skipping Firebase save for collaborative challenge ${'$'}{challenge.id}. JSON length=${'$'}{challengeJson.length}")
        } catch (e: Exception) {
            logger.e("Failed to save collaborative challenge to Firebase", e)
        }
    }

    private suspend fun saveCommunityChallengeToFirebase(challenge: CommunityChallenge) {
        try {
            // TODO: Persist when generic save API is added to FirebaseUserHelper
            val challengeJson = json.encodeToString(challenge)
            logger.d("Skipping Firebase save for community challenge ${'$'}{challenge.id}. JSON length=${'$'}{challengeJson.length}")
        } catch (e: Exception) {
            logger.e("Failed to save community challenge to Firebase", e)
        }
    }

    private suspend fun saveLeaderboardEntryToFirebase(entry: LeaderboardEntry) {
        try {
            // TODO: Persist when generic save API is added to FirebaseUserHelper
            val entryJson = json.encodeToString(entry)
            logger.d("Skipping Firebase save for leaderboard entry ${'$'}{entry.userId}. JSON length=${'$'}{entryJson.length}")
        } catch (e: Exception) {
            logger.e("Failed to save leaderboard entry to Firebase", e)
        }
    }

    private suspend fun saveStudyGroupToFirebase(studyGroup: StudyGroup) {
        try {
            // TODO: Persist when generic save API is added to FirebaseUserHelper
            val groupJson = json.encodeToString(studyGroup)
            logger.d("Skipping Firebase save for study group ${'$'}{studyGroup.id}. JSON length=${'$'}{groupJson.length}")
        } catch (e: Exception) {
            logger.e("Failed to save study group to Firebase", e)
        }
    }

    private suspend fun getLeaderboardEntry(userId: String, category: LeaderboardType): LeaderboardEntry? {
        // TODO: Implement Firebase read when a generic get API is available in FirebaseUserHelper
        logger.w("getLeaderboardEntry: Generic Firebase get not available; returning null for userId=${'$'}userId category=${'$'}category")
        return null
    }

    private suspend fun recalculateLeaderboardRanks(category: LeaderboardType) {
        // Implementation for recalculating ranks would go here
        // This would involve fetching all entries for the category, sorting by points, and updating ranks
    }

    suspend fun getSocialProfile(userId: String): SocialProfile? {
        // TODO: Implement Firebase read when a generic get API is available in FirebaseUserHelper
        logger.w("getSocialProfile: Generic Firebase get not available; returning null for userId=${'$'}userId")
        return null
    }

    suspend fun getLeaderboard(category: LeaderboardType, limit: Int = 20): List<LeaderboardEntry> {
        // TODO: Implement Firebase read when a generic get API is available in FirebaseUserHelper
        logger.w("getLeaderboard: Generic Firebase get not available; returning empty list for category=${'$'}category")
        return emptyList()
    }

    suspend fun getActiveCollaborativeChallenges(): List<CollaborativeChallenge> {
        logger.w("getActiveCollaborativeChallenges: Generic Firebase get not available; returning empty list")
        return emptyList()
    }

    suspend fun getCurrentCommunityChallenge(): CommunityChallenge? {
        logger.w("getCurrentCommunityChallenge: Generic Firebase get not available; returning null")
        return null
    }

    // --- Helper stubs to resolve unresolved references ---

    private suspend fun getCollaborativeChallenge(challengeId: String): CollaborativeChallenge? {
        // TODO: Implement Firebase read when a generic get API is available
        logger.w("getCollaborativeChallenge: Generic Firebase get not available; returning null for id=$challengeId")
        return null
    }

    private fun extractStrengths(feedback: String): List<String> {
        // Very basic heuristic: split by sentences and pick positives
        return feedback.split('.', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() && ("good" in it.lowercase() || "well" in it.lowercase() || "nice" in it.lowercase()) }
    }

    private fun extractImprovements(feedback: String): List<String> {
        // Very basic heuristic: split by sentences and pick improvement cues
        return feedback.split('.', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() && ("improve" in it.lowercase() || "consider" in it.lowercase() || "could" in it.lowercase()) }
    }

    private suspend fun saveCodeReviewToFirebase(review: CodeReview) {
        try {
            val reviewJson = json.encodeToString(review)
            logger.d("Skipping Firebase save for code review ${'$'}{review.id}. JSON length=${'$'}{reviewJson.length}")
        } catch (e: Exception) {
            logger.e("Failed to save code review to Firebase", e)
        }
    }

    private fun updateReviewerReputation(reviewerId: String, overallScore: Float) {
        // Placeholder: map score to small reputation delta
        val delta = when {
            overallScore >= 4.5f -> 5
            overallScore >= 4.0f -> 3
            overallScore >= 3.0f -> 1
            else -> 0
        }
        updateUserReputation(reviewerId, delta)
    }

    private fun updateUserReputation(userId: String, delta: Int) {
        // TODO: Update reputation in Firebase when generic update API is available
        logger.d("Reputation update for user ${'$'}userId: +${'$'}delta (stub)")
    }

    private suspend fun saveSocialInteractionToFirebase(interaction: SocialInteraction) {
        try {
            val interactionJson = json.encodeToString(interaction)
            logger.d("Skipping Firebase save for social interaction ${'$'}{interaction.id}. JSON length=${'$'}{interactionJson.length}")
        } catch (e: Exception) {
            logger.e("Failed to save social interaction to Firebase", e)
        }
    }
}
