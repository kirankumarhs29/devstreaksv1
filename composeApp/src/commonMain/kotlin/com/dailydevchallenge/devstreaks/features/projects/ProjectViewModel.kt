package com.dailydevchallenge.devstreaks.features.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.service.ProjectBasedLearningEngine
import com.dailydevchallenge.devstreaks.service.SkillTreeEngine
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing real-world projects and user interactions
 */
class ProjectViewModel(
    private val projectEngine: ProjectBasedLearningEngine,
    private val skillTreeEngine: SkillTreeEngine
) : ViewModel() {

    private val logger = getLogger()

    private val userId = UserPreferences.getSafeUserId()

    // UI State
    private val _uiState = MutableStateFlow(ProjectUiState())
    val uiState: StateFlow<ProjectUiState> = _uiState.asStateFlow()

    // User's active projects
    private val _activeProjects = MutableStateFlow<List<RealWorldProject>>(emptyList())
    val activeProjects: StateFlow<List<RealWorldProject>> = _activeProjects.asStateFlow()

    // Project progress tracking
    private val _projectProgress = MutableStateFlow<Map<String, ProjectProgress>>(emptyMap())
    val projectProgress: StateFlow<Map<String, ProjectProgress>> = _projectProgress.asStateFlow()

    // Current project details
    private val _selectedProject = MutableStateFlow<RealWorldProject?>(null)
    val selectedProject: StateFlow<RealWorldProject?> = _selectedProject.asStateFlow()

    init {
        loadUserProjects()
    }

    /**
     * Generate a new personalized project
     */
    fun generateNewProject(
        interests: List<String> = emptyList(),
        timeAvailablePerDay: Int = 60,
        preferredDuration: Int = 14,
        category: ProjectCategory? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)

            projectEngine.generatePersonalizedProject(
                userId = userId,
                interests = interests,
                timeAvailablePerDay = timeAvailablePerDay,
                preferredDuration = preferredDuration,
                category = category
            ).fold(
                onSuccess = { newProject ->
                    val currentProjects = _activeProjects.value.toMutableList()
                    currentProjects.add(newProject)
                    _activeProjects.value = currentProjects

                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        successMessage = "New project '${newProject.title}' generated! Ready to start building."
                    )

                    // Load progress for the new project
                    loadProjectProgress(newProject.id)
                },
                onFailure = { error ->
                    logger.e("Failed to generate project", error)
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = "Failed to generate project: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Start or continue working on a project task
     */
    fun startTask(project: RealWorldProject, task: ProjectTask) {
        _uiState.value = _uiState.value.copy(
            selectedProject = project,
            currentTask = task,
            showTaskDialog = true
        )
    }

    /**
     * Complete a project task with submission
     */
    fun completeTask(
        projectId: String,
        taskId: String,
        timeSpent: Int,
        submission: String? = null
    ) {
        viewModelScope.launch {
            try {
                // Create a task submission object
                val taskSubmission = ProjectTaskSubmission(
                    id = com.dailydevchallenge.devstreaks.utils.generateUUID(),
                    userId = userId,
                    taskId = taskId,
                    projectId = projectId,
                    submissionText = submission ?: "Task completed",
                    submittedAt = kotlinx.datetime.Clock.System.now()
                )

                // Use the correct method from ProjectBasedLearningEngine
                val result = projectEngine.submitProjectTask(
                    userId = userId,
                    projectId = projectId,
                    taskId = taskId,
                    submission = taskSubmission
                )

                result.onSuccess { updatedProgress ->
                    // Update local progress
                    val currentProgress = _projectProgress.value.toMutableMap()
                    currentProgress[projectId] = updatedProgress
                    _projectProgress.value = currentProgress

                    // Update project completion status
                    updateProjectCompletionStatus(projectId)

                    _uiState.value = _uiState.value.copy(
                        showTaskDialog = false,
                        currentTask = null,
                        successMessage = "Task completed! Great progress on your project."
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to complete task: ${error.message}"
                    )
                }

            } catch (e: Exception) {
                logger.e("Failed to complete task", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to complete task: ${e.message}"
                )
            }
        }
    }

    /**
     * Expand a project phase with additional LLM-generated tasks
     */
    fun expandProjectPhase(
        projectId: String,
        phaseId: String,
        userFeedback: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExpanding = true)

            try {
                // Use the correct method from ProjectBasedLearningEngine
                val result = projectEngine.expandProjectWithNewPhase(
                    userId = userId,
                    projectId = projectId,
                    completedPhaseId = phaseId
                )

                result.onSuccess { newPhase ->
                    // Update the project with new phase
                    val currentProjects = _activeProjects.value.toMutableList()
                    val projectIndex = currentProjects.indexOfFirst { it.id == projectId }

                    if (projectIndex >= 0) {
                        val project = currentProjects[projectIndex]
                        val updatedPhases = project.phases + newPhase

                        currentProjects[projectIndex] = project.copy(
                            phases = updatedPhases,
                            totalTasks = project.totalTasks + newPhase.tasks.size
                        )
                        _activeProjects.value = currentProjects
                    }

                    _uiState.value = _uiState.value.copy(
                        isExpanding = false,
                        successMessage = "Added new phase '${newPhase.title}' to enhance your learning!"
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isExpanding = false,
                        error = "Failed to expand phase: ${error.message}"
                    )
                }

            } catch (e: Exception) {
                logger.e("Failed to expand project phase", e)
                _uiState.value = _uiState.value.copy(
                    isExpanding = false,
                    error = "Failed to expand phase: ${e.message}"
                )
            }
        }
    }

    /**
     * Submit project work for peer review
     */
    fun submitForReview(
        projectId: String,
        taskId: String,
        submissionUrl: String,
        description: String
    ) {
        viewModelScope.launch {
            try {
                // Create a proper task submission
                val submission = ProjectTaskSubmission(
                    id = com.dailydevchallenge.devstreaks.utils.generateUUID(),
                    userId = userId,
                    taskId = taskId,
                    projectId = projectId,
                    submissionText = description,
                    attachments = listOf(submissionUrl),
                    submittedAt = kotlinx.datetime.Clock.System.now()
                )

                // Submit the task for review
                val result = projectEngine.submitProjectTask(
                    userId = userId,
                    projectId = projectId,
                    taskId = taskId,
                    submission = submission
                )

                result.onSuccess {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Submitted for peer review! You'll receive feedback soon."
                    )
                }.onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to submit for review: ${error.message}"
                    )
                }

            } catch (e: Exception) {
                logger.e("Failed to submit for review", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to submit for review: ${e.message}"
                )
            }
        }
    }

    /**
     * Get suggested next projects based on completion
     */
    fun getSuggestedProjects(completedProjectId: String) {
        viewModelScope.launch {
            try {
                // Use the existing method to get user projects as suggestions
                val userProjects = projectEngine.getUserProjects(userId)
                val suggestions = userProjects.filter { it.id != completedProjectId && it.status != ProjectStatus.COMPLETED }

                _uiState.value = _uiState.value.copy(
                    suggestedProjects = suggestions.take(3) // Limit to 3 suggestions
                )
            } catch (e: Exception) {
                logger.e("Failed to get suggested projects", e)
            }
        }
    }

    /**
     * Load user's projects and progress
     */
    private fun loadUserProjects() {
        viewModelScope.launch {
            try {
                // Get user's projects
                val projects = projectEngine.getUserProjects(userId)

                // Update active projects
                _activeProjects.value = projects.filter { it.status == ProjectStatus.IN_PROGRESS }

                // Load progress for each active project
                _activeProjects.value.forEach { project ->
                    loadProjectProgress(project.id)
                }
            } catch (e: Exception) {
                logger.e("Failed to load user projects", e)
            }
        }
    }

    /**
     * Load progress for a specific project
     */
    private fun loadProjectProgress(projectId: String) {
        viewModelScope.launch {
            try {
                val progress = projectEngine.getProjectProgress(userId, projectId)
                if (progress != null) {
                    _projectProgress.update { currentMap ->
                        currentMap + (projectId to progress)
                    }
                }
            } catch (e: Exception) {
                logger.e("Failed to load project progress", e)
            }
        }
    }

    /**
     * Update project completion status in the active projects list
     */
    private fun updateProjectCompletionStatus(projectId: String) {
        viewModelScope.launch {
            val currentProjects = _activeProjects.value.toMutableList()
            val projectIndex = currentProjects.indexOfFirst { it.id == projectId }

            if (projectIndex != -1) {
                val project = currentProjects[projectIndex]
                val updatedProject = project.copy(status = ProjectStatus.COMPLETED)

                currentProjects[projectIndex] = updatedProject
                _activeProjects.value = currentProjects
            }
        }
    }

    /**
     * Get project progress for a specific project - Method needed by ProjectActivityCard
     */
    fun getProjectProgress(projectId: String): ProjectProgress? {
        return _projectProgress.value[projectId]
    }

    /**
     * Submit a task with submission data - Method needed by ProjectActivityCard
     */
    fun submitTask(
        projectId: String,
        taskId: String,
        submission: ProjectTaskSubmission
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingTask = true) }

            try {
                val result = projectEngine.submitProjectTask(
                    userId = userId,
                    projectId = projectId,
                    taskId = taskId,
                    submission = submission
                )

                result.onSuccess { updatedProgress ->
                    _projectProgress.update { currentMap ->
                        currentMap + (projectId to updatedProgress)
                    }
                    _uiState.update {
                        it.copy(
                            isSubmittingTask = false,
                            successMessage = "Task submitted successfully"
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isSubmittingTask = false,
                            error = "Failed to submit task: ${error.message}"
                        )
                    }
                }

            } catch (e: Exception) {
                logger.e("Failed to submit task", e)
                _uiState.update {
                    it.copy(
                        isSubmittingTask = false,
                        error = "Failed to submit task: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Expand project with new phase - Method needed by ProjectActivityCard
     */
    fun expandProjectWithNewPhase(
        projectId: String,
        completedPhaseId: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPhase = true) }

            try {
                val result = projectEngine.expandProjectWithNewPhase(
                    userId = userId,
                    projectId = projectId,
                    completedPhaseId = completedPhaseId
                )

                result.onSuccess { newPhase ->
                    // Update the project with new phase
                    val currentProjects = _activeProjects.value.toMutableList()
                    val projectIndex = currentProjects.indexOfFirst { it.id == projectId }

                    if (projectIndex >= 0) {
                        val project = currentProjects[projectIndex]
                        val updatedPhases = project.phases + newPhase

                        currentProjects[projectIndex] = project.copy(
                            phases = updatedPhases,
                            totalTasks = project.totalTasks + newPhase.tasks.size
                        )
                        _activeProjects.value = currentProjects
                    }

                    _uiState.update {
                        it.copy(
                            isGeneratingPhase = false,
                            successMessage = "New project phase generated: ${newPhase.title}"
                        )
                    }

                    // Reload project progress
                    loadProject(projectId)
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isGeneratingPhase = false,
                            error = "Failed to generate new phase: ${error.message}"
                        )
                    }
                }

            } catch (e: Exception) {
                logger.e("Failed to expand project", e)
                _uiState.update {
                    it.copy(
                        isGeneratingPhase = false,
                        error = "Failed to generate new phase: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Load a specific project and its progress
     */
    fun loadProject(projectId: String) {
        viewModelScope.launch {
            try {
                val project = projectEngine.getProject(userId, projectId)
                val progress = projectEngine.getProjectProgress(userId, projectId)

                if (project != null) {
                    _selectedProject.value = project
                    if (progress != null) {
                        _projectProgress.update { currentMap ->
                            currentMap + (projectId to progress)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.e("Failed to load project", e)
            }
        }
    }
}
