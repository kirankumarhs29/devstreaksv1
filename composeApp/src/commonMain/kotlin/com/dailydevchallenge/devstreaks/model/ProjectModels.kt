package com.dailydevchallenge.devstreaks.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

/**
 * User's progress on a specific project
 */
@Serializable
data class ProjectProgress(
    val id: String,
    val userId: String,
    val projectId: String,
    val currentPhaseId: String,
    val currentTaskId: String? = null,
    val completedPhases: List<String> = emptyList(),
    val completedTasks: Set<String> = emptySet(), // Changed to Set for easier lookup
    val overallProgress: Float = 0f, // Added missing property
    val totalTimeSpent: Int = 0, // minutes
    val lastActiveAt: Instant,
    val streak: Int = 0, // Added missing property
    val notes: String = "",
    val blockers: List<String> = emptyList() // Current challenges user is facing
)

/**
 * Task submission by user - Missing class needed by ProjectActivityCard
 */
@Serializable
data class ProjectTaskSubmission(
    val id: String,
    val userId: String,
    val taskId: String,
    val projectId: String,
    val submissionText: String,
    val attachments: List<String> = emptyList(), // File URLs or content
    val submittedAt: Instant,
    val status: SubmissionStatus = SubmissionStatus.PENDING,
    val feedback: String? = null,
    val score: Int? = null,
    val reviewedAt: Instant? = null,
    val reviewerId: String? = null
)

/**
 * Status of task submission
 */
@Serializable
enum class SubmissionStatus {
    PENDING,
    APPROVED,
    NEEDS_REVISION,
    REJECTED
}

/**
 * Real-world project composed of multiple LLM-generated challenges
 */
@Serializable
data class RealWorldProject(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val category: ProjectCategory,
    val difficultyLevel: DifficultyLevel,
    val estimatedDurationDays: Int,
    val skills: List<String> = emptyList(), // Skill node IDs this project develops
    val weakAreasAddressed: List<String> = emptyList(), // Weak area IDs this project targets
    val phases: List<ProjectPhase>,
    val totalTasks: Int,
    val completedTasks: Int = 0,
    val progress: Float = 0f,
    val xpReward: Int = 0,
    val status: ProjectStatus = ProjectStatus.NOT_STARTED,
    val createdAt: Instant,
    val lastUpdated: Instant = createdAt, // Added missing property
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val generatedByLLM: Boolean = true,
    val generationMetadata: ProjectGenerationMetadata? = null,
    // Added missing properties used by ProjectActivityCard
    val technologies: List<String> = emptyList(),
    val learningOutcomes: List<String> = emptyList(),
    val realWorldApplications: List<String> = emptyList(),
    val adaptiveMetadata: ProjectAdaptiveMetadata? = null
)

/**
 * Adaptive metadata for project generation
 */
@Serializable
data class ProjectAdaptiveMetadata(
    val generationPrompt: String,
    val userWeakAreas: List<WeakArea>,
    val targetSkills: List<String>,
    val adaptationReason: String
)

/**
 * Individual phase within a project
 */
@Serializable
data class ProjectPhase(
    val id: String,
    val title: String,
    val description: String,
    val order: Int,
    val estimatedDays: Int,
    val learningObjectives: List<String> = emptyList(), // Added missing property
    val tasks: List<ProjectTask>,
    val prerequisitePhases: List<String> = emptyList(),
    val isUnlocked: Boolean = false,
    val isCompleted: Boolean = false,
    val completedAt: Instant? = null,
    val createdAt: Instant
)

/**
 * Individual task within a project phase
 */
@Serializable
data class ProjectTask(
    val id: String,
    val title: String,
    val description: String,
    val type: ProjectTaskType,
    val order: Int = 0, // Added missing property
    val difficulty: DifficultyLevel = DifficultyLevel.MEDIUM,
    val estimatedMinutes: Int,
    val skillsAddressed: List<String> = emptyList(), // Added missing property (renamed from skillsRequired)
    val deliverables: List<String> = emptyList(), // Expected outputs
    val resources: List<LearningResource> = emptyList(), // Changed to LearningResource
    val acceptanceCriteria: List<String> = emptyList(), // Added missing property
    val hints: List<String> = emptyList(),
    val isCompleted: Boolean = false,
    val submissionUrl: String? = null,
    val feedback: String? = null,
    val completedAt: Instant? = null,
    val createdAt: Instant,
    val generatedByLLM: Boolean = true, // Added missing property
    val attempts: Int = 0,
    val xpReward: Int = 0
)

/**
 * Learning resource for tasks
 */
@Serializable
data class LearningResource(
    val title: String,
    val type: ResourceType,
    val url: String,
    val description: String
)

/**
 * Project categories for organization
 */
@Serializable
enum class ProjectCategory {
    WEB_DEVELOPMENT,
    MOBILE_DEVELOPMENT,
    DATA_SCIENCE,
    BACKEND_DEVELOPMENT,
    GAME_DEVELOPMENT,
    MACHINE_LEARNING,
    DEVOPS,
    FULL_STACK,
    API_DEVELOPMENT,
    DATABASE_DESIGN,
    SYSTEM_DESIGN,
    OPEN_SOURCE_CONTRIBUTION,
    PORTFOLIO_PROJECT
}

/**
 * Project completion status
 */
@Serializable
enum class ProjectStatus {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    ABANDONED
}

/**
 * Types of project tasks
 */
@Serializable
enum class ProjectTaskType {
    SETUP,
    IMPLEMENTATION,
    TESTING,
    DOCUMENTATION,
    DESIGN,
    RESEARCH,
    DEBUGGING,
    DEPLOYMENT,
    CODE_REVIEW,
    REFACTORING
}

/**
 * Learning resources for project tasks
 */
@Serializable
data class ProjectResource(
    val id: String,
    val title: String,
    val type: ResourceType,
    val url: String? = null,
    val content: String? = null,
    val description: String
)

@Serializable
enum class ResourceType {
    DOCUMENTATION,
    TUTORIAL,
    VIDEO,
    ARTICLE,
    CODE_EXAMPLE,
    REFERENCE,
    TOOL
}

/**
 * Metadata for LLM-generated projects
 */
@Serializable
data class ProjectGenerationMetadata(
    val generationPrompt: String,
    val userSkillLevel: String,
    val targetedWeakAreas: List<WeakArea>,
    val userInterests: List<String>,
    val timeConstraints: String,
    val adaptationReason: String,
    val relatedSkillNodes: List<String>
)

/**
 * Request for LLM project generation
 */
@Serializable
data class ProjectGenerationRequest(
    val userId: String,
    val userSkillLevel: String,
    val weakAreas: List<WeakArea>,
    val interests: List<String>,
    val timeAvailablePerDay: Int,
    val preferredDuration: Int, // days
    val skillsToFocus: List<String>, // Skill node IDs
    val projectCategory: ProjectCategory? = null
)

/**
 * LLM response for project generation
 */
@Serializable
data class ProjectGenerationResponse(
    val project: RealWorldProject,
    val generationRationale: String,
    val learningObjectives: List<String>,
    val successCriteria: List<String>
)
