package com.dailydevchallenge.devstreaks.features.skilltree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailydevchallenge.devstreaks.model.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Main Skill Tree visualization component with interactive nodes - Connected to Phase 3 services
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillTreeView(
    modifier: Modifier = Modifier,
    viewModel: SkillTreeViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val skillTree by viewModel.skillTree.collectAsState()
    val skillMasteries by viewModel.skillMasteries.collectAsState()
    var selectedNode by remember { mutableStateOf<SkillNode?>(null) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadUserSkillTree()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with progress overview and AI generation
        SkillTreeHeader(
            skillTree = skillTree,
            totalMasteries = skillMasteries,
            onGenerateSkillTree = { showGenerateDialog = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main skill tree content
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = uiState.error ?: "Unknown error", color = MaterialTheme.colorScheme.error)
            }
        } else if (skillTree != null) {
            SkillTreeContent(
                skillTree = skillTree!!,
                skillMasteries = skillMasteries,
                onNodeClick = { node -> selectedNode = node }
            )
        } else {
            SkillTreeEmptyState(
                isLoading = uiState.isLoading,
                onGenerateSkillTree = {
                    scope.launch {
                        viewModel.generateNewSkillTree()
                    }
                }
            )
        }

        // Selected node details panel
        selectedNode?.let { node ->
            SkillNodeDetailsPanel(
                node = node,
                mastery = skillMasteries[node.id],
                onMarkPracticed = {
                    scope.launch {
                        // Update mastery progress (simulate + persist)
                        val current = skillMasteries[node.id]?.progress ?: 0f
                        val updated = current.coerceAtMost(1f) + 0.1f
                        val newMastery = SkillMastery(
                            node.id, updated.coerceAtMost(1f).toString(),
                            skillNodeId = node.id,
                            masteryLevel = when {
                                updated >= 1f -> MasteryLevel.EXPERT
                                updated >= 0.75f -> MasteryLevel.ADVANCED
                                updated >= 0.5f -> MasteryLevel.INTERMEDIATE
                                updated >= 0.25f -> MasteryLevel.BEGINNER
                                else -> MasteryLevel.NOVICE
                            },
                            progress = updated,
                            xpEarned = (updated * 100).toInt(), // Example XP calculation
                            completedChallenges = 0, // Update as needed
                            timeSpentMinutes = 0, // Update as needed
                            firstAttemptAt = node.createdAt,
                            lastActivityAt = node.createdAt,
                            practiceStreak = (skillMasteries[node.id]?.practiceStreak ?: 0) + 1
                        )
                        val newMap = skillMasteries.toMutableMap().apply { put(node.id, newMastery) }
                        // Simulate persistence (replace with repo/db call if available)
                        // viewModel.updateSkillMastery(node.id, updated) // If you have such a function
                        selectedNode = null
                    }
                },
                onDismiss = { selectedNode = null }
            )
        }
    }

    // Generate dialog
    if (showGenerateDialog) {
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        viewModel.generateNewSkillTree()
                        showGenerateDialog = false
                    }
                }) { Text("Generate Skill Tree") }
            },
            title = { Text("Generate New Skill Tree") },
            text = { Text("Let AI generate a personalized skill tree based on your learning goals.") }
        )
    }
}

@Composable
fun SkillTreeContent(
    skillTree: SkillTree,
    skillMasteries: Map<String, SkillMastery>,
    onNodeClick: (SkillNode) -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(skillTree.allNodes.values.toList()) { node ->
            val mastery = skillMasteries[node.id]?.progress ?: 0f
            SkillNodeItem(
                node = node,
                mastery = mastery,
                onClick = { onNodeClick(node) }
            )
        }
    }
}

@Composable
fun SkillNodeItem(
    node: SkillNode,
    mastery: Float,
    onClick: () -> Unit
) {
    val unlocked = !node.isLocked
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (unlocked) MaterialTheme.colorScheme.surface else Color.LightGray)
            .clickable(enabled = unlocked) { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = node.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (unlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
            )
            Text(
                text = node.description,
                style = MaterialTheme.typography.bodyMedium,
                color = if (unlocked) MaterialTheme.colorScheme.onSurface else Color.Gray
            )
            LinearProgressIndicator(
                progress = { mastery },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = if (unlocked) MaterialTheme.colorScheme.primary else Color.Gray
            )
            Text(
                text = if (unlocked) "${(mastery * 100).toInt()}% mastered" else "Locked",
                style = MaterialTheme.typography.labelSmall,
                color = if (unlocked) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun SkillNodeDetailsPanel(
    node: SkillNode,
    mastery: SkillMastery?,
    onMarkPracticed: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onMarkPracticed, enabled = node.isLocked) {
                Text("Mark as Practiced")
            }
        },
        title = { Text(node.title) },
        text = {
            Column {
                Text(node.description)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { mastery?.progress ?: 0f })
                Text("Mastery: ${(mastery?.progress ?: 0f) * 100}%")
            }
        }
    )
}

@Composable
fun SkillTreeHeader(
    skillTree: SkillTree?,
    totalMasteries: Map<String, SkillMastery>,
    onGenerateSkillTree: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Skill Tree",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(onClick = onGenerateSkillTree) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Generate AI Tree")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nodes: ${skillTree?.allNodes?.size ?: 0} | Mastered: ${totalMasteries
                    .values.count { it.progress >= 1f }}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SkillTreeEmptyState(
    isLoading: Boolean,
    onGenerateSkillTree: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("No Skill Tree Found", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Generate a personalized skill tree to start your journey.", textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onGenerateSkillTree) {
                    Text("Generate Skill Tree")
                }
            }
        }
    }
}

private fun DrawScope.drawSkillConnections(nodes: List<SkillNode>, skillTree: SkillTree) {
    val nodePositions = mutableMapOf<String, Offset>()

    // Calculate node positions (simplified grid layout)
    nodes.forEachIndexed { index, node ->
        val x = (index % 3) * (size.width / 3) + (size.width / 6)
        val y = (index / 3) * (size.height / 2) + (size.height / 4)
        nodePositions[node.id] = Offset(x, y)
    }

    // Draw connections between prerequisite nodes
    nodes.forEach { node ->
        node.prerequisites.forEach { prereqId ->
            val startPos = nodePositions[prereqId]
            val endPos = nodePositions[node.id]

            if (startPos != null && endPos != null) {
                drawLine(
                    color = Color.Gray,
                    start = startPos,
                    end = endPos,
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
                )
            }
        }
    }
}

private fun getMasteryColor(level: MasteryLevel): Color {
    return when (level) {
        MasteryLevel.NOVICE -> Color.Gray
        MasteryLevel.BEGINNER -> Color.Blue
        MasteryLevel.INTERMEDIATE -> Color.Green
        MasteryLevel.ADVANCED -> Color(0xFFFFB347) // Orange
        MasteryLevel.EXPERT -> Color.Red
    }
}

private fun getMasteryIcon(level: MasteryLevel) = when (level) {
    MasteryLevel.NOVICE -> Icons.Default.Circle
    MasteryLevel.BEGINNER -> Icons.Default.Star
    MasteryLevel.INTERMEDIATE -> Icons.Default.StarHalf
    MasteryLevel.ADVANCED -> Icons.Default.Stars
    MasteryLevel.EXPERT -> Icons.Default.Diamond
}
