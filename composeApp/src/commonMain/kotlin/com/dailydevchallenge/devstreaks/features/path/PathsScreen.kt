package com.dailydevchallenge.devstreaks.features.path

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.routes.PathDetail
import com.dailydevchallenge.devstreaks.features.navigation.DevStreakTopBar
import com.dailydevchallenge.devstreaks.model.ChallengePathWithTasks
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import org.koin.compose.koinInject

@Composable
fun PathsScreen(navController: NavController) {
    val repo: ChallengeRepository = koinInject()
    var paths by remember { mutableStateOf<List<ChallengePathWithTasks>>(emptyList()) }
    val allCompletedTasks = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        val enrichedPaths = repo.getAllPathsWithTasks()
        val completed = repo.getAllCompletedTaskIds().filterNotNull()
        paths = enrichedPaths
        allCompletedTasks.addAll(completed)
    }

    Scaffold(
        topBar = { DevStreakTopBar(title = "🚀 Learning Paths") },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 💡 Daily Motivation Block
            item {
                Text(
                    text = "\"Stay consistent, not perfect. Small steps compound.\"",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // 📚 Featured Path if only one
            if (paths.size == 1) {
                val path = paths[0]
                val totalTasks = path.tasks.size
                val completedTasks = path.tasks.count { it.id in allCompletedTasks }
                val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
                val buttonText = when {
                    completedTasks == totalTasks -> "Review"
                    completedTasks > 0 -> "Continue"
                    else -> "Start"
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(PathDetail(path.id)) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                "🔥 Featured Path",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = path.track,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(Modifier.height(12.dp))

                            // 🚀 Concept tags (you can replace with actual tags)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Arrays", "Stacks", "Recursion").forEach { tag ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(tag) }
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⭐ XP: ${path.tasks.sumOf { it.xp }} | ✅ $completedTasks/$totalTasks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )

                                Button(onClick = {
                                    navController.navigate(PathDetail(path.id))
                                }) {
                                    Text(buttonText)
                                }
                            }
                        }
                    }
                }
            } else {
                // 🗂 Multiple paths
                items(paths) { path ->
                    val totalTasks = path.tasks.size
                    val completedTasks = path.tasks.count { it.id in allCompletedTasks }
                    val totalXp = path.tasks.sumOf { it.xp }
                    val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
                    val buttonText = when {
                        completedTasks == totalTasks -> "Review"
                        completedTasks > 0 -> "Continue"
                        else -> "Start"
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(PathDetail(path.id)) },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = path.track,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "Explore topics in this path.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(12.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⭐ XP: $totalXp | ✅ $completedTasks/$totalTasks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )

                                Button(
                                    onClick = { navController.navigate(PathDetail(path.id)) },
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(buttonText)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

