

package com.dailydevchallenge.devstreaks.features.social

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailydevchallenge.devstreaks.model.*
import org.koin.compose.koinInject
import com.dailydevchallenge.devstreaks.features.social.CollaborativeChallengeCard
import com.dailydevchallenge.devstreaks.features.social.PeerReviewCard
import com.dailydevchallenge.devstreaks.features.social.SocialViewModel
import com.dailydevchallenge.devstreaks.features.social.StudyGroupCard

/**
 * Main social dashboard screen with peer reviews, collaborative challenges, and community features
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialDashboardScreen(
    onNavigateToSkillTree: () -> Unit,
    onNavigateToProjects: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SocialViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val socialProfile by viewModel.socialProfile.collectAsState()
    val leaderboards by viewModel.leaderboards.collectAsState()
    val codeReviews by viewModel.codeReviews.collectAsState()
    val collaborativeChallenges by viewModel.collaborativeChallenges.collectAsState()
    val currentCommunityChallenge by viewModel.currentCommunityChallenge.collectAsState()
    val userStudyGroups by viewModel.userStudyGroups.collectAsState()

    var showCreateChallengeDialog by remember { mutableStateOf(false) }
    var showCreateStudyGroupDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Social Profile Header
        item {
            SocialProfileCard(
                socialProfile = socialProfile,
                onNavigateToSkillTree = onNavigateToSkillTree,
                onNavigateToProjects = onNavigateToProjects
            )
        }

        // Current Community Challenge
        item {
            currentCommunityChallenge?.let { challenge ->
                CommunityChallengeCard(
                    challenge = challenge,
                    onJoinChallenge = { /* TODO: Implement join */ },
                    onLeaveChallenge = { /* TODO: Implement leave */ }
                )
            }
        }

        // Quick Actions
        item {
            QuickActionsCard(
                onCreateCollaborativeChallenge = { showCreateChallengeDialog = true },
                onCreateStudyGroup = { showCreateStudyGroupDialog = true },
                onGeneratePeerFeedback = {
                    // TODO: Navigate to code submission screen
                }
            )
        }

        // Leaderboards Section
        item {
            LeaderboardSection(leaderboards = leaderboards)
        }

        // Recent Code Reviews
        if (codeReviews.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Peer Reviews",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            items(codeReviews.take(3)) { review ->
                PeerReviewCard(review = review)
            }
        }

        // Active Collaborative Challenges
        if (collaborativeChallenges.isNotEmpty()) {
            item {
                Text(
                    text = "Active Collaborative Challenges",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            items(collaborativeChallenges.take(3)) { challenge ->
                CollaborativeChallengeCard(
                    challenge = challenge,
                    onJoinChallenge = { viewModel.joinCollaborativeChallenge(challenge.id) }
                )
            }
        }

        // User Study Groups
        if (userStudyGroups.isNotEmpty()) {
            item {
                Text(
                    text = "My Study Groups",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            items(userStudyGroups) { studyGroup ->
                StudyGroupCard(studyGroup = studyGroup)
            }
        }
    }

    // Loading Overlay
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    // Error Snackbar
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // TODO: Show snackbar with error
            viewModel.clearError()
        }
    }

    // Success Snackbar
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            // TODO: Show snackbar with success message
            viewModel.clearSuccessMessage()
        }
    }

    // Create Challenge Dialog
    if (showCreateChallengeDialog) {
        CreateCollaborativeChallengeDialog(
            onDismiss = { showCreateChallengeDialog = false },
            onCreateChallenge = { theme, difficulty, maxParticipants ->
                viewModel.createCollaborativeChallenge(theme, difficulty, maxParticipants)
                showCreateChallengeDialog = false
            }
        )
    }

    // Create Study Group Dialog
    if (showCreateStudyGroupDialog) {
        CreateStudyGroupDialog(
            onDismiss = { showCreateStudyGroupDialog = false },
            onCreateStudyGroup = { name, description, focusSkills, targetLevel, maxMembers ->
                viewModel.createStudyGroup(name, description, focusSkills, targetLevel, maxMembers)
                showCreateStudyGroupDialog = false
            }
        )
    }
}

@Composable
private fun SocialProfileCard(
    socialProfile: SocialProfile?,
    onNavigateToSkillTree: () -> Unit,
    onNavigateToProjects: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        text = socialProfile?.displayName ?: "Loading...",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Level ${socialProfile?.level ?: 1} • ${socialProfile?.totalXp ?: 0} XP",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    modifier = Modifier.size(48.dp)
                )
            }

            // Social stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SocialStatItem(
                    label = "Skill Badges",
                    value = "${socialProfile?.skillBadges?.size ?: 0}"
                )
                SocialStatItem(
                    label = "Achievements",
                    value = "${socialProfile?.achievements?.size ?: 0}"
                )
                SocialStatItem(
                    label = "Reputation",
                    value = "${socialProfile?.reputation ?: 0}"
                )
            }

            // Quick navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToSkillTree,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AccountTree, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Skill Tree")
                }

                OutlinedButton(
                    onClick = onNavigateToProjects,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Work, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Projects")
                }
            }
        }
    }
}

@Composable
private fun SocialStatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CommunityChallengeCard(
    challenge: CommunityChallenge,
    onJoinChallenge: () -> Unit,
    onLeaveChallenge: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 Community Challenge",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text(challenge.difficulty.name) }
                )
            }

            Text(
                text = challenge.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Theme: ${challenge.theme}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(onClick = onJoinChallenge) {
                    Text("Join Challenge")
                }
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    onCreateCollaborativeChallenge: () -> Unit,
    onCreateStudyGroup: () -> Unit,
    onGeneratePeerFeedback: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCreateCollaborativeChallenge,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Group, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Create Challenge", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onCreateStudyGroup,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.School, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Study Group", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onGeneratePeerFeedback,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RateReview, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Peer Review", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardSection(
    leaderboards: Map<LeaderboardType, List<LeaderboardEntry>>
) {
    if (leaderboards.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Leaderboards",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(leaderboards.entries.toList()) { entry ->
                LeaderboardCard(
                    category = entry.key,
                    topEntries = entry.value.take(3)
                )
            }
        }
    }
}

@Composable
private fun LeaderboardCard(
    category: LeaderboardType,
    topEntries: List<LeaderboardEntry>
) {
    Card(
        modifier = Modifier.width(200.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = category.name.replace("_", " "),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            topEntries.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}. User",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${entry.points}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Dialog components would be implemented here
@Composable
private fun CreateCollaborativeChallengeDialog(
    onDismiss: () -> Unit,
    onCreateChallenge: (String, DifficultyLevel, Int) -> Unit
) {
    // TODO: Implement create challenge dialog
}

@Composable
private fun CreateStudyGroupDialog(
    onDismiss: () -> Unit,
    onCreateStudyGroup: (String, String, List<String>, DifficultyLevel, Int) -> Unit
) {
    // TODO: Implement create study group dialog
}
