package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
// learning intent screen
import com.dailydevchallenge.devstreaks.features.onboarding.ChatInputField
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

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
            DevStreakTopBar(
                title = "DevCoach",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Routes.clearDevChat)
                    }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        },
        bottomBar = {
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .consumeWindowInsets(padding) // 🔑 fix
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .imePadding() // Ensures content scrolls above keyboard
                    .imePadding()
                    .navigationBarsPadding(),
                reverseLayout = true // newest at bottom
            ) {
                item {
                    profile?.let {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { showProfile = !showProfile }) {
                                Text(if (showProfile) "🔼 Hide Profile" else "🔽 Show Profile")
                            }

                            AnimatedVisibility(showProfile) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("🧑 Your Learning Profile", style = MaterialTheme.typography.titleSmall)
                                        Spacer(Modifier.height(6.dp))
                                        Text("🎯 Goal: ${it.goal}")
                                        Text("💪 Skills: ${it.skills.joinToString()}")
                                        Text("🧠 Style: ${it.style}")
                                        Text("⏰ Time: ${it.timePerDay} for ${it.days} days")
                                        Text("😨 Fear: ${it.fear}")
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

                items(chatMessages.reversed()) { msg ->
                    ChatBubble(message = msg)
                }


                if (isTyping) {
                    item {
                        ChatBubble(
                            message = ChatUIMessage.Received("...") // Or a better loading effect
                        )
                    }
                }
            }
        }
    }
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

//fun parseMarkdown(text: String): List<String> {
//    // Simple Markdown parsing logic
//    // This can be extended to handle more complex Markdown features
//    return text.split("\n").map { line ->
//        line.replace("**", "").replace("*", "") // Remove bold and italic markers
//            .replace("`", "") // Remove inline code markers
//            .trim() // Trim whitespace
//    }
//}

@OptIn(ExperimentalTime::class)
fun formatTimestamp(epochMillis: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMillis)
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}
@Composable
fun ChatBubble(message: ChatUIMessage, isLoading: Boolean = false) {
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

    AnimatedVisibility(visible = true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
//                if (!isUser) {
//                    Icon(
//                        imageVector = Icons.Default.History,
//                        contentDescription = "Bot",
//                        modifier = Modifier.size(20.dp),
//                        tint = MaterialTheme.colorScheme.primary
//                    )
//                    Spacer(modifier = Modifier.width(4.dp))
//                }

                Surface(
                    shape = shape,
                    color = bubbleColor,
                    tonalElevation = 1.dp,
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp)
                        .widthIn(max = 320.dp)
                        .animateContentSize()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        MarkdownText(text = if (isLoading) "..." else text, color = textColor)
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

//                if (isUser) {
//                    Spacer(modifier = Modifier.width(4.dp))
//                    Icon(
//                        imageVector = Icons.Default.Send,
//                        contentDescription = "You",
//                        modifier = Modifier.size(20.dp),
//                        tint = MaterialTheme.colorScheme.primary
//                    )
//                }
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






