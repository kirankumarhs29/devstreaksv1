package com.dailydevchallenge.devstreaks.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

/**
 * Represents a skill node in the skill tree
 */
@Serializable
data class SkillNode(
    val id: String,
    val title: String,
    val description: String,
    val category: SkillTreeCategory,
    val difficultyLevel: DifficultyLevel,
    val prerequisites: List<String> = emptyList(),
    val children: List<String> = emptyList(),
    val estimatedTimeMinutes: Int,
    val xpReward: Int,
    val isLocked: Boolean = true,
    val masteryLevel: MasteryLevel = MasteryLevel.NOVICE,
    val progress: Float = 0f,
    val createdAt: Instant,
    val generatedByLLM: Boolean = true,
    val adaptiveMetadata: SkillNodeMetadata? = null
)

/**
 * Categories for organizing skill nodes
 */
@Serializable
enum class SkillTreeCategory {
    PROGRAMMING_FUNDAMENTALS,
    DATA_STRUCTURES,
    ALGORITHMS,
    SOFTWARE_DESIGN,
    WEB_DEVELOPMENT,
    MOBILE_DEVELOPMENT,
    DATABASE_DESIGN,
    SYSTEM_DESIGN,
    DEBUGGING,
    TESTING,
    DEVOPS,
    SECURITY,
    PROJECT_MANAGEMENT,
    COMMUNICATION,
    PROBLEM_SOLVING
}

/**
 * Metadata for LLM-generated skill nodes
 */
@Serializable
data class SkillNodeMetadata(
    val generationPrompt: String,
    val userWeakAreas: List<WeakArea>,
    val userSkillLevel: String,
    val adaptationReason: String,
    val relatedConcepts: List<String>
)

/**
 * Complete skill tree structure
 */
@Serializable
data class SkillTree(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val rootNodes: List<String>,
    val allNodes: Map<String, SkillNode>,
    val overallProgress: Float = 0f,
    val totalXpEarned: Int = 0,
    val lastUpdated: Instant,
    val isActive: Boolean = true
)

/**
 * User's mastery progress for a specific skill
 */
@Serializable
data class SkillMastery(
    val id: String,
    val userId: String,
    val skillNodeId: String,
    val masteryLevel: MasteryLevel,
    val progress: Float,
    val xpEarned: Int,
    val completedChallenges: Int,
    val timeSpentMinutes: Int,
    val firstAttemptAt: Instant,
    val lastActivityAt: Instant,
    val practiceStreak: Int = 0
)

/**
 * Skill tree generation request for LLM
 */
@Serializable
data class SkillTreeGenerationRequest(
    val userId: String,
    val userSkillLevel: String,
    val weakAreas: List<WeakArea>,
    val learningGoals: List<String>,
    val timeAvailablePerDay: Int,
    val preferredCategories: List<SkillTreeCategory>,
    val currentMasteryLevels: Map<String, MasteryLevel>
)

/**
 * LLM response for skill tree generation
 */
@Serializable
data class SkillTreeGenerationResponse(
    val skillTree: SkillTree,
    val generationRationale: String,
    val recommendedLearningPath: List<String>,
    val estimatedCompletionTime: Int
)
