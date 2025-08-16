package com.dailydevchallenge.devstreaks.service

import com.dailydevchallenge.devstreaks.llm.LLMService
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

/**
 * Core engine for managing LLM-generated skill trees and progression
 * Integrates with existing Phase 2 adaptive intelligence features
 */
class SkillTreeEngine(
    private val llmService: LLMService,
    private val challengeRepository: ChallengeRepository,
    private val weakAreaDetectionService: WeakAreaDetectionService,
    private val firebaseUserHelper: FirebaseUserHelper
) {
    private val logger = getLogger()
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val FIREBASE_SKILL_TREES_PATH = "skillTrees"
        private const val FIREBASE_SKILL_MASTERY_PATH = "skillMastery"
        private const val MIN_NODES_PER_TREE = 8
        private const val MAX_NODES_PER_TREE = 15
    }

    /**
     * Generate a personalized skill tree using LLM based on user performance and weak areas
     */
    suspend fun generatePersonalizedSkillTree(
        userId: String,
        learningGoals: List<String> = emptyList(),
        timeAvailablePerDay: Int = 30
    ): Result<SkillTree> = withContext(Dispatchers.Default) {
        try {
            logger.d("Generating personalized skill tree for user: $userId")

            // Get user's current performance metrics and weak areas
            val weakAreas = weakAreaDetectionService.detectWeakAreas(userId)
            val userMetrics = challengeRepository.getUserPerformanceSummary(userId)

            val skillLevel = determineUserSkillLevel(userMetrics)
            val currentMasteries = getCurrentSkillMasteries(userId)

            // Create LLM prompt for skill tree generation
            val prompt = buildSkillTreeGenerationPrompt(
                skillLevel = skillLevel,
                weakAreas = weakAreas,
                learningGoals = learningGoals,
                timeAvailablePerDay = timeAvailablePerDay,
                currentMasteries = currentMasteries
            )

            // Generate skill tree via LLM
            val llmResponse = llmService.generateResponse(listOf(
                com.dailydevchallenge.devstreaks.llm.ChatMessage(
                    role = "system",
                    content = "You are an expert programming education architect. Generate personalized skill trees based on user performance data."
                ),
                com.dailydevchallenge.devstreaks.llm.ChatMessage(
                    role = "user",
                    content = prompt
                )
            ))

            val generatedTree = parseSkillTreeFromLLMResponse(llmResponse, userId)

            // Store in Firebase and local database
            saveSkillTreeToFirebase(generatedTree)

            logger.i("Successfully generated skill tree with ${generatedTree.allNodes.size} nodes for user: $userId")
            Result.success(generatedTree)

        } catch (e: Exception) {
            logger.e("Failed to generate skill tree for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Update skill mastery based on challenge completion
     */
    suspend fun updateSkillMastery(
        userId: String,
        skillNodeId: String,
        challengeMetrics: ChallengeCompletionMetrics
    ): SkillMastery = withContext(Dispatchers.Default) {
        val currentMastery = getSkillMastery(userId, skillNodeId)

        // Calculate progress increase based on performance
        val progressIncrease = calculateProgressIncrease(challengeMetrics)
        val newProgress = (currentMastery.progress + progressIncrease).coerceAtMost(1.0f)

        // Determine new mastery level based on progress thresholds
        val newMasteryLevel = when {
            newProgress >= 0.8f -> MasteryLevel.EXPERT
            newProgress >= 0.6f -> MasteryLevel.ADVANCED
            newProgress >= 0.4f -> MasteryLevel.INTERMEDIATE
            newProgress >= 0.2f -> MasteryLevel.BEGINNER
            else -> MasteryLevel.NOVICE
        }

        val updatedMastery = currentMastery.copy(
            progress = newProgress,
            masteryLevel = newMasteryLevel,
            xpEarned = currentMastery.xpEarned + calculateXpFromMetrics(challengeMetrics),
            completedChallenges = currentMastery.completedChallenges + 1,
            timeSpentMinutes = currentMastery.timeSpentMinutes + (challengeMetrics.totalTime / 60000).toInt(),
            lastActivityAt = Clock.System.now(),
            practiceStreak = if (challengeMetrics.completed) currentMastery.practiceStreak + 1 else 0
        )

        // Save to Firebase
        saveSkillMasteryToFirebase(updatedMastery)

        // Check if we should unlock new nodes
        if (newMasteryLevel != currentMastery.masteryLevel) {
            unlockDependentNodes(userId, skillNodeId, newMasteryLevel)
        }

        updatedMastery
    }

    /**
     * Generate new skill nodes dynamically when user progresses
     */
    suspend fun expandSkillTree(
        userId: String,
        completedNodeId: String
    ): List<SkillNode> = withContext(Dispatchers.Default) {
        val weakAreas = weakAreaDetectionService.detectWeakAreas(userId)
        val userMetrics = challengeRepository.getUserPerformanceSummary(userId)

        val expansionPrompt = buildSkillExpansionPrompt(
            completedNodeId = completedNodeId,
            weakAreas = weakAreas,
            userLevel = determineUserSkillLevel(userMetrics)
        )

        val llmResponse = llmService.generateResponse(listOf(
            com.dailydevchallenge.devstreaks.llm.ChatMessage(
                role = "system",
                content = "Generate 2-3 new skill nodes that build upon the completed skill. Focus on addressing weak areas."
            ),
            com.dailydevchallenge.devstreaks.llm.ChatMessage(
                role = "user",
                content = expansionPrompt
            )
        ))

        parseNewSkillNodesFromLLM(llmResponse, userId, completedNodeId)
    }

    private fun buildSkillTreeGenerationPrompt(
        skillLevel: String,
        weakAreas: List<WeakArea>,
        learningGoals: List<String>,
        timeAvailablePerDay: Int,
        currentMasteries: Map<String, MasteryLevel>
    ): String {
        return """
        Generate a personalized programming skill tree for a $skillLevel developer.
        
        Current weak areas: ${weakAreas.joinToString { "${it.skillArea} (severity: ${it.severityScore})" }}
        Learning goals: ${learningGoals.joinToString()}
        Time available: $timeAvailablePerDay minutes/day
        Current masteries: ${currentMasteries.entries.joinToString { "${it.key}: ${it.value}" }}
        
        Create a skill tree with $MIN_NODES_PER_TREE-$MAX_NODES_PER_TREE nodes that:
        1. Addresses identified weak areas with higher priority
        2. Builds progressively from fundamentals to advanced concepts
        3. Includes practical, hands-on skills
        4. Balances theory with real-world application
        
        Return valid JSON with this structure:
        {
          "skillTree": {
            "id": "generated_id",
            "title": "Personalized Skill Tree",
            "description": "Custom learning path",
            "rootNodes": ["node_id1", "node_id2"],
            "allNodes": {
              "node_id1": {
                "title": "Node Title",
                "description": "Detailed description",
                "category": "PROGRAMMING_FUNDAMENTALS",
                "difficultyLevel": "BEGINNER",
                "prerequisites": [],
                "children": ["child_id"],
                "estimatedTimeMinutes": 45,
                "xpReward": 100,
                "isLocked": false,
                "masteryLevel": "NOVICE",
                "progress": 0.0,
                "createdAt": "${Clock.System.now()}",
                "generatedByLLM": true,
                "adaptiveMetadata": {
                  "generationPrompt": "skill_prompt",
                  "userWeakAreas": [],
                  "userSkillLevel": "$skillLevel",
                  "adaptationReason": "addresses_weak_areas",
                  "relatedConcepts": ["concept1", "concept2"]
                }
              }
            },
            "overallProgress": 0.0,
            "totalXpEarned": 0,
            "lastUpdated": "${Clock.System.now()}",
            "isActive": true
          },
          "generationRationale": "Explanation of why this tree was created",
          "recommendedLearningPath": ["node_id1", "node_id2"],
          "estimatedCompletionTime": 720
        }
        """.trimIndent()
    }

    private fun buildSkillExpansionPrompt(
        completedNodeId: String,
        weakAreas: List<WeakArea>,
        userLevel: String
    ): String {
        return """
        Generate 2-3 new skill nodes that build upon the completed skill: $completedNodeId
        User level: $userLevel
        Current weak areas: ${weakAreas.joinToString { it.skillArea }}
        
        Create nodes that:
        1. Build logically on the completed skill
        2. Address identified weak areas
        3. Maintain appropriate difficulty progression
        4. Include practical exercises
        
        Return JSON array of new skill nodes with same structure as above.
        """.trimIndent()
    }

    private suspend fun parseSkillTreeFromLLMResponse(
        llmResponse: String,
        userId: String
    ): SkillTree {
        return try {
            val response = json.decodeFromString<SkillTreeGenerationResponse>(llmResponse)
            response.skillTree.copy(
                id = generateUUID(),
                userId = userId,
                lastUpdated = Clock.System.now()
            )
        } catch (e: Exception) {
            logger.e("Failed to parse LLM skill tree response", e)
            // Fallback: create a basic skill tree
            createFallbackSkillTree(userId)
        }
    }

    private suspend fun parseNewSkillNodesFromLLM(
        llmResponse: String,
        userId: String,
        parentNodeId: String
    ): List<SkillNode> {
        return try {
            val nodes = json.decodeFromString<List<SkillNode>>(llmResponse)
            nodes.map { node ->
                node.copy(
                    id = generateUUID(),
                    createdAt = Clock.System.now(),
                    prerequisites = listOf(parentNodeId)
                )
            }
        } catch (e: Exception) {
            logger.e("Failed to parse new skill nodes from LLM", e)
            emptyList()
        }
    }

    private fun createFallbackSkillTree(userId: String): SkillTree {
        val nodeId = generateUUID()
        val node = SkillNode(
            id = nodeId,
            title = "Programming Fundamentals",
            description = "Master the basics of programming concepts",
            category = SkillTreeCategory.PROGRAMMING_FUNDAMENTALS,
            difficultyLevel = DifficultyLevel.BEGINNER,
            estimatedTimeMinutes = 60,
            xpReward = 150,
            isLocked = false,
            createdAt = Clock.System.now()
        )

        return SkillTree(
            id = generateUUID(),
            userId = userId,
            title = "Basic Programming Path",
            description = "Essential programming skills",
            rootNodes = listOf(nodeId),
            allNodes = mapOf(nodeId to node),
            lastUpdated = Clock.System.now()
        )
    }

    private suspend fun determineUserSkillLevel(userMetrics: UserPerformanceSummary?): String {
        return when {
            userMetrics == null -> "BEGINNER"
            userMetrics.successRate >= 0.8 -> "ADVANCED"
            userMetrics.successRate >= 0.6 -> "INTERMEDIATE"
            else -> "BEGINNER"
        }
    }

    private suspend fun getCurrentSkillMasteries(userId: String): Map<String, MasteryLevel> {
        // TODO: Implement Firebase read when a generic get API is available in FirebaseUserHelper
        logger.w("getCurrentSkillMasteries: Generic Firebase get not available; returning empty map for userId=$userId")
        return emptyMap()
    }

    private suspend fun getSkillMastery(userId: String, skillNodeId: String): SkillMastery {
        // TODO: Implement Firebase read when a generic get API is available in FirebaseUserHelper
        logger.w("getSkillMastery: Generic Firebase get not available; returning initial mastery for skillNodeId=$skillNodeId")
        return createInitialSkillMastery(userId, skillNodeId)
    }

    private fun createInitialSkillMastery(userId: String, skillNodeId: String): SkillMastery {
        return SkillMastery(
            id = generateUUID(),
            userId = userId,
            skillNodeId = skillNodeId,
            masteryLevel = MasteryLevel.NOVICE,
            progress = 0f,
            xpEarned = 0,
            completedChallenges = 0,
            timeSpentMinutes = 0,
            firstAttemptAt = Clock.System.now(),
            lastActivityAt = Clock.System.now()
        )
    }

    private fun calculateProgressIncrease(metrics: ChallengeCompletionMetrics): Float {
        return when {
            !metrics.completed -> 0f
            metrics.finalScore >= 0.9 -> 0.2f
            metrics.finalScore >= 0.7 -> 0.15f
            metrics.finalScore >= 0.5 -> 0.1f
            else -> 0.05f
        }
    }

    private fun calculateXpFromMetrics(metrics: ChallengeCompletionMetrics): Int {
        val baseXp = if (metrics.completed) 50 else 10
        val scoreBonus = (metrics.finalScore * 50).toInt()
        val speedBonus = if (metrics.totalTime < 300000) 25 else 0 // 5 min bonus
        return baseXp + scoreBonus + speedBonus
    }

    private suspend fun saveSkillTreeToFirebase(skillTree: SkillTree) {
        try {
            // TODO: Persist tree when generic save API is added to FirebaseUserHelper
            val treeJson = json.encodeToString(skillTree)
            logger.d("Skipping Firebase save for skillTree ${'$'}{skillTree.id}. JSON length=${'$'}{treeJson.length}")
        } catch (e: Exception) {
            logger.e("Failed to save skill tree to Firebase", e)
        }
    }

    private suspend fun saveSkillMasteryToFirebase(mastery: SkillMastery) {
        try {
            // TODO: Persist mastery when generic save API is added to FirebaseUserHelper
            val masteryJson = json.encodeToString(mastery)
            logger.d("Skipping Firebase save for mastery ${'$'}{mastery.skillNodeId}. JSON length=${'$'}{masteryJson.length}")
        } catch (e: Exception) {
            logger.e("Failed to save skill mastery to Firebase", e)
        }
    }

    private suspend fun unlockDependentNodes(
        userId: String,
        completedNodeId: String,
        newMasteryLevel: MasteryLevel
    ) {
        if (newMasteryLevel >= MasteryLevel.INTERMEDIATE) {
            // Generate new nodes that depend on this completed skill
            val newNodes = expandSkillTree(userId, completedNodeId)

            // Add new nodes to existing skill tree
            val currentTree = getActiveSkillTree(userId)
            if (currentTree != null) {
                val updatedNodes = currentTree.allNodes.toMutableMap()
                newNodes.forEach { node ->
                    updatedNodes[node.id] = node
                }

                val updatedTree = currentTree.copy(
                    allNodes = updatedNodes,
                    lastUpdated = Clock.System.now()
                )

                saveSkillTreeToFirebase(updatedTree)
            }
        }
    }

    suspend fun getActiveSkillTree(userId: String): SkillTree? {
        // TODO: Implement Firebase read when a generic get API is available in FirebaseUserHelper
        logger.w("getActiveSkillTree: Generic Firebase get not available; returning null for userId=$userId")
        return null
    }

    suspend fun getAllUserSkillMasteries(userId: String): Map<String, SkillMastery> {
        // TODO: Implement Firebase read when a generic get API is available in FirebaseUserHelper
        logger.w("getAllUserSkillMasteries: Generic Firebase get not available; returning empty map for userId=$userId")
        return emptyMap()
    }
}
