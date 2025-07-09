package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.koin.compose.koinInject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.routes.Routes
import com.dailydevchallenge.devstreaks.llm.ChatUIMessage
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfilePreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevCoachScreen(
    navController: NavController,
    viewModel: DevChatViewModel = koinInject(),
    onBack: () -> Unit = {}
) {
    val chatMessages = viewModel.chatMessages
    val isTyping = viewModel.isTyping
    var userInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val profile = remember { LearningProfilePreferences.getProfile() }
    var showProfile by remember { mutableStateOf(true) }

    LaunchedEffect(chatMessages.size) {
        listState.animateScrollToItem(chatMessages.size)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("💬 DevCoach Chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Routes.clearDevChat)
                    }) {
                        Icon(Icons.Default.History, contentDescription = "View History")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it },
                    label = { Text("Ask something...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (userInput.isNotBlank()) {
                            viewModel.sendMessage(userInput)
                            userInput = ""
                        }
                    },
                    enabled = userInput.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 12.dp),
            state = listState
        ) {
            profile?.let {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { showProfile = !showProfile }) {
                            Text(if (showProfile) "🔼 Hide Profile" else "🔽 Show Profile")
                        }

                        AnimatedVisibility(visible = showProfile) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("🧑 Your Learning Profile", style = MaterialTheme.typography.titleSmall)
                                    Spacer(Modifier.height(6.dp))
                                    Text("🎯 Goal: ${it.goal}", style = MaterialTheme.typography.bodyMedium)
                                    Text("💪 Skills: ${it.skills.joinToString()}", style = MaterialTheme.typography.bodyMedium)
                                    Text("🧠 Style: ${it.style}", style = MaterialTheme.typography.bodyMedium)
                                    Text("⏰ Time: ${it.timePerDay} for ${it.days} days", style = MaterialTheme.typography.bodyMedium)
                                    Text("😨 Fear: ${it.fear}", style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(6.dp))
                                    TextButton(onClick = { navController.navigate(Routes.editProfile) }) {
                                        Text("Edit Profile")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            items(chatMessages) { msg ->
                ChatBubble(msg)
            }

            if (isTyping) {
                item {
                    Text(
                        "DevCoach is typing...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun ChatBubble(message: ChatUIMessage) {
    val isUser = message is ChatUIMessage.Sent
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    // Parse the message text for Markdown or other formatting
    val bubbleShape = if (isUser) {
        RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
    }

    val parsedTexts = parseMarkdown(
        when (message) {
            is ChatUIMessage.Sent -> message.text
            is ChatUIMessage.Received -> message.text
        }
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 3.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .widthIn(max = 280.dp) // optional width constraint
            ) {
                parsedTexts.forEach { line ->
                    Text(
                        text = line,
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}


@Composable
fun SuggestionChip(text: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        shape = RoundedCornerShape(50),
        colors = AssistChipDefaults.assistChipColors()
    )
}

fun parseMarkdown(text: String): List<String> {
    // Simple Markdown parsing logic
    // This can be extended to handle more complex Markdown features
    return text.split("\n").map { line ->
        line.replace("**", "").replace("*", "") // Remove bold and italic markers
            .replace("`", "") // Remove inline code markers
            .trim() // Trim whitespace
    }
}


