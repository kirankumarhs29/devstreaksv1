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

/**
 * Project-based learning engine that generates real-world projects using LLM
 * Integrates with skill trees and adaptive intelligence from Phase 2
 */
class ProjectBasedLearningEngine(
    private val llmService: LLMService,
    private val skillTreeEngine: SkillTreeEngine,
    private val weakAreaDetectionService: WeakAreaDetectionService,
    private val challengeRepository: ChallengeRepository,
    private val firebaseUserHelper: FirebaseUserHelper
) {
    private val logger = getLogger()
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val FIREBASE_PROJECTS_PATH = "realWorldProjects"
        private const val FIREBASE_PROJECT_PROGRESS_PATH = "projectProgress"
        private const val FIREBASE_PROJECT_SUBMISSIONS_PATH = "projectSubmissions"
        private const val MIN_TASKS_PER_PHASE = 3
        private const val MAX_TASKS_PER_PHASE = 8
        private const val MIN_PHASES_PER_PROJECT = 2
        private const val MAX_PHASES_PER_PROJECT = 5
    }

    /**
     * Generate a personalized real-world project based on user skills and weak areas
     */
    suspend fun generatePersonalizedProject(
        userId: String,
        interests: List<String> = emptyList(),
        timeAvailablePerDay: Int = 60,
        preferredDuration: Int = 14,
        category: ProjectCategory? = null
    ): Result<RealWorldProject> = withContext(Dispatchers.Default) {
        try {
            logger.d("Generating personalized project for user: $userId")

            // Gather user context from Phase 2 services
            val weakAreas = weakAreaDetectionService.detectWeakAreas(userId)
            val userMetrics = challengeRepository.getUserPerformanceMetrics(userId)
            val skillLevel = determineUserSkillLevel(userMetrics)

            // Get relevant skill nodes to focus on
            val skillsToFocus = getRelevantSkillsForProject(userId, weakAreas, category)

            // Create comprehensive LLM prompt for project generation
            val prompt = buildProjectGenerationPrompt(
                skillLevel = skillLevel,
                weakAreas = weakAreas,
                interests = interests,
                timeAvailablePerDay = timeAvailablePerDay,
                preferredDuration = preferredDuration,
                skillsToFocus = skillsToFocus,
                category = category
            )

            // Generate project via LLM
            val llmResponse = llmService.generateResponse(listOf(
                ChatMessage(
                    role = "system",
                    content = "You are an expert software engineering mentor who creates real-world projects that address specific learning gaps and build practical skills."
                ),
                ChatMessage(
                    role = "user",
                    content = prompt
                )
            ))

            val generatedProject = parseProjectFromLLMResponse(llmResponse, userId)

            // Store in Firebase and local database
            saveProjectToFirebase(generatedProject)

            // Create initial project progress
            val initialProgress = ProjectProgress(
                id = generateUUID(),
                userId = userId,
                projectId = generatedProject.id,
                currentPhaseId = generatedProject.phases.first().id,
                lastActiveAt = Clock.System.now()
            )
            saveProjectProgressToFirebase(initialProgress)

            logger.i("Successfully generated project '${generatedProject.title}' with ${generatedProject.totalTasks} tasks")
            Result.success(generatedProject)

        } catch (e: Exception) {
            logger.e("Failed to generate project for user: $userId", e)
            Result.failure(e)
        }
    }

    /**
     * Submit a project task and update progress
     */
    suspend fun submitProjectTask(
        userId: String,
        projectId: String,
        taskId: String,
        submission: ProjectTaskSubmission
    ): Result<ProjectProgress> = withContext(Dispatchers.Default) {
        try {
            // Save submission
            saveTaskSubmissionToFirebase(submission)

            // Update project progress
            val currentProgress = getProjectProgress(userId, projectId)
            val project = getProject(userId, projectId)

            if (project != null && currentProgress != null) {
                val updatedProgress = calculateProgressAfterSubmission(
                    project = project,
                    currentProgress = currentProgress,
                    completedTaskId = taskId,
                    submissionQuality = evaluateSubmissionQuality(submission)
                )

                saveProjectProgressToFirebase(updatedProgress)

                // Update skill mastery based on task completion
                updateSkillMasteryFromProject(userId, project, taskId, submission)

                Result.success(updatedProgress)
            } else {
                Result.failure(Exception("Project or progress not found"))
            }
        } catch (e: Exception) {
            logger.e("Failed to submit project task", e)
            Result.failure(e)
        }
    }

    /**
     * Generate additional project phases based on user progress
     */
    suspend fun expandProjectWithNewPhase(
        userId: String,
        projectId: String,
        completedPhaseId: String
    ): Result<ProjectPhase> = withContext(Dispatchers.Default) {
        try {
            val project = getProject(userId, projectId)
            val weakAreas = weakAreaDetectionService.detectWeakAreas(userId)

            if (project != null) {
                val expansionPrompt = buildPhaseExpansionPrompt(
                    project = project,
                    completedPhaseId = completedPhaseId,
                    weakAreas = weakAreas
                )

                val llmResponse = llmService.generateResponse(listOf(
                    ChatMessage(
                        role = "system",
                        content = "Generate a new project phase that builds upon completed work and addresses remaining weak areas."
                    ),
                    ChatMessage(
                        role = "user",
                        content = expansionPrompt
                    )
                ))

                val newPhase = parseProjectPhaseFromLLM(llmResponse, projectId)

                // Add to existing project
                val updatedProject = project.copy(
                    phases = project.phases + newPhase,
                    totalTasks = project.totalTasks + newPhase.tasks.size,
                    lastUpdated = Clock.System.now()
                )

                saveProjectToFirebase(updatedProject)

                Result.success(newPhase)
            } else {
                Result.failure(Exception("Project not found"))
            }
        } catch (e: Exception) {
            logger.e("Failed to expand project", e)
            Result.failure(e)
        }
    }

    private fun buildProjectGenerationPrompt(
        skillLevel: String,
        weakAreas: List<WeakArea>,
        interests: List<String>,
        timeAvailablePerDay: Int,
        preferredDuration: Int,
        skillsToFocus: List<SkillNode>,
        category: ProjectCategory?
    ): String {
        return """
        Generate a comprehensive real-world software project for a $skillLevel developer.
        
        Current weak areas to address: ${weakAreas.joinToString { "${it.skillArea} (severity: ${it.severityScore})" }}
        Interests: ${interests.joinToString()}
        Time available: $timeAvailablePerDay minutes/day for $preferredDuration days
        Skills to focus on: ${skillsToFocus.joinToString { it.title }}
        ${category?.let { "Preferred category: $it" } ?: ""}
        
        Create a project with $MIN_PHASES_PER_PROJECT-$MAX_PHASES_PER_PROJECT phases that:
        1. Addresses identified weak areas through practical application
        2. Builds progressively from basic setup to advanced features
        3. Includes real-world technologies and best practices
        4. Provides clear learning objectives and deliverables
        5. Can be completed within the time constraints
        
        Return valid JSON with this structure:
        {
          "id": "project_id",
          "title": "Project Title",
          "description": "Detailed project description",
          "category": "WEB_DEVELOPMENT",
          "difficultyLevel": "INTERMEDIATE",
          "estimatedDurationDays": $preferredDuration,
          "totalTasks": 12,
          "phases": [
            {
              "id": "phase_id",
              "title": "Phase Title",
              "description": "Phase description",
              "order": 1,
              "estimatedDays": 3,
              "learningObjectives": ["objective1", "objective2"],
              "tasks": [
                {
                  "id": "task_id",
                  "title": "Task Title",
                  "description": "Detailed task description",
                  "type": "CODING",
                  "order": 1,
                  "estimatedMinutes": 120,
                  "skillsAddressed": ["JavaScript", "React"],
                  "deliverables": ["Component implementation", "Unit tests"],
                  "resources": [
                    {
                      "title": "Resource Title",
                      "type": "DOCUMENTATION",
                      "url": "https://example.com",
                      "description": "Resource description"
                    }
                  ],
                  "acceptanceCriteria": ["Criteria 1", "Criteria 2"],
                  "createdAt": "${Clock.System.now()}",
                  "generatedByLLM": true
                }
              ],
              "createdAt": "${Clock.System.now()}"
            }
          ],
          "technologies": ["React", "Node.js", "PostgreSQL"],
          "learningOutcomes": ["Outcome 1", "Outcome 2"],
          "realWorldApplications": ["Application 1", "Application 2"],
          "createdAt": "${Clock.System.now()}",
          "generatedByLLM": true,
          "adaptiveMetadata": {
            "generationPrompt": "project_prompt",
            "userWeakAreas": [],
            "targetSkills": ["skill1", "skill2"],
            "adaptationReason": "addresses_weak_areas"
          }
        }
        """.trimIndent()
    }

    private fun buildPhaseExpansionPrompt(
        project: RealWorldProject,
        completedPhaseId: String,
        weakAreas: List<WeakArea>
    ): String {
        return """
        Expand the project "${project.title}" with a new phase that builds upon the completed phase: $completedPhaseId
        
        Existing phases: ${project.phases.map { "${it.title} - ${it.description}" }.joinToString("\n")}
        Current weak areas: ${weakAreas.joinToString { it.skillArea }}
        Technologies used: ${project.technologies.joinToString()}
        
        Create a new phase that:
        1. Builds logically on previous phases
        2. Introduces advanced concepts or features
        3. Addresses remaining weak areas
        4. Maintains realistic scope and timeline
        
        Return JSON for a single ProjectPhase with same structure as above.
        """.trimIndent()
    }

    private suspend fun parseProjectFromLLMResponse(
        llmResponse: String,
        userId: String
    ): RealWorldProject {
        return try {
            val project = json.decodeFromString<RealWorldProject>(llmResponse)
            project.copy(
                id = generateUUID(),
                userId = userId,
                createdAt = Clock.System.now(),
                lastUpdated = Clock.System.now()
            )
        } catch (e: Exception) {
            logger.e("Failed to parse LLM project response", e)
            createFallbackProject(userId)
        }
    }

    private suspend fun parseProjectPhaseFromLLM(
        llmResponse: String,
        projectId: String
    ): ProjectPhase {
        return try {
            json.decodeFromString<ProjectPhase>(llmResponse).copy(
                id = generateUUID(),
                createdAt = Clock.System.now()
            )
        } catch (e: Exception) {
            logger.e("Failed to parse LLM phase response", e)
            createFallbackPhase(projectId)
        }
    }

    private fun createFallbackProject(userId: String): RealWorldProject {
        val taskId = generateUUID()
        val phaseId = generateUUID()

        val task = ProjectTask(
            id = taskId,
            title = "Setup Development Environment",
            description = "Configure your development environment and create initial project structure",
            type = ProjectTaskType.SETUP,
            order = 1,
            estimatedMinutes = 60,
            skillsAddressed = listOf("Development Setup", "Project Structure"),
            deliverables = listOf("Project repository", "Basic configuration"),
            resources = listOf(
                LearningResource(
                    title = "Project Setup Guide",
                    type = ResourceType.DOCUMENTATION,
                    url = "https://docs.example.com",
                    description = "Step-by-step setup guide"
                )
            ),
            acceptanceCriteria = listOf("Repository created", "Dependencies installed"),
            createdAt = Clock.System.now()
        )

        val phase = ProjectPhase(
            id = phaseId,
            title = "Project Setup",
            description = "Initial project configuration and environment setup",
            order = 1,
            estimatedDays = 1,
            learningObjectives = listOf("Project organization", "Development workflow"),
            tasks = listOf(task),
            createdAt = Clock.System.now()
        )

        return RealWorldProject(
            id = generateUUID(),
            userId = userId,
            title = "Basic Web Application",
            description = "Build a simple web application from scratch",
            category = ProjectCategory.WEB_DEVELOPMENT,
            difficultyLevel = DifficultyLevel.BEGINNER,
            estimatedDurationDays = 7,
            totalTasks = 1,
            phases = listOf(phase),
            technologies = listOf("HTML", "CSS", "JavaScript"),
            learningOutcomes = listOf("Web development basics", "Project structure"),
            realWorldApplications = listOf("Personal websites", "Small business sites"),
            createdAt = Clock.System.now(),
            lastUpdated = Clock.System.now()
        )
    }

    private fun createFallbackPhase(projectId: String): ProjectPhase {
        val taskId = generateUUID()

        val task = ProjectTask(
            id = taskId,
            title = "Add New Feature",
            description = "Implement an additional feature to enhance the project",
            type = ProjectTaskType.IMPLEMENTATION,
            order = 1,
            estimatedMinutes = 90,
            skillsAddressed = listOf("Feature Development"),
            deliverables = listOf("Working feature", "Documentation"),
            resources = emptyList(),
            acceptanceCriteria = listOf("Feature implemented", "Tests passing"),
            createdAt = Clock.System.now()
        )

        return ProjectPhase(
            id = generateUUID(),
            title = "Feature Enhancement",
            description = "Add advanced features to the project",
            order = 2,
            estimatedDays = 2,
            learningObjectives = listOf("Advanced implementation", "Feature design"),
            tasks = listOf(task),
            createdAt = Clock.System.now()
        )
    }

    private suspend fun getRelevantSkillsForProject(
        userId: String,
        weakAreas: List<WeakArea>,
        category: ProjectCategory?
    ): List<SkillNode> {
        val activeTree = skillTreeEngine.getActiveSkillTree(userId)
        return if (activeTree != null) {
            activeTree.allNodes.values.filter { node ->
                // Filter based on weak areas and category
                weakAreas.any { weakArea ->
                    node.title.contains(weakArea.skillArea, ignoreCase = true) ||
                    node.description.contains(weakArea.skillArea, ignoreCase = true)
                } || (category != null && node.category.name.contains(category.name.substringBefore("_"), ignoreCase = true))
            }.take(5)
        } else {
            emptyList()
        }
    }

    private suspend fun determineUserSkillLevel(userMetrics: UserPerformanceMetrics?): String {
        return when {
            userMetrics == null -> "BEGINNER"
            userMetrics.averageScore >= 0.8 -> "ADVANCED"
            userMetrics.averageScore >= 0.6 -> "INTERMEDIATE"
            else -> "BEGINNER"
        }
    }

    private fun calculateProgressAfterSubmission(
        project: RealWorldProject,
        currentProgress: ProjectProgress,
        completedTaskId: String,
        submissionQuality: Double
    ): ProjectProgress {
        val currentPhase = project.phases.find { it.id == currentProgress.currentPhaseId }
        val completedTasks = currentProgress.completedTasks + completedTaskId

        val phaseProgress = if (currentPhase != null) {
            completedTasks.size.toFloat() / currentPhase.tasks.size
        } else 0f

        val overallProgress = completedTasks.size.toFloat() / project.totalTasks

        // Check if phase is complete and move to next phase
        val nextPhaseId = if (phaseProgress >= 1.0f) {
            val nextPhase = project.phases.find { it.order == (currentPhase?.order ?: 0) + 1 }
            nextPhase?.id ?: currentProgress.currentPhaseId
        } else {
            currentProgress.currentPhaseId
        }

        return currentProgress.copy(
            completedTasks = completedTasks,
            currentPhaseId = nextPhaseId,
            overallProgress = overallProgress,
            lastActiveAt = Clock.System.now(),
            streak = if (submissionQuality >= 0.7) currentProgress.streak + 1 else 0
        )
    }

    private fun evaluateSubmissionQuality(submission: ProjectTaskSubmission): Double {
        // Simple quality evaluation - could be enhanced with LLM analysis
        return when {
            submission.submissionText.length < 50 -> 0.3
            submission.submissionText.contains("TODO", ignoreCase = true) -> 0.5
            submission.attachments.isNotEmpty() -> 0.8
            else -> 0.7
        }
    }

    private suspend fun updateSkillMasteryFromProject(
        userId: String,
        project: RealWorldProject,
        taskId: String,
        submission: ProjectTaskSubmission
    ) {
        val task = project.phases.flatMap { it.tasks }.find { it.id == taskId }
        if (task != null) {
            val activeTree = skillTreeEngine.getActiveSkillTree(userId)
            activeTree?.allNodes?.values?.forEach { skillNode ->
                if (task.skillsAddressed.any { skill ->
                    skillNode.title.contains(skill, ignoreCase = true) ||
                    skillNode.description.contains(skill, ignoreCase = true)
                }) {
                    val metrics = ChallengeCompletionMetrics(
                        challengeId = task.id,
                        userId = userId,
                        sessionId = generateUUID(),
                        totalTime = task.estimatedMinutes * 60000L,
                        attemptsCount = 1,
                        hintsUsed = 0,
                        errorsCount = 0,
                        completed = true,
                        finalScore = evaluateSubmissionQuality(submission)
                    )
                    skillTreeEngine.updateSkillMastery(userId, skillNode.id, metrics)
                }
            }
        }
    }

    private suspend fun saveProjectToFirebase(project: RealWorldProject) {
        try {
            // TODO: Persist project to Firebase when generic save API is added to FirebaseUserHelper
            val projectJson = json.encodeToString(project)
            logger.d("Skipping Firebase save for project ${'$'}{project.id}. JSON length=${'$'}{projectJson.length}")
        } catch (e: Exception) {
            logger.e("Failed to save project to Firebase", e)
        }
    }

    private suspend fun saveProjectProgressToFirebase(progress: ProjectProgress) {
        try {
            // TODO: Persist progress to Firebase when generic save API is added to FirebaseUserHelper
            val progressJson = json.encodeToString(progress)
            logger.d("Skipping Firebase save for project progress ${'$'}{progress.projectId}. JSON length=${'$'}{progressJson.length}")
        } catch (e: Exception) {
            logger.e("Failed to save project progress to Firebase", e)
        }
    }

    private suspend fun saveTaskSubmissionToFirebase(submission: ProjectTaskSubmission) {
        try {
            // TODO: Persist submission to Firebase when generic save API is added to FirebaseUserHelper
            val submissionJson = json.encodeToString(submission)
            logger.d("Skipping Firebase save for submission ${'$'}{submission.id}. JSON length=${'$'}{submissionJson.length}")
        } catch (e: Exception) {
            logger.e("Failed to save task submission to Firebase", e)
        }
    }

    suspend fun getProject(userId: String, projectId: String): RealWorldProject? {
        // TODO: Fetch from Firebase when generic get API is available in FirebaseUserHelper
        logger.w("getProject: Firebase generic get API not available; returning null for projectId=${'$'}projectId")
        return null
    }

    suspend fun getProjectProgress(userId: String, projectId: String): ProjectProgress? {
        // TODO: Fetch from Firebase when generic get API is available in FirebaseUserHelper
        logger.w("getProjectProgress: Firebase generic get API not available; returning null for projectId=${'$'}projectId")
        return null
    }

    suspend fun getUserProjects(userId: String): List<RealWorldProject> {
        // TODO: Fetch from Firebase when generic get API is available in FirebaseUserHelper
        logger.w("getUserProjects: Firebase generic get API not available; returning empty list for userId=${'$'}userId")
        return emptyList()
    }
}
