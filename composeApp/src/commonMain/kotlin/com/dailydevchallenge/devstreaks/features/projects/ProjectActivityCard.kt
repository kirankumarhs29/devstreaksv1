package com.dailydevchallenge.devstreaks.features.projects

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.*
import org.koin.compose.koinInject

/**
 * Main project activity card showing project overview and progress - Connected to Phase 3 services
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectActivityCard(
    project: RealWorldProject,
    progress: ProjectProgress? = null,
    onProjectClick: () -> Unit,
    onContinueTask: (ProjectTask) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = koinInject() // Connect to Phase 3 ViewModel
) {
    var expanded by remember { mutableStateOf(false) }

    // Collect additional state from ViewModel for real-time updates
//    val uiState by viewModel.uiState.collectAsState()
    val currentProgress = progress ?: viewModel.getProjectProgress(project.id)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProjectClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Project header with AI generation indicator
            ProjectHeader(
                project = project,
                progress = currentProgress,
                onExpand = { expanded = !expanded },
                expanded = expanded
            )

            // Progress indicators
            ProjectProgressSection(
                project = project,
                progress = currentProgress
            )

            // Current phase and active tasks
            currentProgress?.let { prog ->
                val currentPhase = project.phases.find { it.id == prog.currentPhaseId }
                currentPhase?.let { phase ->
                    CurrentPhaseSection(
                        phase = phase,
                        progress = prog,
                        onContinueTask = onContinueTask,
                        onSubmitTask = { taskId, submission ->
                            viewModel.submitTask(project.id, taskId, submission)
                        }
                    )
                }
            }

            // Expandable project details
            if (expanded) {
                ProjectDetailsSection(
                    project = project,
                    onGenerateNewPhase = {
                        currentProgress?.currentPhaseId?.let { phaseId ->
                            viewModel.expandProjectWithNewPhase(project.id, phaseId)
                        }
                    }
                )
            }

            // Action buttons
            ProjectActions(
                project = project,
                progress = currentProgress,
                onContinue = {
                    currentProgress?.let { prog ->
                        val currentPhase = project.phases.find { it.id == prog.currentPhaseId }
                        val nextTask = currentPhase?.tasks?.find { task ->
                            task.id !in prog.completedTasks
                        }
                        nextTask?.let(onContinueTask)
                    }
                },
                expanded = expanded,
                onToggleExpanded = { expanded = !expanded }
            )
        }
    }
}

@Composable
private fun ProjectHeader(
    project: RealWorldProject,
    progress: ProjectProgress?,
    onExpand: () -> Unit,
    expanded: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                    overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
                )

                // AI generation indicator
                if (project.generatedByLLM) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI Generated",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                "AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Text(
                text = project.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = if (expanded) TextOverflow.Visible else TextOverflow.Ellipsis
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Difficulty badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = getDifficultyColor(project.difficultyLevel).copy(alpha = 0.1f)
            ) {
                Text(
                    text = project.difficultyLevel.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = getDifficultyColor(project.difficultyLevel),
                    fontWeight = FontWeight.Medium
                )
            }

            // Expand/collapse button
            IconButton(onClick = onExpand) {
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
        }
    }
}

@Composable
private fun ProjectProgressSection(
    project: RealWorldProject,
    progress: ProjectProgress?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Progress",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )

            progress?.let { prog ->
                Text(
                    text = "${(prog.overallProgress * 100).toInt()}% Complete",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Progress bar
        LinearProgressIndicator(
            progress = progress?.overallProgress ?: 0f,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Project stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ProjectStat(
                icon = Icons.Default.Assignment,
                label = "Tasks",
                value = "${progress?.completedTasks?.size ?: 0}/${project.totalTasks}"
            )
            ProjectStat(
                icon = Icons.Default.Schedule,
                label = "Duration",
                value = "${project.estimatedDurationDays}d"
            )
            ProjectStat(
                icon = Icons.Default.TrendingUp,
                label = "Streak",
                value = "${progress?.streak ?: 0}"
            )
        }
    }
}

@Composable
private fun CurrentPhaseSection(
    phase: ProjectPhase,
    progress: ProjectProgress,
    onContinueTask: (ProjectTask) -> Unit,
    onSubmitTask: (String, ProjectTaskSubmission) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Current Phase: ${phase.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = phase.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )

            // Active tasks
            val activeTasks = phase.tasks.filter { it.id !in progress.completedTasks }
            if (activeTasks.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(activeTasks.take(2)) { task ->
                        TaskItem(
                            task = task,
                            onContinue = { onContinueTask(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectDetailsSection(
    project: RealWorldProject,
    onGenerateNewPhase: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Technologies
        if (project.technologies.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Technologies",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(project.technologies) { tech ->
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
        }

        // Learning outcomes
        if (project.learningOutcomes.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Learning Outcomes",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
                project.learningOutcomes.take(3).forEach { outcome ->
                    Text(
                        text = "• $outcome",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Generate new phase button
        OutlinedButton(
            onClick = onGenerateNewPhase,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate New Phase")
        }
    }
}

@Composable
private fun ProjectActions(
    project: RealWorldProject,
    progress: ProjectProgress?,
    onContinue: () -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onContinue,
            modifier = Modifier.weight(1f),
            enabled = progress != null && progress.overallProgress < 1.0f
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (progress?.overallProgress ?: 0f > 0f) "Continue" else "Start Project"
            )
        }
    }
}

@Composable
private fun ProjectStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskItem(
    task: ProjectTask,
    onContinue: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onContinue() }
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${task.estimatedMinutes}min • ${task.type.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "Continue task",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getDifficultyColor(difficulty: DifficultyLevel): Color {
    return when (difficulty) {
        DifficultyLevel.BEGINNER -> Color(0xFF4CAF50)
        DifficultyLevel.MEDIUM -> Color(0xFFFF9800)
        DifficultyLevel.HARD -> Color(0xFFF44336)
        DifficultyLevel.EXPERT -> Color(0xFF9C27B0)
        else -> Color(0xFF9E9E9E) // Default gray for unknown levels
    }
}
