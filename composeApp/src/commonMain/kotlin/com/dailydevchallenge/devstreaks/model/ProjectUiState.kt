package com.dailydevchallenge.devstreaks.model

data class ProjectUiState(
    val isGenerating: Boolean = false,
    val isExpanding: Boolean = false,
    val isSubmittingTask: Boolean = false,
    val isGeneratingPhase: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null,
    val selectedProject: RealWorldProject? = null,
    val currentTask: ProjectTask? = null,
    val showTaskDialog: Boolean = false,
    val suggestedProjects: List<RealWorldProject> = emptyList()
)
