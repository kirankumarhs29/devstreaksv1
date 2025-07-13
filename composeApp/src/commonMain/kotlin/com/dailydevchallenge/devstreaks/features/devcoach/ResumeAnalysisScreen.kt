package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.model.InterviewQuestion
import com.dailydevchallenge.devstreaks.model.ResumeAnalysis
import org.koin.compose.koinInject
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.utils.getLogger
import com.dailydevchallenge.devstreaks.utils.getPdfPickerHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ResumeAndInterviewScreen(
    resumeChatViewModel: ResumeChatViewModel = koinInject()
) {
    var showPicker by remember { mutableStateOf(false) }
    var resumeText by remember { mutableStateOf("") }
    var jobRole by remember { mutableStateOf("") }

    var analysis by remember { mutableStateOf<ResumeAnalysis?>(null) }
    var questions by remember { mutableStateOf<List<InterviewQuestion>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var isInterviewMode by remember { mutableStateOf(false) }

    val chatMessages = remember { mutableStateListOf<ChatMessage>() }
    val coroutineScope = rememberCoroutineScope()
    val logger = remember { getLogger() }
    val llmService = koinInject<LLMService>()
    val scrollState = rememberLazyListState()

    if (showPicker) {
        getPdfPickerHandler().PickPdf {
            resumeChatViewModel.setResumeText(it)
            logger.d("Resume", "Resume Text set: $it")
            resumeText = it
            showPicker = false
        }
    }

    // Show resume analysis as chat
    LaunchedEffect(analysis) {
        analysis?.let {
            chatMessages += ChatMessage.System("📝 Resume Analysis for '$jobRole'")
            chatMessages += ChatMessage.System("🧠 Summary: ${it.summary}")
            chatMessages += ChatMessage.System("✅ Strengths: ${it.skillsMatched.joinToString()}")
            chatMessages += ChatMessage.System("⚠️ Weaknesses: ${it.skillsMissing.joinToString()}")
            chatMessages += ChatMessage.System("🎯 Role Fit Score: ${it.jobMatchScore}")
            chatMessages += ChatMessage.System("📚 Recommendations: ${it.recommendations}")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        state = scrollState,
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chatMessages.reversed()) { msg ->
            ChatBubble(message = msg)
        }

        item {
            if (!isInterviewMode) {
                Column {
                    OutlinedTextField(
                        value = jobRole,
                        onValueChange = { jobRole = it },
                        label = { Text("Enter Target Job Role") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    Button(onClick = { showPicker = true }) {
                        Text("📄 Pick Resume PDF")
                    }

                    if (resumeText.isNotBlank()) {
                        Text("✅ Resume Loaded", color = Color.Green)
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val analysisResult = llmService.analyzeResume(resumeText, jobRole)
                                    val generatedQuestions = llmService.generateMockInterview(
                                        jobRole,
                                        analysisResult.summary,
                                        analysisResult.skillsMatched
                                    )
                                    analysis = analysisResult
                                    questions = generatedQuestions
                                    isInterviewMode = true
                                } catch (_: Exception) {
                                    analysis = null
                                    questions = emptyList()
                                    isInterviewMode = false
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        enabled = resumeText.isNotBlank() && jobRole.isNotBlank()
                    ) {
                        Text("🚀 Analyze & Start Interview")
                    }

                    if (isLoading) {
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    if (isInterviewMode && questions.isNotEmpty()) {
        InteractiveInterviewChat(
            questions = questions,
            chatMessages = chatMessages
        ) {
            // Retry logic
            isInterviewMode = false
            questions = emptyList()
            analysis = null
            resumeText = ""
            jobRole = ""
        }
    }
}

@Composable
fun InteractiveInterviewChat(
    questions: List<InterviewQuestion>,
    chatMessages: SnapshotStateList<ChatMessage>,
    onRetry: () -> Unit
) {
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    var timeLeft by remember { mutableStateOf(30) }
    var showSubmit by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    // Show question
    LaunchedEffect(currentQuestionIndex) {
        val q = questions[currentQuestionIndex]
        chatMessages.add(ChatMessage.System("🎤 Q${currentQuestionIndex + 1}: ${q.question}"))

        delay(500)
        timeLeft = 30
        showSubmit = false

        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        showSubmit = true
    }

    LaunchedEffect(chatMessages.size) {
        scrollState.animateScrollToItem(chatMessages.size)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Spacer(Modifier.height(4.dp))
        Text("⏱️ Time left: $timeLeft s", color = Color.Red)

        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Your Answer") }
        )

        Spacer(Modifier.height(8.dp))

        if (!isFinished) {
            Button(
                onClick = {
                    if (userInput.isNotBlank()) {
                        chatMessages.add(ChatMessage.User(userInput))
                        val followUp = questions[currentQuestionIndex].followUp
                        if (followUp.isNotBlank()) {
                            chatMessages.add(ChatMessage.System("💡 Follow-up: $followUp"))
                        }

                        userInput = ""
                        if (currentQuestionIndex + 1 < questions.size) {
                            currentQuestionIndex++
                        } else {
                            chatMessages.add(ChatMessage.System("✅ Interview Complete. Well done!"))
                            isFinished = true
                        }
                    }
                },
                enabled = userInput.isNotBlank()
            ) {
                Text(if (currentQuestionIndex < questions.lastIndex) "➡️ Submit & Next" else "✅ Submit & Finish")
            }
        } else {
            Button(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("🔁 Retry Interview")
            }
        }
    }
}

sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class System(val text: String) : ChatMessage()
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val (bgColor, contentText, alignment) = when (message) {
        is ChatMessage.User -> Triple(Color(0xFFDCF8C6), message.text, Arrangement.End)
        is ChatMessage.System -> Triple(Color(0xFFECECEC), message.text, Arrangement.Start)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = alignment
    ) {
        Text(
            text = contentText,
            color = Color.Black,
            modifier = Modifier
                .background(bgColor, shape = MaterialTheme.shapes.medium)
                .padding(12.dp)
        )
    }
}

