package com.dailydevchallenge.devstreaks.features.projects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.*
import org.koin.compose.koinInject

/**
 * Detailed project screen showing phases, tasks, and progress
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val project by viewModel.selectedProject.collectAsState()
    val projectProgress by viewModel.projectProgress.collectAsState()

    LaunchedEffect(projectId) {
        viewModel.loadProject(projectId)
    }

    project?.let { proj ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with back navigation
            item {
                ProjectDetailHeader(
                    project = proj,
                    progress = projectProgress[projectId],
                    onNavigateBack = onNavigateBack
                )
            }

            // Project overview
            item {
                ProjectOverviewCard(
                    project = proj,
                    progress = projectProgress[projectId]
                )
            }

            // Project phases
            items(proj.phases) { phase ->
                ProjectPhaseCard(
                    phase = phase,
                    progress = projectProgress[projectId],
                    onStartTask = { task ->
                        // TODO: Navigate to task detail
                    },
                    onSubmitTask = { taskId, submission ->
                        viewModel.submitTask(projectId, taskId, submission)
                    }
                )
            }
        }
    } ?: run {
        // Loading or error state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isGenerating || uiState.isExpanding) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Loading project...")
                }
            } else {
                Text("Project not found")
            }
        }
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // TODO: Show error snack bar
        }
    }
}

@Composable
private fun ProjectDetailHeader(
    project: RealWorldProject,
    progress: ProjectProgress?,
    onNavigateBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                if (project.generatedByLLM) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI Generated",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "AI Generated",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = project.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = project.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            progress?.let { prog ->
                LinearProgressIndicator(
                    progress = prog.overallProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${(prog.overallProgress * 100).toInt()}% Complete",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ProjectOverviewCard(
    project: RealWorldProject,
    progress: ProjectProgress?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Project Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Technologies
            if (project.technologies.isNotEmpty()) {
                Text(
                    text = "Technologies",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    project.technologies.forEach { tech ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = tech,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Learning outcomes
            if (project.learningOutcomes.isNotEmpty()) {
                Text(
                    text = "Learning Outcomes",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                project.learningOutcomes.forEach { outcome ->
                    Text(
                        text = "• $outcome",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Real-world applications
            if (project.realWorldApplications.isNotEmpty()) {
                Text(
                    text = "Real-World Applications",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                project.realWorldApplications.forEach { application ->
                    Text(
                        text = "• $application",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectPhaseCard(
    phase: ProjectPhase,
    progress: ProjectProgress?,
    onStartTask: (ProjectTask) -> Unit,
    onSubmitTask: (String, ProjectTaskSubmission) -> Unit
) {
    val completedTasks = progress?.completedTasks ?: emptySet()
    val isCurrentPhase = progress?.currentPhaseId == phase.id

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentPhase) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Phase ${phase.order}: ${phase.title}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (isCurrentPhase) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Current",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            Text(
                text = phase.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Learning objectives
            if (phase.learningObjectives.isNotEmpty()) {
                Text(
                    text = "Learning Objectives:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                phase.learningObjectives.forEach { objective ->
                    Text(
                        text = "• $objective",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tasks
            phase.tasks.forEach { task ->
                ProjectTaskItem(
                    task = task,
                    isCompleted = task.id in completedTasks,
                    isActive = isCurrentPhase,
                    onStartTask = { onStartTask(task) },
                    onSubmitTask = { submission ->
                        onSubmitTask(task.id, submission)
                    }
                )
            }
        }
    }
}

@Composable
private fun ProjectTaskItem(
    task: ProjectTask,
    isCompleted: Boolean,
    isActive: Boolean,
    onStartTask: () -> Unit,
    onSubmitTask: (ProjectTaskSubmission) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Circle,
                        contentDescription = if (isCompleted) "Completed" else "Not completed",
                        tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${task.estimatedMinutes}min • ${task.type.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isCompleted && isActive) {
                Button(
                    onClick = onStartTask
                ) {
                    Text("Start")
                }
            }
        }
    }
}
