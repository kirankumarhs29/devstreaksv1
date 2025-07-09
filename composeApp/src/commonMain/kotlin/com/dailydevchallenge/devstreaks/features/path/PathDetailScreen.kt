package com.dailydevchallenge.devstreaks.features.path

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.navigation.DevStreakTopBar
import com.dailydevchallenge.devstreaks.model.ChallengePath
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import org.koin.compose.koinInject
import com.dailydevchallenge.devstreaks.data.ChallengeDetail

@Composable
fun PathDetailScreen(navController: NavController, pathId: String) {
    val repository: ChallengeRepository = koinInject()

    var path by remember { mutableStateOf<ChallengePath?>(null) }
    var challenges by remember { mutableStateOf<List<ChallengeTask>>(emptyList()) }
    var completedMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(pathId) {
        path = repository.getPathById(pathId)
        challenges = repository.getTasksForPath(pathId)

        val progressList = repository.getAllUserProgress()
        completedMap = progressList
            .mapNotNull { it.completedTaskId?.let { id -> it.completedDate?.let { date -> id to date } } }
            .toMap()
    }

    path?.let { safePath ->
        Scaffold(
            topBar = { DevStreakTopBar(safePath.track, onBack = { navController.popBackStack() }) },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Text(
                        text = safePath.track,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "📘 Challenges in this path",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (challenges.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No challenges found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }

                } else {
                    items(challenges) { challenge ->
                        val completedDate = completedMap[challenge.id]

                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { navController.navigate(ChallengeDetail(challenge.id)) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = challenge.title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (completedDate != null) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Completed",
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    } else {
                                        Text(
                                            text = "In Progress",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = MaterialTheme.colorScheme.secondary
                                            ),
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                }

                                completedDate?.let {
                                    Text(
                                        text = "📅 Completed on: $it",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.outline
                                        ),
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                challenge.tip?.let { tip ->
                                    Text(
                                        text = tip,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "${challenge.xp} XP",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    } ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Path not found", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
