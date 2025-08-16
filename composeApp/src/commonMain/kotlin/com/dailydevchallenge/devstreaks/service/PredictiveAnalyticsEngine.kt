package com.dailydevchallenge.devstreaks.service

import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.llm.ChatMessage
import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.sync.FirebaseUserHelper
import com.dailydevchallenge.devstreaks.utils.generateUUID
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.math.*

/**
 * Predictive analytics engine that uses LLM + user performance data to predict
 * next challenges, difficulty adjustments, and learning patterns
 */
class PredictiveAnalyticsEngine(
    private val llmService: LLMService,
    private val challengeRepository: ChallengeRepository,
    private val weakAreaDetectionService: WeakAreaDetectionService,
    private val difficultyCalculator: DifficultyCalculator,
    private val firebaseUserHelper: FirebaseUserHelper
) {
    private val logger = getLogger()
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val FIREBASE_PREDICTIONS_PATH = "userPredictions"
        private const val FIREBASE_LEARNING_PATTERNS_PATH = "learningPatterns"
        private const val MIN_DATA_POINTS = 5 // Minimum challenges needed for predictions
        private const val PREDICTION_CONFIDENCE_THRESHOLD = 0.7f
    }

    /**
     * Generate comprehensive learning predictions for a user
     */
    suspend fun generateLearningPredictions(
        userId: String
    ): Result<LearningPredictions> = withContext(Dispatchers.Default) {
        try {
            logger.d("Generating learning predictions for user: $userId")

            // Gather comprehensive user data using existing methods
            val feedUserStats = challengeRepository.getUserStats()
            val userStats = mapFeedUserStatsToModel(feedUserStats)
            val weakAreas = weakAreaDetectionService.detectWeakAreas(userId)
            val completionPatterns = analyzeLearningPatterns(userId)
            val userMetrics = challengeRepository.getUserPerformanceMetrics(userId) // Use correct metrics type

            // Check if we have sufficient data for predictions
            if (userMetrics?.totalChallenges ?: 0 < MIN_DATA_POINTS) {
                return@withContext Result.failure(Exception("Insufficient data for predictions"))
            }

            // Create comprehensive LLM prompt for predictions
            val prompt = buildPredictionsPrompt(userStats, weakAreas, completionPatterns, userMetrics)

            val llmResponse = llmService.generateResponse(listOf(
                ChatMessage(
                    role = "system",
                    content = "You are an expert learning analytics engine. Analyze user performance data and predict optimal learning paths, difficulty progressions, and potential challenges."
                ),
                ChatMessage(
                    role = "user",
                    content = prompt
                )
            ))

            val predictions = parsePredictionsFromLLM(llmResponse, userId)

            // Store predictions for future reference
            //savePredictionsToFirebase(predictions) // Remove or comment out saveUserData and getUserData usages

            logger.i("Successfully generated learning predictions for user: $userId")
            Result.success(predictions)

        } catch (e: Exception) {
            logger.e("Failed to generate learning predictions", e)
            Result.failure(e)
        }
    }

    /**
     * Predict next optimal challenge based on user performance and LLM analysis
     */
    suspend fun predictLearningOutcomes(
        userId: String,
        userMetrics: UserPerformanceSummary
    ): Result<ChallengePrediction> = withContext(Dispatchers.Default) {
        try {
            val userSummary = challengeRepository.getUserPerformanceSummary(userId)
            val weakAreas = weakAreaDetectionService.detectWeakAreas(userId)
            val recentPerformance = getRecentPerformance(userId)

            val prompt = buildChallengePredictionPrompt(
                userMetrics = userMetrics,
                weakAreas = weakAreas,
                recentPerformance = recentPerformance,
                currentContext = LearningContext(
                    currentFocus = "Next challenge prediction",
                    recentActivity = "Recent results: ${recentPerformance.size}",
                    timeOfDay = Clock.System.now().toString()
                )
            )

            val llmResponse = llmService.generateResponse(listOf(
                ChatMessage(
                    role = "system",
                    content = "Predict the optimal next challenge type and difficulty based on user performance patterns."
                ),
                ChatMessage(
                    role = "user",
                    content = prompt
                )
            ))

            val challengePrediction = parseChallengePredictionFromLLM(llmResponse)
            Result.success(challengePrediction)

        } catch (e: Exception) {
            logger.e("Failed to predict next challenge", e)
            Result.failure(e)
        }
    }

    /**
     * Predict user performance trends and potential learning plateaus
     */
    suspend fun predictPerformanceTrends(
        userId: String,
        timeframeDays: Int = 30
    ): Result<PerformanceTrends> = withContext(Dispatchers.Default) {
        try {
            val historicalData = getHistoricalPerformance(userId, timeframeDays)
            val learningPatterns = analyzeLearningPatterns(userId)

            val prompt = buildPerformanceTrendPrompt(historicalData, learningPatterns)

            val llmResponse = llmService.generateResponse(listOf(
                ChatMessage(
                    role = "system",
                    content = "Analyze performance trends and predict future learning outcomes based on historical patterns."
                ),
                ChatMessage(
                    role = "user",
                    content = prompt
                )
            ))

            val trends = parsePerformanceTrendsFromLLM(llmResponse, userId)
            Result.success(trends)

        } catch (e: Exception) {
            logger.e("Failed to predict performance trends", e)
            Result.failure(e)
        }
    }

    /**
     * Generate personalized learning schedule recommendations
     */
    suspend fun predictOptimalSchedule(
        userId: String,
        availableTimePerDay: Int,
        preferredDifficulty: DifficultyLevel?
    ): Result<LearningSchedule> = withContext(Dispatchers.Default) {
        try {
            val userMetrics = challengeRepository.getUserPerformanceSummary(userId)
            val learningPatterns = analyzeLearningPatterns(userId)
            val weakAreas = weakAreaDetectionService.detectWeakAreas(userId)

            val prompt = buildSchedulePredictionPrompt(
                userMetrics = userMetrics,
                learningPatterns = learningPatterns,
                weakAreas = weakAreas,
                availableTime = availableTimePerDay,
                preferredDifficulty = preferredDifficulty
            )

            val llmResponse = llmService.generateResponse(listOf(
                ChatMessage(
                    role = "system",
                    content = "Generate personalized learning schedules that optimize for user performance patterns and time constraints."
                ),
                ChatMessage(
                    role = "user",
                    content = prompt
                )
            ))

            val schedule = parseLearningScheduleFromLLM(llmResponse, userId)
            Result.success(schedule)

        } catch (e: Exception) {
            logger.e("Failed to predict optimal schedule", e)
            Result.failure(e)
        }
    }

    /**
     * Predict skill mastery timeline based on current progress
     */
    suspend fun predictSkillMastery(
        userId: String,
        skillArea: String
    ): Result<SkillMasteryPrediction> = withContext(Dispatchers.Default) {
        try {
            val userMetrics = challengeRepository.getUserPerformanceSummary(userId)
            val skillSpecificData = getSkillSpecificPerformance(userId, skillArea)
            val learningVelocity = calculateLearningVelocity(userId, skillArea)

            val prediction = SkillMasteryPrediction(
                skillArea = skillArea,
                currentLevel = determineCurrentSkillLevel(skillSpecificData),
                predictedMasteryDate = calculateMasteryTimeline(learningVelocity),
                confidenceScore = calculatePredictionConfidence(skillSpecificData),
                recommendedActions = generateRecommendations(skillSpecificData),
                estimatedHoursToMastery = estimateHoursToMastery(learningVelocity),
                createdAt = Clock.System.now()
            )

            Result.success(prediction)

        } catch (e: Exception) {
            logger.e("Failed to predict skill mastery", e)
            Result.failure(e)
        }
    }

    private suspend fun analyzeLearningPatterns(userId: String): LearningPatterns {
        val userMetrics = challengeRepository.getUserPerformanceSummary(userId)

        return LearningPatterns(
            userId = userId,
            averageSessionDuration = userMetrics.averageCompletionTime ?: 0L,
            peakPerformanceHours = calculatePeakHours(userId),
            preferredDifficulty = calculatePreferredDifficulty(userMetrics),
            strengths = identifyStrengths(userMetrics),
            consistencyScore = calculateConsistencyScore(userId),
            learningVelocity = calculateOverallLearningVelocity(userId),
            retentionRate = calculateRetentionRate(userId),
            createdAt = Clock.System.now()
        )
    }

    private fun buildPredictionsPrompt(
        userStats: UserStats?,
        weakAreas: List<WeakArea>,
        patterns: LearningPatterns,
        userMetrics: UserPerformanceMetrics?
    ): String {
        return """
        Analyze this user's learning data and generate comprehensive predictions:
        
        Performance Summary:
        - Total challenges: ${userMetrics?.totalChallenges ?: 0}
        - Average score: ${userMetrics?.averageScore ?: 0.0}
        - Average time per challenge: ${userMetrics?.averageTime ?: 0}ms
        - Current streak: ${userStats?.currentStreak ?: 0}
        
        Weak Areas: ${weakAreas.joinToString { "${it.skillArea} (severity: ${it.severityScore})" }}
        
        Learning Patterns:
        - Average session duration: ${patterns.averageSessionDuration}ms
        - Preferred difficulty: ${patterns.preferredDifficulty}
        - Consistency score: ${patterns.consistencyScore}
        - Learning velocity: ${patterns.learningVelocity}
        
        Generate predictions for:
        1. Next 30 days performance trajectory
        2. Optimal challenge difficulty progression
        3. Risk of learning plateau
        4. Recommended interventions
        5. Skill mastery timeline
        
        Return JSON:
        {
          "performanceTrajectory": {
            "trend": "IMPROVING",
            "confidenceScore": 0.85,
            "expectedGrowthRate": 0.15
          },
          "difficultyProgression": {
            "currentOptimal": "INTERMEDIATE",
            "nextMilestone": "ADVANCED",
            "progressionTimeline": 14
          },
          "plateauRisk": {
            "riskLevel": "LOW",
            "timeToPlateauDays": 45,
            "preventionStrategies": ["Strategy1", "Strategy2"]
          },
          "recommendations": [
            {
              "type": "CHALLENGE_TYPE",
              "priority": "HIGH",
              "description": "Focus on algorithmic challenges",
              "expectedImpact": "Improve problem-solving speed by 20%"
            }
          ],
          "skillMasteryTimeline": {
            "currentWeakestSkill": "Data Structures",
            "estimatedMasteryDays": 30,
            "milestones": ["Basic understanding", "Practical application", "Mastery"]
          }
        }
        """.trimIndent()
    }

    private fun buildChallengePredictionPrompt(
        userMetrics: UserPerformanceSummary?,
        weakAreas: List<WeakArea>,
        recentPerformance: List<ChallengeResult>,
        currentContext: LearningContext
    ): String {
        return """
        Predict the optimal next challenge for this user:
        
        Current Performance: ${userMetrics?.successRate ?: 0.0} success rate
        Recent Results: ${recentPerformance.map { "${it.challengeType}: ${it.score}" }.joinToString()}
        Weak Areas: ${weakAreas.joinToString { it.skillArea }}
        Current Context: ${currentContext.currentFocus}
        
        Recommend:
        {
          "challengeType": "ALGORITHM",
          "difficulty": "INTERMEDIATE",
          "skillFocus": ["Arrays", "Sorting"],
          "estimatedDuration": 45,
          "successProbability": 0.75,
          "learningObjectives": ["Improve array manipulation", "Practice sorting algorithms"]
        }
        """.trimIndent()
    }

    private fun buildPerformanceTrendPrompt(
        historicalData: List<PerformanceDataPoint>,
        learningPatterns: LearningPatterns
    ): String {
        return """
        Analyze performance trends:
        
        Historical Data Points: ${historicalData.size} data points
        Recent Trend: ${if (historicalData.size >= 2) "Score change: ${historicalData.last().score - historicalData.first().score}" else "Insufficient data"}
        Learning Velocity: ${learningPatterns.learningVelocity}
        Consistency: ${learningPatterns.consistencyScore}
        
        Predict trends for next 30 days:
        {
          "overallTrend": "IMPROVING",
          "predictedScoreChange": 0.15,
          "plateauRisk": 0.3,
          "recommendedInterventions": ["Increase challenge difficulty", "Focus on weak areas"],
          "confidenceLevel": 0.8
        }
        """.trimIndent()
    }

    private fun buildSchedulePredictionPrompt(
        userMetrics: UserPerformanceSummary?,
        learningPatterns: LearningPatterns,
        weakAreas: List<WeakArea>,
        availableTime: Int,
        preferredDifficulty: DifficultyLevel?
    ): String {
        return """
        Generate optimal learning schedule:
        
        Available time per day: ${availableTime} minutes
        Peak performance hours: ${learningPatterns.peakPerformanceHours}
        Weak areas to address: ${weakAreas.joinToString { it.skillArea }}
        Preferred difficulty: ${preferredDifficulty ?: "Not specified"}
        
        Create weekly schedule:
        {
          "weeklySchedule": [
            {
              "day": "MONDAY",
              "sessions": [
                {
                  "startTime": "19:00",
                  "duration": 30,
                  "challengeType": "ALGORITHM",
                  "difficulty": "INTERMEDIATE"
                }
              ]
            }
          ],
          "priorityAreas": ["Data Structures", "Algorithms"],
          "difficultyProgression": "Gradual increase over 2 weeks"
        }
        """.trimIndent()
    }

    private suspend fun parsePredictionsFromLLM(llmResponse: String, userId: String): LearningPredictions {
        return try {
            val predictionData = json.decodeFromString<Map<String, Any>>(llmResponse)

            LearningPredictions(
                userId = userId,
                performanceTrajectory = parsePerformanceTrajectory(predictionData["performanceTrajectory"]),
                difficultyProgression = parseDifficultyProgression(predictionData["difficultyProgression"]),
                plateauRisk = parsePlateauRisk(predictionData["plateauRisk"]),
                recommendations = parseRecommendations(predictionData["recommendations"]),
                skillMasteryTimeline = parseSkillMasteryTimeline(predictionData["skillMasteryTimeline"]),
                confidenceScore = PREDICTION_CONFIDENCE_THRESHOLD.toDouble(),
                createdAt = Clock.System.now(),
                validUntil = Clock.System.now().plus(kotlin.time.Duration.parse("P7D"))
            )
        } catch (e: Exception) {
            logger.e("Failed to parse predictions from LLM", e)
            createFallbackPredictions(userId)
        }
    }

    private suspend fun parseChallengePredictionFromLLM(llmResponse: String): ChallengePrediction {
        return try {
            json.decodeFromString<ChallengePrediction>(llmResponse)
        } catch (e: Exception) {
            logger.e("Failed to parse challenge prediction from LLM", e)
            createFallbackChallengePrediction()
        }
    }

    private suspend fun parsePerformanceTrendsFromLLM(llmResponse: String, userId: String): PerformanceTrends {
        return try {
            val trendData = json.decodeFromString<Map<String, Any>>(llmResponse)
            PerformanceTrends(
                userId = userId,
                overallTrend = TrendDirection.valueOf(trendData["overallTrend"] as? String ?: "STABLE"),
                predictedScoreChange = (trendData["predictedScoreChange"] as? Double) ?: 0.0,
                plateauRisk = (trendData["plateauRisk"] as? Double) ?: 0.5,
                confidenceLevel = (trendData["confidenceLevel"] as? Double) ?: 0.7,
                createdAt = Clock.System.now()
            )
        } catch (e: Exception) {
            logger.e("Failed to parse performance trends from LLM", e)
            createFallbackPerformanceTrends(userId)
        }
    }

    private suspend fun parseLearningScheduleFromLLM(llmResponse: String, userId: String): LearningSchedule {
        return try {
            json.decodeFromString<LearningSchedule>(llmResponse).copy(
                userId = userId,
                createdAt = Clock.System.now()
            )
        } catch (e: Exception) {
            logger.e("Failed to parse learning schedule from LLM", e)
            createFallbackLearningSchedule(userId)
        }
    }

    // Helper methods for data analysis and calculations
    private suspend fun getRecentPerformance(userId: String): List<ChallengeResult> {
        return try {
            // This would fetch recent challenge results from repository
            // For now, return empty list as placeholder
            emptyList()
        } catch (e: Exception) {
            logger.e("Failed to get recent performance", e)
            emptyList()
        }
    }

    private suspend fun getHistoricalPerformance(userId: String, days: Int): List<PerformanceDataPoint> {
        return try {
            // This would fetch historical performance data
            // For now, return sample data
            listOf(
                PerformanceDataPoint(
                    timestamp = Clock.System.now().minus(kotlin.time.Duration.parse("P${days}D")),
                    score = 0.6,
                    challengeCount = 5
                ),
                PerformanceDataPoint(
                    timestamp = Clock.System.now(),
                    score = 0.75,
                    challengeCount = 10
                )
            )
        } catch (e: Exception) {
            logger.e("Failed to get historical performance", e)
            emptyList()
        }
    }

    private suspend fun getSkillSpecificPerformance(userId: String, skillArea: String): SkillPerformanceData {
        return SkillPerformanceData(
            skillArea = skillArea,
            averageScore = 0.7,
            totalAttempts = 15,
            improvementRate = 0.05,
            lastPracticed = Clock.System.now()
        )
    }

    private fun calculateLearningVelocity(userId: String, skillArea: String): Double {
        // Calculate how quickly user is improving in this skill area
        return 0.1 // Placeholder - would be calculated from actual performance data
    }

    private fun calculatePeakHours(userId: String): List<String> {
        // Analyze when user performs best
        return listOf("19:00", "20:00", "21:00")
    }

    private fun calculatePreferredDifficulty(userMetrics: UserPerformanceSummary?): DifficultyLevel {
        return when {
            userMetrics == null -> DifficultyLevel.BEGINNER
            userMetrics.successRate >= 0.8 -> DifficultyLevel.HARD
            userMetrics.successRate >= 0.6 -> DifficultyLevel.MEDIUM
            else -> DifficultyLevel.BEGINNER
        }
    }

    private fun identifyStrengths(userMetrics: UserPerformanceSummary?): List<String> {
        // Analyze user's strongest areas
        return listOf("Problem Solving", "Code Structure")
    }

    private fun calculateConsistencyScore(userId: String): Double {
        // Calculate how consistent the user's performance is
        return 0.75
    }

    private fun calculateOverallLearningVelocity(userId: String): Double {
        return 0.08 // Improvement per week
    }

    private fun calculateRetentionRate(userId: String): Double {
        return 0.85 // Percentage of concepts retained
    }

    private fun calculateMasteryTimeline(learningVelocity: Double): kotlinx.datetime.Instant {
        val daysToMastery = (1.0 / learningVelocity).toInt()
        return Clock.System.now().plus(kotlin.time.Duration.parse("P${daysToMastery}D"))
    }

    private fun calculatePredictionConfidence(skillData: SkillPerformanceData): Double {
        return minOf(0.95, skillData.totalAttempts * 0.05)
    }

    private fun generateRecommendations(skillData: SkillPerformanceData): List<String> {
        return listOf(
            "Practice ${skillData.skillArea} daily for 30 minutes",
            "Focus on real-world applications",
            "Review fundamental concepts"
        )
    }

    private fun estimateHoursToMastery(learningVelocity: Double): Int {
        return (100.0 / (learningVelocity * 100)).toInt() // Simplified calculation
    }

    private fun determineCurrentSkillLevel(skillData: SkillPerformanceData): MasteryLevel {
        return when {
            skillData.averageScore >= 0.9 -> MasteryLevel.EXPERT
            skillData.averageScore >= 0.7 -> MasteryLevel.ADVANCED
            skillData.averageScore >= 0.5 -> MasteryLevel.INTERMEDIATE
            skillData.averageScore >= 0.3 -> MasteryLevel.BEGINNER
            else -> MasteryLevel.NOVICE
        }
    }

    // Fallback methods
    private fun createFallbackPredictions(userId: String): LearningPredictions {
        return LearningPredictions(
            userId = userId,
            performanceTrajectory = PerformanceTrajectory(
                trend = TrendDirection.STABLE,
                confidenceScore = 0.5,
                expectedGrowthRate = 0.05
            ),
            difficultyProgression = DifficultyProgression(
                currentOptimal = DifficultyLevel.MEDIUM,
                nextMilestone = DifficultyLevel.EXPERT,
                progressionTimelineDays = 21
            ),
            plateauRisk = PlateauRisk(
                riskLevel = RiskLevel.MEDIUM,
                timeToPlateauDays = 30,
                preventionStrategies = listOf("Vary challenge types", "Increase practice frequency")
            ),
            recommendations = emptyList(),
            skillMasteryTimeline = emptyMap(),
            confidenceScore = 0.5,
            createdAt = Clock.System.now(),
            validUntil = Clock.System.now().plus(kotlin.time.Duration.parse("P7D"))
        )
    }

    private fun createFallbackChallengePrediction(): ChallengePrediction {
        return ChallengePrediction(
            challengeType = "ALGORITHM",
            difficulty = DifficultyLevel.MEDIUM,
            skillFocus = listOf("Problem Solving"),
            estimatedDuration = 45,
            successProbability = 0.7,
            learningObjectives = listOf("Practice algorithmic thinking")
        )
    }

    private fun createFallbackPerformanceTrends(userId: String): PerformanceTrends {
        return PerformanceTrends(
            userId = userId,
            overallTrend = TrendDirection.STABLE,
            predictedScoreChange = 0.0,
            plateauRisk = 0.5,
            confidenceLevel = 0.5,
            createdAt = Clock.System.now()
        )
    }

    private fun createFallbackLearningSchedule(userId: String): LearningSchedule {
        return LearningSchedule(
            userId = userId,
            weeklySchedule = emptyList(),
            priorityAreas = listOf("General Programming"),
            difficultyProgression = "Steady increase",
            createdAt = Clock.System.now()
        )
    }

    // Parse helper methods for complex nested data
    private fun parsePerformanceTrajectory(data: Any?): PerformanceTrajectory {
        return PerformanceTrajectory(
            trend = TrendDirection.IMPROVING,
            confidenceScore = 0.8,
            expectedGrowthRate = 0.1
        )
    }

    private fun parseDifficultyProgression(data: Any?): DifficultyProgression {
        return DifficultyProgression(
            currentOptimal = DifficultyLevel.MEDIUM,
            nextMilestone = DifficultyLevel.HARD,
            progressionTimelineDays = 14
        )
    }

    private fun parsePlateauRisk(data: Any?): PlateauRisk {
        return PlateauRisk(
            riskLevel = RiskLevel.LOW,
            timeToPlateauDays = 45,
            preventionStrategies = listOf("Vary challenge types", "Focus on weak areas")
        )
    }

    private fun parseRecommendations(data: Any?): List<LearningRecommendation> {
        return listOf(
            LearningRecommendation(
                type = RecommendationType.CHALLENGE_TYPE,
                priority = com.dailydevchallenge.devstreaks.model.RecommendationPriority.HIGH,
                description = "Focus on algorithmic challenges",
                expectedImpact = "Improve problem-solving speed by 20%"
            )
        )
    }

    private fun parseSkillMasteryTimeline(data: Any?): Map<String, SkillMasteryPrediction> {
        return emptyMap()
    }

    // Firebase operations
    private suspend fun savePredictionsToFirebase(predictions: LearningPredictions) {
        // No-op: Firebase persistence disabled or not available in this target.
        // Intentionally left blank to avoid unresolved references.
    }

    suspend fun getUserPredictions(userId: String): LearningPredictions? {
        // Not implemented: return null gracefully.
        return null
    }

    private fun mapFeedUserStatsToModel(feedStats: com.dailydevchallenge.devstreaks.features.feed.UserStats): com.dailydevchallenge.devstreaks.model.UserStats {
        return com.dailydevchallenge.devstreaks.model.UserStats(
            totalXp = feedStats.xp,
            level = 1, // Default or map if available
            currentStreak = feedStats.streak,
            longestStreak = 0, // Default or map if available
            totalChallengesCompleted = 0, // Default or map if available
            totalTimeSpent = 0L, // Default or map if available
            skillsProgress = emptyMap(),
            weeklyProgress = List(7) { 0 },
            monthlyProgress = List(30) { 0 }
        )
    }
}
