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
import com.dailydevchallenge.devstreaks.model.CoachingContextType
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

    // Integrate adaptive intelligence for personalized coaching
    val adaptiveOrchestrator: com.dailydevchallenge.devstreaks.service.AdaptiveIntelligenceOrchestrator = koinInject()
    val userId = com.dailydevchallenge.devstreaks.settings.UserPreferences.getSafeUserId()
    var adaptiveCoaching by remember { mutableStateOf<com.dailydevchallenge.devstreaks.model.PersonalizedCoachingResponse?>(null) }
    var weakAreas by remember { mutableStateOf<List<com.dailydevchallenge.devstreaks.model.WeakArea>>(emptyList()) }

    // Load adaptive coaching insights on screen load
    LaunchedEffect(Unit) {
        try {
            val coaching = adaptiveOrchestrator.getPersonalizedCoaching(
                userId = userId,
                userMessage = "I need general coaching and learning guidance",
                contextType = CoachingContextType.GENERAL_GUIDANCE,
                learningProfile = profile
            )
            adaptiveCoaching = coaching

            // Get weak areas for context
            val detectedWeakAreas = adaptiveOrchestrator.detectWeakAreas(userId)
            weakAreas = detectedWeakAreas

        } catch (e: Exception) {
            // Handle error silently for better UX
        }
    }

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
        ) {
            // Adaptive Intelligence Insights Panel
            adaptiveCoaching?.let { coaching ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "🧠 AI Coaching Insights",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(4.dp))

                        coaching.motivationalMessage?.let { message ->
                            if (message.isNotEmpty()) {
                                Text(
                                    "💪 $message",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }

                        if (coaching.strategicGuidance.isNotEmpty()) {
                            Text(
                                "🎯 ${coaching.strategicGuidance.first()}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Weak Areas Panel
            if (weakAreas.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "🎯 Focus Areas",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.height(4.dp))

                        weakAreas.take(2).forEach { area ->
                            Text(
                                "• ${area.skillArea}: ${area.recommendedActions.firstOrNull() ?: "Focus on improving this skill"}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 1.dp)
                            )
                        }

                        if (weakAreas.size > 2) {
                            Text(
                                "... and ${weakAreas.size - 2} more areas",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Chat Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages) { message ->
                    ChatMessageItem(
                        message = message,
                        profile = profile,
                        // Pass weak areas context for enhanced coaching
                        weakAreasContext = weakAreas.map { "${it.skillArea}: ${it.recommendedActions.firstOrNull() ?: "Focus on improving this skill"}" }
                    )
                }

                if (isTyping) {
                    item {
                        // Simple typing indicator since TypingIndicator is not available
                        Card(
                            modifier = Modifier.padding(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("DevCoach is typing...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Enhanced Chat Input with Adaptive Context
            ChatInputField(
                input = userInput,
                onInputChange = { userInput = it },
                onSend = {
                    // Enhanced message with adaptive context
                    val enhancedMessage = buildContextualMessage(userInput, weakAreas, adaptiveCoaching)
                    viewModel.sendMessage(enhancedMessage)
                    userInput = ""
                }
            )
        }

        // Profile Dialog
        if (showProfileDialog) {
            AlertDialog(
                onDismissRequest = { showProfileDialog = false },
                title = { Text("Learning Profile") },
                text = {
                    Column {
                        profile?.let { p ->
                            Text("Difficulty: ${p.fear}")
                            Text("Learning Style: ${p.style}")
                            Text("Goals: ${p.goal}")
                        } ?: Text("No profile set")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showProfileDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

/**
 * Enhanced ChatMessageItem with weak areas context for better coaching
 */
@Composable
private fun ChatMessageItem(
    message: ChatUIMessage,
    profile: LearningProfile?,
    weakAreasContext: List<String> = emptyList()
) {
    val isUser = message is ChatUIMessage.Sent
    val messageText = when (message) {
        is ChatUIMessage.Sent -> message.text
        is ChatUIMessage.Received -> message.text
    }
    val messageTimestamp = when (message) {
        is ChatUIMessage.Sent -> message.timestamp
        is ChatUIMessage.Received -> message.timestamp
    }

    // Add visual indicators for coaching messages related to weak areas
    val isWeakAreaRelated = weakAreasContext.any { context ->
        messageText.contains(context.substringBefore(":"), ignoreCase = true)
    }

    val cardColor = when {
        isUser -> MaterialTheme.colorScheme.primary
        isWeakAreaRelated -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(if (isUser) 0.8f else 1f)
            .let { if (isUser) it.wrapContentWidth(Alignment.End) else it },
        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (!isUser && isWeakAreaRelated) {
                Text(
                    "🎯 Targeted Coaching",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(Modifier.height(4.dp))
            }

            Text(
                text = messageText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))
            Text(
                text = Instant.fromEpochMilliseconds(messageTimestamp)
                    .toLocalDateTime(TimeZone.currentSystemDefault()).time.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Build contextual message that includes weak areas and adaptive coaching insights
 */
private fun buildContextualMessage(
    userMessage: String,
    weakAreas: List<com.dailydevchallenge.devstreaks.model.WeakArea>,
    coaching: com.dailydevchallenge.devstreaks.model.PersonalizedCoachingResponse?
): String {
    val contextualInfo = mutableListOf<String>()

    // Add weak areas context if relevant
    if (weakAreas.isNotEmpty()) {
        contextualInfo.add("Current focus areas: ${weakAreas.joinToString(", ") { it.skillArea }}")
    }

    // Add coaching context if available
    coaching?.let {
        if (it.learningTips.isNotEmpty()) {
            contextualInfo.add("Learning preference: ${it.learningTips.first()}")
        }
    }

    return if (contextualInfo.isNotEmpty()) {
        "$userMessage\n\nContext: ${contextualInfo.joinToString("; ")}"
    } else {
        userMessage
    }
}

/**
 * Get adaptive placeholder based on user's weak areas
 */
private fun getAdaptivePlaceholder(weakAreas: List<com.dailydevchallenge.devstreaks.model.WeakArea>): String {
    return when {
        weakAreas.isNotEmpty() -> "Ask about ${weakAreas.first().skillArea} or any coding topic..."
        else -> "Ask me anything about coding..."
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
