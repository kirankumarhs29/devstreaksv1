package com.dailydevchallenge.devstreaks.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.AdaptiveChallenge
import com.dailydevchallenge.devstreaks.repository.UserProgressRepositoryImpl
import kotlinx.coroutines.launch

@Composable
fun AdaptiveChallengeScreen(
    userId: String,
    userProgressRepository: UserProgressRepositoryImpl
) {
    val scope = rememberCoroutineScope()
    var challenges by remember { mutableStateOf<List<AdaptiveChallenge>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        loading = true
        challenges = userProgressRepository.getAdaptiveChallenges(userId)
        loading = false
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        if (challenges.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No adaptive challenges found.")
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                items(challenges) { challenge ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(challenge.title, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(challenge.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text("Difficulty: ${challenge.difficulty}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

