package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.koin.compose.koinInject
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.IconButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import com.dailydevchallenge.devstreaks.repository.Conversation


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationHistoryScreen(
    viewModel: DevChatViewModel = koinInject(),
    onBack: () -> Unit = {}
) {
    var conversations by remember { mutableStateOf(emptyList<Conversation>()) }

    var isClearing by remember { mutableStateOf(false) }

    LaunchedEffect(isClearing) {
        if (!isClearing) {
            conversations = viewModel.loadAllConversations()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🗂️ Conversation History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (conversations.isNotEmpty()) {
                        IconButton(onClick = {
                            isClearing = true
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete All")
                        }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (conversations.isEmpty()) {
                item {
                    Text("No conversations yet.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                items(conversations) { convo ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("🧑‍💻 You: ${convo.userMessage}", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(6.dp))
                            Text("🤖 DevCoach: ${convo.botResponse}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (isClearing) {
        AlertDialog(
            onDismissRequest = { isClearing = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllConversations {
                        isClearing = false
                    }
                }) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { isClearing = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete all conversations?") }
        )
    }
}
