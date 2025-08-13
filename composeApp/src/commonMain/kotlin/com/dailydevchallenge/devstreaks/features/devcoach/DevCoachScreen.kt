package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.koin.compose.koinInject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.navigation.DevStreakTopBar
import com.dailydevchallenge.devstreaks.features.routes.Routes
import com.dailydevchallenge.devstreaks.llm.ChatUIMessage
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfilePreferences
import com.dailydevchallenge.devstreaks.features.onboarding.ChatInputField
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import com.dailydevchallenge.devstreaks.features.dailyCoach.DevCoachLottieSpeakingAvatar
import com.dailydevchallenge.devstreaks.features.dailyCoach.TTSController
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile

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
    var showProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(chatMessages.size) {
        listState.animateScrollToItem(chatMessages.size)
    }

    Scaffold(
        topBar = {
            DevStreakTopBar(
                title = "DevCoach",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Routes.clearDevChat)
                    }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                    IconButton(onClick = { viewModel.clearChat() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Chat")
                    }
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(Icons.Default.Person, contentDescription = "Show Profile")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                val lastAiMessage = chatMessages.lastOrNull { it is ChatUIMessage.Received }

                items(chatMessages) { msg ->
                    val shouldSpeak = msg == lastAiMessage && !isTyping
                    ChatBubble1(message = msg, shouldSpeak = shouldSpeak)
                }

                if (isTyping) {
                    item {
                        ChatBubble1(
                            message = ChatUIMessage.Received("...") // Or a better loading effect
                        )
                    }
                }
            }

            ChatInputField(
                input = userInput,
                onInputChange = { userInput = it },
                onSend = {
                    if (userInput.isNotBlank()) {
                        viewModel.sendMessage(userInput)
                        userInput = ""
                    }
                }
            )
            if (showProfileDialog && profile != null) {
                ProfileDialog(
                    profile = profile,
                    onDismissRequest = { showProfileDialog = false },
                    onEditClicked = {
                        showProfileDialog = false
                        navController.navigate(Routes.editProfile)
                    }
                )
            }
        }
    }
}
@Composable
fun ProfileDialog(
    profile: LearningProfile,
    onDismissRequest: () -> Unit,
    onEditClicked: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Your Learning Profile") },
        text = {
            Column {
                Text("🎯 Goal: ${profile.goal}")
                Text("💪 Skills: ${profile.skills.joinToString()}")
                Text("🧠 Style: ${profile.style}")
                Text("⏰ Time: ${profile.timePerDay} for ${profile.days} days")
                Text("😨 Fear: ${profile.fear}")
            }
        },
        confirmButton = {
            TextButton(onClick = onEditClicked) {
                Text("Edit Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}


//@Composable
//fun SuggestionChip(text: String, onClick: () -> Unit) {
//    AssistChip(
//        onClick = onClick,
//        label = { Text(text) },
//        shape = RoundedCornerShape(50),
//        colors = AssistChipDefaults.assistChipColors()
//    )
//}


fun formatTimestamp(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}
@Composable
fun ChatBubble1(message: ChatUIMessage, isLoading: Boolean = false,
                shouldSpeak: Boolean = false ) {

    val isUser = message is ChatUIMessage.Sent
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val shape = if (isUser)
        RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
    else
        RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)

    val text = when (message) {
        is ChatUIMessage.Sent -> message.text
        is ChatUIMessage.Received -> message.text
    }

    val timestamp = when (message) {
        is ChatUIMessage.Sent -> message.timestamp
        is ChatUIMessage.Received -> message.timestamp
    }

    val time = formatTimestamp(timestamp)
    var isSpeaking by remember { mutableStateOf(false) }
    if (!isUser) {
        LaunchedEffect(shouldSpeak) {
            if (shouldSpeak) {
                isSpeaking = true
                TTSController.speak(text) {
                    isSpeaking = false
                }
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Surface(
                shape = shape,
                color = bubbleColor,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp)
                    .widthIn(max = 320.dp)
                    .animateContentSize()
                    .clickable(enabled = !isUser) {
                        isSpeaking = true
                        TTSController.speak(text) {
                            isSpeaking = false
                        }
                    }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (isUser) {
                        MarkdownText(text = if (isLoading) "..." else text, color = textColor)
                    } else {
                        DevCoachLottieSpeakingAvatar(isSpeaking)
                        MarkdownText(text = text, color = textColor)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = time,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.align(
                            if (isUser) Alignment.End else Alignment.Start
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun MarkdownText(text: String, color: Color) {
    val parsed = parseMarkdown(text)
    Text(text = parsed, color = color)
}

fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val regex = Regex("""(\*\*.+?\*\*|\*.+?\*|`.+?`)""")
        var lastIndex = 0

        for (match in regex.findAll(text)) {
            append(text.substring(lastIndex, match.range.first))
            val content = match.value.removeSurrounding("**").removeSurrounding("*").removeSurrounding("`")

            when {
                match.value.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(content) }
                match.value.startsWith("*") -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(content) }
                match.value.startsWith("`") -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(content) }
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) append(text.substring(lastIndex))
    }
}
