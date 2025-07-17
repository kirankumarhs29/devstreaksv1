package com.dailydevchallenge.devstreaks.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.devstreaks.features.navigation.DevStreakTopBar
import com.dailydevchallenge.devstreaks.utils.PlatformUtils
import com.dailydevchallenge.devstreaks.utils.SafeBackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
@Composable
fun LearningIntentScreen(
    viewModel: OnboardingViewModel,
    navController: NavController,
    onFinish: (goal: String, skills: List<String>, experience: String, timePerDay: String, days: String, style: String, fear: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var currentInput by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    if (PlatformUtils.isAndroid()) {
        SafeBackHandler(enabled = true) {
            val hasUserStarted = messages.size > 1 || currentInput.isNotBlank() || isTyping
            if (hasUserStarted) {
                showExitDialog = true
            } else {
                navController.popBackStack()
            }
        }
    }


    LaunchedEffect(Unit) {
        messages = listOf(
            ChatMessage(
                text = "👋 Hey Dev! What’s your next big learning goal?",
                isUser = false,
                inputType = InputType.TEXT
            )
        )
    }

    LaunchedEffect(messages.size, currentInput) {
        delay(100)
        scrollState.animateScrollToItem(messages.size)
    }

    Scaffold(
        topBar = {
            DevStreakTopBar(
                title = "Your Dev Journey",
                onBack = {
                    val hasUserStarted = messages.size > 1 || currentInput.isNotBlank() || isTyping
                    if (hasUserStarted) {
                        showExitDialog = true
                    } else {
                        navController.popBackStack()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
                if (isTyping) {
                    item {
                        ChatBubble(ChatMessage(text = "Typing...", isUser = false), isLoading = true)
                    }
                }
            }

            val latest = messages.lastOrNull()

            when (latest?.inputType) {
                InputType.TEXT -> {
                    Surface(
                        tonalElevation = 4.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    ) {
                        ChatInputField(
                            input = currentInput,
                            onInputChange = { currentInput = it },
                            onSend = {
                                coroutineScope.launch {
                                    sendUserMessage(
                                        input = currentInput,
                                        currentMessages = messages,
                                        onFinish = onFinish,
                                        onUpdate = { messages = it },
                                        setTyping = { isTyping = it },
                                        viewModel = viewModel
                                    )
                                    currentInput = ""
                                    keyboardController?.hide()
                                }
                            }
                        )
                    }
                }

                InputType.MULTI_CHOICE -> {
                    ChoiceChips(options = latest.options) { selected ->
                        coroutineScope.launch {
                            sendUserMessage(
                                input = selected,
                                currentMessages = messages,
                                onFinish = onFinish,
                                onUpdate = { messages = it },
                                setTyping = { isTyping = it },
                                viewModel = viewModel
                            )
                        }
                    }
                }

                else -> {}
            }
        }
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("Exit Onboarding?") },
                text = { Text("Are you sure you want to exit? Your progress will be lost.") },
                confirmButton = {
                    TextButton(onClick = {
                        showExitDialog = false
                        navController.popBackStack()
                    }) {
                        Text("Exit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Stay")
                    }
                }
            )
        }

    }
}



private suspend fun sendUserMessage(
    input: String,
    currentMessages: List<ChatMessage>,
    onFinish: (String, List<String>, String, String, String, String, String) -> Unit,
    onUpdate: (List<ChatMessage>) -> Unit,
    setTyping: (Boolean) -> Unit,
    viewModel: OnboardingViewModel
) {
    if (input.isBlank()) return

    val updated = currentMessages + ChatMessage(text = input, isUser = true)
    onUpdate(updated)
    setTyping(true)
    delay(700)

    val userInputs = updated.filter { it.isUser }.map { it.text }

    val nextMessage = when (userInputs.size) {
        1 -> ChatMessage(
            text = "🧠 What skills are you already confident in? (e.g. Kotlin, Git)",
            isUser = false,
            inputType = InputType.TEXT
        )

        2 -> ChatMessage(
            text = "💻 What’s your coding experience level?",
            isUser = false,
            inputType = InputType.MULTI_CHOICE,
            options = listOf("Beginner", "1-2 yrs", "3+ yrs")
        )

        3 -> ChatMessage(
            text = "⏰ How much time per day can you dedicate?",
            isUser = false,
            inputType = InputType.MULTI_CHOICE,
            options = listOf("15 mins", "30 mins", "1 hour")
        )

        4 -> ChatMessage(
            text = "📅 How many days do you want your plan to last?",
            isUser = false,
            inputType = InputType.MULTI_CHOICE,
            options = listOf("5", "7", "10")
        )

        5 -> ChatMessage(
            text = "🎓 Choose your learning style",
            isUser = false,
            inputType = InputType.MULTI_CHOICE,
            options = listOf("Project-based", "Concept-first", "Challenge-based")
        )

        6 -> ChatMessage(
            text = "😨 What’s your fear zone or area to improve?",
            isUser = false,
            inputType = InputType.TEXT
        )
        7 -> {
            val goal = userInputs.getOrNull(0) ?: ""
            val skillsRaw = userInputs.getOrNull(1) ?: ""
            val experience = userInputs.getOrNull(2) ?: ""
            val time = userInputs.getOrNull(3) ?: ""
            val days = userInputs.getOrNull(4) ?: ""
            val style = userInputs.getOrNull(5) ?: ""
            val fear = userInputs.getOrNull(6) ?: ""

            val skills = skillsRaw.split(",").map { it.trim() }
            viewModel.markOnboardingComplete()
            onFinish(goal, skills, experience, time, days, style, fear)


            ChatMessage(
                text = "✅ Awesome! Your personalized plan is loading... 🚀",
                isUser = false,
                inputType = InputType.NONE
            )
        }
        else -> null
    }

    nextMessage?.let { onUpdate(updated + it) }
    setTyping(false)
}

@Composable
fun ChatInputField(input: String, onInputChange: (String) -> Unit, onSend: () -> Unit) {
    val inputBackgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(8.dp)
            .background(inputBackgroundColor, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
    ) {
        TextField(
            value = input,
            onValueChange = onInputChange,
            placeholder = { Text("Type here...") },
            maxLines = 3,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        IconButton(onClick = onSend) {
            Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun ChoiceChips(options: List<String>, onSelect: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        options.forEach { option ->
            AssistChip(onClick = { onSelect(option) }, label = { Text(option) })
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, isLoading: Boolean = false) {
    val isUser = message.isUser
    val alignment = if (isUser) Arrangement.End else Arrangement.Start
    val bubbleColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant

    val textColor = MaterialTheme.colorScheme.onSurface

    val shape = if (isUser)
        RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)
    else
        RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = alignment
    ) {
        Surface(shape = shape, color = bubbleColor, tonalElevation = 1.dp) {
            Text(
                text = if (isLoading) "..." else message.text,
                modifier = Modifier.padding(12.dp),
                color = textColor
            )
        }
    }
}
