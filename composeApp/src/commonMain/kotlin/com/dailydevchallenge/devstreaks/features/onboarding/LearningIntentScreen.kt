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
import com.dailydevchallenge.devstreaks.features.devcoach.ResumeChatViewModel
import com.dailydevchallenge.devstreaks.features.navigation.DevStreakTopBar
import com.dailydevchallenge.devstreaks.model.LearningIntent
import com.dailydevchallenge.devstreaks.model.ResumeAnalysis
import com.dailydevchallenge.devstreaks.utils.PlatformUtils
import com.dailydevchallenge.devstreaks.utils.SafeBackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
@Composable
fun LearningIntentScreen(
    viewModel: OnboardingViewModel,
    navController: NavController,
    resumeChatViewModel: ResumeChatViewModel, // add this param!
    onFinish: (goal: String, skills: List<String>, experience: String, timePerDay: String, days: String, style: String, fear: String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var currentInput by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    val resumeHistory by resumeChatViewModel.resumeHistory.collectAsState()
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

    var latestResume: ResumeAnalysis? = null
    if (resumeHistory.isNotEmpty()) {
        // Get the latest resume analysis
        latestResume = resumeHistory.lastOrNull { it.skillsMatched.isNotEmpty() }
    }


    LaunchedEffect(Unit) {
        // Default greeting
        val resumeMsg = latestResume?.let {
            val summary = if (it.summary.isNotBlank()) "\n\nProfile summary:\n${it.summary}" else ""
            "🤩 Welcome back! We see your profile has: " + it.skillsMatched.joinToString(", ") + summary
        }
        // Build messages such that input prompt is always last!
        val baseMessages = buildList {
            add(
                ChatMessage(
                    text = "👋 Hey Dev! What’s your next big learning goal?",
                    isUser = false,
                    inputType = InputType.TEXT
                )
            )
            if (resumeMsg != null) {
                // Insert the info message right BEFORE the prompt, not after!
                add(0, ChatMessage(text = resumeMsg, isUser = false, inputType = InputType.NONE))
            }
        }
        messages = baseMessages
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
                                        latestResume = latestResume,
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
                                latestResume = latestResume,
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
    latestResume: ResumeAnalysis? = null,
    setTyping: (Boolean) -> Unit,
    viewModel: OnboardingViewModel
) {
    if (input.isBlank()) return

    val updated = currentMessages + ChatMessage(text = input, isUser = true)
    onUpdate(updated)
    setTyping(true)
    delay(700)

    val userInputs = updated.filter { it.isUser }.map { it.text }

    // Update learning intent in real-time as user provides information
    updateLearningIntentFromInputs(userInputs, latestResume, viewModel)

    val nextMessage = when (userInputs.size) {
        1 -> ChatMessage(
            text = "🧠 What skills are you already confident in? (e.g. Kotlin, Git, React)",
            isUser = false,
            inputType = InputType.TEXT
        )

        2 -> ChatMessage(
            text = "💻 What's your coding experience level? (e.g. Complete beginner, 2 years, 5+ years)",
            isUser = false,
            inputType = InputType.MULTI_CHOICE,
            options = listOf("Complete beginner", "Some coding experience", "1-2 years", "3-5 years", "5+ years")
        )

        3 -> ChatMessage(
            text = "🎯 What's your target career track?",
            isUser = false,
            inputType = InputType.MULTI_CHOICE,
            options = listOf("Frontend Developer", "Backend Developer", "Full Stack Developer", "Mobile Developer", "Data Scientist", "DevOps Engineer")
        )

        4 -> ChatMessage(
            text = "⏰ How much time per day can you dedicate?",
            isUser = false,
            inputType = InputType.MULTI_CHOICE,
            options = listOf("15 mins", "30 mins", "45 mins", "1 hour", "2 hours")
        )

        5 -> ChatMessage(
            text = "🎓 Choose your preferred learning style",
            isUser = false,
            inputType = InputType.MULTI_CHOICE,
            options = listOf("Hands-on projects", "Step-by-step tutorials", "Interactive challenges", "Video explanations")
        )

        6 -> ChatMessage(
            text = "😨 What's your biggest learning concern or area you want to improve?",
            isUser = false,
            inputType = InputType.TEXT
        )

        7 -> {
            val goal = userInputs.getOrNull(0) ?: ""
            val skillsRaw = userInputs.getOrNull(1) ?: ""
            val experience = userInputs.getOrNull(2) ?: ""
            val careerTrack = userInputs.getOrNull(3) ?: ""
            val timeInput = userInputs.getOrNull(4) ?: ""
            val learningStyle = userInputs.getOrNull(5) ?: ""
            val fears = userInputs.getOrNull(6) ?: ""

            val userSkills = skillsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val resumeSkills = latestResume?.skillsMatched ?: emptyList()
            val combinedSkills = (resumeSkills + userSkills).filter { it.isNotBlank() }.distinct()

            // Parse time commitment
            val timePerDay = when {
                timeInput.contains("15") -> 15
                timeInput.contains("30") -> 30
                timeInput.contains("45") -> 45
                timeInput.contains("1 hour") -> 60
                timeInput.contains("2 hours") -> 120
                else -> 30
            }

            // Create final learning intent
            val finalIntent = LearningIntent(
                primaryGoal = goal,
                skillFocus = combinedSkills,
                careerTrack = careerTrack,
                experience = experience,
                timePerDay = timePerDay,
                learningStyle = learningStyle,
                fears = fears,
                motivations = listOf("Daily improvement", "Career advancement")
            )

            // Save to UserContextManager
            viewModel.updateLearningIntent(finalIntent)

            // Mark onboarding step complete and trigger path generation
            viewModel.nextStep() // This will move to PersonalizationStep

            ChatMessage(
                text = "✅ Perfect! I have everything I need to create your personalized learning path. Let's review your preferences and generate your custom journey! 🚀",
                isUser = false,
                inputType = InputType.NONE
            )
        }
        else -> null
    }

    nextMessage?.let { onUpdate(updated + it) }
    setTyping(false)
}

// Helper function to update learning intent progressively
private suspend fun updateLearningIntentFromInputs(
    userInputs: List<String>,
    latestResume: ResumeAnalysis?,
    viewModel: OnboardingViewModel
) {
    val currentIntent = LearningIntent(
        primaryGoal = userInputs.getOrNull(0) ?: "",
        skillFocus = if (userInputs.size > 1) {
            val skillsRaw = userInputs.getOrNull(1) ?: ""
            val userSkills = skillsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
            val resumeSkills = latestResume?.skillsMatched ?: emptyList()
            (resumeSkills + userSkills).filter { it.isNotBlank() }.distinct()
        } else emptyList(),
        experience = userInputs.getOrNull(2) ?: "",
        careerTrack = userInputs.getOrNull(3) ?: "",
        timePerDay = when (userInputs.getOrNull(4)) {
            "15 mins" -> 15
            "30 mins" -> 30
            "45 mins" -> 45
            "1 hour" -> 60
            "2 hours" -> 120
            else -> 30
        },
        learningStyle = userInputs.getOrNull(5) ?: "",
        fears = userInputs.getOrNull(6) ?: "",
        motivations = listOf("Daily improvement", "Career advancement")
    )

    // Update the view model with progressive intent
    viewModel.updateLearningIntent(currentIntent)
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
