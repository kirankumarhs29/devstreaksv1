package com.dailydevchallenge.devstreaks.features.projects

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.*
import org.koin.compose.koinInject

/**
 * Comprehensive project-based learning screen with LLM-generated real-world projects
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onNavigateToSkillTree: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProjectViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeProjects by viewModel.activeProjects.collectAsState()
    val projectProgress by viewModel.projectProgress.collectAsState()

    var showCreateProjectDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        item {
            ProjectsHeader(
                onCreateNewProject = { showCreateProjectDialog = true },
                onNavigateToSkillTree = onNavigateToSkillTree
            )
        }

        // Active Projects Section
        items(activeProjects) { project ->
            ActiveProjectCard(
                project = project,
                progress = projectProgress[project.id],
                onContinueProject = { /* TODO: Implement continue logic if needed */ },
                onViewProjectDetails = { /* TODO: Implement view details logic if needed */ }
            )
        }

        // Project Categories
        item {
            ProjectCategoriesSection(
                onCategorySelected = { category ->
                    /* TODO: Implement category selection logic if needed */
                }
            )
        }

        // User Projects List
        // Use activeProjects instead of userProjects, and ActiveProjectCard for all
        if (activeProjects.isNotEmpty()) {
            item {
                Text(
                    text = "My Projects",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(activeProjects) { project ->
                ActiveProjectCard(
                    project = project,
                    progress = projectProgress[project.id],
                    onContinueProject = { /* TODO: Implement continue logic if needed */ },
                    onViewProjectDetails = { /* TODO: Implement view details logic if needed */ }
                )
            }
        }

        // Project Recommendations
        if (activeProjects.isEmpty()) {
            item {
                ProjectRecommendationsCard(
                    onGenerateProject = { interests, timeAvailable, difficulty ->
                        viewModel.generateNewProject(
                            interests = interests,
                            timeAvailablePerDay = timeAvailable,
                            preferredDuration = 14,
                            category = null
                        )
                    }
                )
            }
        }
    }

    // Loading Overlay
    if (uiState.isGenerating) { // Use isGenerating from uiState
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Generating personalized project...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    // Error Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // TODO: Show snackbar with error
            // viewModel.clearError() // Remove or implement if needed
        }
    }

    // Success Snackbar
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            // TODO: Show snackbar with success message
            // viewModel.clearSuccessMessage() // Remove or implement if needed
        }
    }

    // Create Project Dialog
    if (showCreateProjectDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateProjectDialog = false },
            onCreateProject = { interests, timeAvailable, duration, category ->
                viewModel.generateNewProject(
                    interests = interests,
                    timeAvailablePerDay = timeAvailable,
                    preferredDuration = duration,
                    category = category
                )
                showCreateProjectDialog = false
            }
        )
    }
}

@Composable
private fun ProjectsHeader(
    onCreateNewProject: () -> Unit,
    onNavigateToSkillTree: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
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
                Column {
                    Text(
                        text = "Real-World Projects",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Build practical skills through guided projects",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.Work,
                    contentDescription = "Projects",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCreateNewProject,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Project")
                }

                OutlinedButton(
                    onClick = onNavigateToSkillTree,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AccountTree, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Skill Tree")
                }
            }
        }
    }
}

@Composable
private fun ActiveProjectCard(
    project: RealWorldProject,
    progress: ProjectProgress?,
    onContinueProject: () -> Unit,
    onViewProjectDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        onClick = onViewProjectDetails
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
                    text = "🔥 Active Project",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = project.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Text(
                text = project.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Progress indicator
            progress?.let { prog ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Progress",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${(prog.overallProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { prog.overallProgress }, // Use lambda overload
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Technologies used
            if (project.technologies.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(project.technologies.take(4)) { tech ->
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

            Button(
                onClick = onContinueProject,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Continue Project")
            }
        }
    }
}

@Composable
private fun ProjectCategoriesSection(
    onCategorySelected: (ProjectCategory) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Project Categories",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ProjectCategory.entries) { category -> // Use entries for Kotlin 1.9+
                ProjectCategoryCard(
                    category = category,
                    onCategorySelected = onCategorySelected
                )
            }
        }
    }
}

@Composable
private fun ProjectCategoryCard(
    category: ProjectCategory,
    onCategorySelected: (ProjectCategory) -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onCategorySelected(category) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val (icon, color) = getCategoryIconAndColor(category)

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = color.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = category.name.replace("_", " "),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun ProjectRecommendationsCard(
    onGenerateProject: (List<String>, Int, DifficultyLevel?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "No Projects Yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Let AI generate personalized projects based on your skill level and interests. Each project is tailored to address your weak areas and build real-world experience.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    // Generate with default parameters
                    onGenerateProject(emptyList(), 60, null)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate AI Project")
            }
        }
    }
}

private fun getCategoryIconAndColor(category: ProjectCategory): Pair<androidx.compose.ui.graphics.vector.ImageVector, androidx.compose.ui.graphics.Color> {
    return when (category) {
        ProjectCategory.WEB_DEVELOPMENT -> Icons.Default.Web to androidx.compose.ui.graphics.Color(0xFF2196F3)
        ProjectCategory.MOBILE_DEVELOPMENT -> Icons.Default.PhoneAndroid to androidx.compose.ui.graphics.Color(0xFF4CAF50)
        ProjectCategory.DATA_SCIENCE -> Icons.Default.Analytics to androidx.compose.ui.graphics.Color(0xFF9C27B0)
        ProjectCategory.BACKEND_DEVELOPMENT -> Icons.Default.Storage to androidx.compose.ui.graphics.Color(0xFFFF9800)
        ProjectCategory.GAME_DEVELOPMENT -> Icons.Default.SportsEsports to androidx.compose.ui.graphics.Color(0xFFF44336)
        ProjectCategory.MACHINE_LEARNING -> Icons.Default.Psychology to androidx.compose.ui.graphics.Color(0xFF00BCD4)
        ProjectCategory.DEVOPS -> Icons.Default.CloudQueue to androidx.compose.ui.graphics.Color(0xFF795548)
        ProjectCategory.FULL_STACK -> Icons.Default.Layers to androidx.compose.ui.graphics.Color(0xFF607D8B)
        else -> Icons.Default.Help to androidx.compose.ui.graphics.Color(0xFF9E9E9E) // Exhaustive
    }
}

// Dialog component placeholder
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (List<String>, Int, Int, ProjectCategory?) -> Unit
) {
    // TODO: Implement create project dialog with form fields
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        content = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Create New Project", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Project creation dialog will be implemented here")
            }
        }
    )
}
