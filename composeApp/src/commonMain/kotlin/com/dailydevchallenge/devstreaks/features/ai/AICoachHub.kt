package com.dailydevchallenge.devstreaks.features.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.routes.Routes

/**
 * Central AI Hub that provides unified access to all AI features
 * Replaces scattered navigation with a cohesive AI assistant experience
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AICoachHub(
    navController: NavController,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🤖 AI Coach Hub") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AIFeatureCard(
                    title = "💬 Chat with DevCoach",
                    description = "Get instant help with coding, career advice, and motivation",
                    icon = Icons.Default.Chat,
                    onClick = { navController.navigate(Routes.DevCoach) }
                )
            }

            item {
                AIFeatureCard(
                    title = "📄 Resume Analysis",
                    description = "AI-powered resume review with personalized feedback",
                    icon = Icons.Default.Description,
                    onClick = { navController.navigate(Routes.ResumeAnalysis) }
                )
            }

            item {
                AIFeatureCard(
                    title = "🎯 Mock Interview",
                    description = "Practice interviews with adaptive AI questioning",
                    icon = Icons.Default.RecordVoiceOver,
                    onClick = { navController.navigate(Routes.InterviewHome) }
                )
            }

            item {
                AIFeatureCard(
                    title = "📊 Progress Insights",
                    description = "AI analysis of your learning journey and next steps",
                    icon = Icons.Default.Analytics,
                    isComingSoon = true,
                    onClick = { /* Navigate to new progress insights screen */ }
                )
            }

            item {
                AIFeatureCard(
                    title = "🔍 Code Review",
                    description = "Get instant feedback on your coding solutions",
                    icon = Icons.Default.Code,
                    isComingSoon = true,
                    onClick = { /* Navigate to dedicated code review */ }
                )
            }
        }
    }
}

@Composable
private fun AIFeatureCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isComingSoon: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (!isComingSoon) it else it },
        onClick = if (!isComingSoon) onClick else { {} }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isComingSoon) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                      else MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isComingSoon) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                           else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (isComingSoon) {
                    Text(
                        text = "Coming Soon",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!isComingSoon) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
