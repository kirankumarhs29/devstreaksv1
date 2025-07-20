package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
//import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.tts.SpeechToTextHelper
import com.dailydevchallenge.devstreaks.tts.TTSHelper
import org.koin.compose.koinInject
import com.dailydevchallenge.devstreaks.utils.getPdfPickerHandler
import com.mohamedrejeb.calf.core.LocalPlatformContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime

// --- Enhanced ChatMessage Types ---
sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class System(val text: String) : ChatMessage()
    data class FileUpload(val fileName: String) : ChatMessage()
    data class InterviewQuestionMsg(val question: String, val index: Int) : ChatMessage()
    data class Followup(val text: String) : ChatMessage()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun ResumeAndInterviewScreen(
    resumeChatViewModel: ResumeChatViewModel = koinInject()
) {
    val messages by resumeChatViewModel.chatMessages.collectAsState()
    val isLoading by resumeChatViewModel.isLoading.collectAsState()
    val inputText = remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()
    val speechHelper: SpeechToTextHelper = koinInject()
    val context = LocalPlatformContext.current
    val ttsHelper = remember { TTSHelper(context) }
    val resumeHistory by resumeChatViewModel.resumeHistory.collectAsState()
    val interviewSessions by resumeChatViewModel.interviewSessions.collectAsState()
    val sessionAnswers by resumeChatViewModel.sessionAnswers.collectAsState()
    var streak by remember { mutableStateOf(3) }

    // Ensures welcome message shows ONCE
    LaunchedEffect(Unit) { resumeChatViewModel.loadHistory() }
    LaunchedEffect(Unit) { resumeChatViewModel.startProfileSetup() }
    LaunchedEffect(messages.size) { scrollState.animateScrollToItem(messages.size) }
    DisposableEffect(Unit) { onDispose { ttsHelper.shutdown() } }
    val lastMsg = messages.lastOrNull()
    var showCelebrate by remember { mutableStateOf(false) }
    LaunchedEffect(messages) {
        if (messages.lastOrNull() is ChatMessage.System &&
            (messages.lastOrNull() as? ChatMessage.System)?.text?.contains("Interview complete") == true
        ) {
            showCelebrate = true
        } else {
            showCelebrate = false
        }
    }

    LaunchedEffect(lastMsg) {
        when (lastMsg) {
            is ChatMessage.InterviewQuestionMsg -> ttsHelper.speak(lastMsg.question)
            is ChatMessage.Followup -> ttsHelper.speak(lastMsg.text)
            // You can also include ChatMessage.System if you wish.
            else -> { /* do nothing */ }
        }
    }


    // Resume picker triggers ViewModel only
    if (showPicker) {
        getPdfPickerHandler().PickPdf {
            resumeChatViewModel.setResumeText(it)
            showPicker = false
        }
    }

    val lastSystemMsg = messages.lastOrNull { it is ChatMessage.System } as? ChatMessage.System
    val shouldShowFab = lastSystemMsg?.text?.contains("upload your resume", ignoreCase = true) == true

    Scaffold(
        topBar = {
            // Coach banner
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
                            )
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        "👋 Welcome! Ready to ace your next interview?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                if (streak > 1) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF3E0))
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥 Interview streak: $streak days!", color = Color(0xFFFB8C00), fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        floatingActionButton = {
            if (shouldShowFab && !isLoading) {
                FloatingActionButton(onClick = { showPicker = true }) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Upload Resume")
                }
            }
        },
    ) { paddingValues ->
        AnimatedVisibility(showCelebrate) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF169062))
                    .padding(14.dp)
            ) {
                Text(
                    "🎉 Great job, mock interview complete! 🎉",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        Column(modifier = Modifier.fillMaxSize().imePadding().padding(paddingValues)) {
        LazyColumn(
                state = scrollState,
            modifier = Modifier.weight(1f).fillMaxWidth().imePadding().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Resume Analysis History Row
                if (resumeHistory.isNotEmpty()) {
                    item {
                        Text("Resume Analyses:", style = MaterialTheme.typography.titleMedium)
                    }
                    item {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            resumeHistory.forEach { analysis ->
                                Card(Modifier.padding(6.dp).width(240.dp)
                                    .background(if (analysis.jobMatchScore > 75) Color(0xFFE3FCEE) else Color(0xFFFFEBEE))
                                ) {
                                    Column(Modifier.padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("Summary: ${analysis.summary}", maxLines = 2)
                                        Text("Score: ${analysis.jobMatchScore}", fontWeight = FontWeight.Bold,
                                            color = if (analysis.jobMatchScore > 75) Color(0xFF169062) else Color(0xFFD84315))
                                        Text("🕒 ${Instant.fromEpochMilliseconds(analysis.createdAt).toString().substringBefore(".")}")
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Interview Sessions History Row
                if (interviewSessions.isNotEmpty()) {
                    item { Spacer(Modifier.height(8.dp)) }
                    item { Text("Past Interviews:", style = MaterialTheme.typography.titleMedium) }
                    item {
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            interviewSessions.forEach { session ->
                                Card(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .width(160.dp)
                                        .background(Color(0xFFE0F7FA))
                                        .clickable { resumeChatViewModel.loadSessionAnswers(session.id) }
                                ) {
                                    Column(Modifier.padding(8.dp)) {
                                        Text("📅 ${session.sessionDate}", color = Color(0xFF006064))
                                        Text("🗒️ ${session.summary ?: "Session"}", fontSize = MaterialTheme.typography.bodySmall.fontSize)
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Question/Answer display for selected session
                if (sessionAnswers.isNotEmpty()) {
                    item {
                        Row(
                            Modifier
                                .padding(horizontal = 8.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Q&A for Selected Interview:",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF006064)
                            )
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = { resumeChatViewModel.clearSessionAnswers() }
                            ) { Text("Close") }
                        }
                    }
                    items(sessionAnswers) { ans ->
                        Card(Modifier.padding(vertical = 2.dp)) {
                            Column(Modifier.padding(8.dp)) {
                                Text("❓ ${ans.questionId}", fontWeight = FontWeight.Medium)
                                Text("🗨 ${ans.answerText}", color = Color.DarkGray)
                                if (!ans.feedback.isNullOrBlank()) {
                                    Text(
                                        "💡 Feedback: ${ans.feedback}",
                                        color = Color(0xFFFFA000),
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }
                // Standard chat bubbles
                items(messages) { msg -> ChatBubble(msg,
                    onSpeakClick = { text -> ttsHelper.speak(text) }
                ) }
                // ...your inline upload button and loading items here as before...
                val lastMsg = messages.lastOrNull()
                if (lastMsg is ChatMessage.System &&
                    lastMsg.text.contains("upload your resume", ignoreCase = true) && !isLoading
                ) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { showPicker = true },
                                enabled = !isLoading,
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = "Upload Resume")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload Resume PDF")
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(56.dp)) // Height of your input bar, or just more space
                    }
                }
            if (isLoading) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = Color(0xFF1976D2))
                        Spacer(Modifier.width(8.dp))
                        Text("Interview Coach is thinking…", color = Color.Gray, fontStyle = FontStyle.Italic)
                    }
                }
            }

        }

            // This is now ALWAYS fixed to the bottom, above keyboard
            ChatInputBar(
                value = inputText.value,
                enabled = !isLoading,
                onValueChange = { inputText.value = it },
                onSendClick = {
                    val msg = inputText.value.trim()
                    if (msg.isNotEmpty()) {
                        when {
                            messages.lastOrNull() is ChatMessage.System &&
                                    (messages.lastOrNull() as ChatMessage.System).text.contains(
                                        "job role",
                                        ignoreCase = true
                                    ) ->
                                resumeChatViewModel.setJobRole(msg)

                            messages.lastOrNull() is ChatMessage.FileUpload ->
                                resumeChatViewModel.analyzeAndShow()

                            messages.lastOrNull() is ChatMessage.System &&
                                    (messages.lastOrNull() as ChatMessage.System).text.contains(
                                        "mock interview",
                                        ignoreCase = true
                                    ) &&
                                    msg.equals("yes", ignoreCase = true) ->
                                resumeChatViewModel.startAdaptiveInterview()

                            resumeChatViewModel.currentQuestion.value != null ->
                                resumeChatViewModel.submitInterviewAnswer(msg)

                            msg.equals("upload", ignoreCase = true) -> showPicker = true
                        }
                        inputText.value = ""
                    }
                },
                isListening = speechHelper.isListening,
                onListenClick = {
                    if (!speechHelper.isListening) {
                        speechHelper.startListening { recognized ->
                            inputText.value = recognized
                        }
                    } else {
                        speechHelper.stopListening()
                    }
                }
            )
        }
    }
}

// --- Chat UI Elements ---
@Composable
fun ChatBubble(
    message: ChatMessage,
    onSpeakClick: ((String) -> Unit)? = null // Optional TTS callback
) {
    val (bgColor, alignment, content) = when (message) {
        is ChatMessage.User -> Triple(Color(0xFFDCF8C6), Arrangement.End, message.text)
        is ChatMessage.System -> Triple(Color(0xFFECECEC), Arrangement.Start, message.text)
        is ChatMessage.FileUpload -> Triple(Color(0xFFD1EDFF), Arrangement.Start, message.fileName)
        is ChatMessage.InterviewQuestionMsg -> Triple(
            Color(0xFFF3E5F5), Arrangement.Start, "Q${message.index + 1}: ${message.question}"
        )
        is ChatMessage.Followup -> Triple(Color(0xFFF5F5DC), Arrangement.Start, "💡 ${message.text}")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = alignment
    ) {
        Column {
            Text(
                text = content,
                color = Color.Black,
                modifier = Modifier
                    .background(bgColor, shape = MaterialTheme.shapes.medium)
                    .padding(12.dp)
            )
            if (message is ChatMessage.InterviewQuestionMsg && onSpeakClick != null) {
                IconButton(
                    onClick = { onSpeakClick(message.question) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speak again",
                        tint = Color.Gray
                    )
                }
            }
            if (message is ChatMessage.Followup && onSpeakClick != null) {
                IconButton(
                    onClick = { onSpeakClick(message.text) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speak again",
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}


@Composable
fun ChatInputBar(
    value: String,
    enabled: Boolean,
    isListening: Boolean,
    onListenClick: () -> Unit,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = enabled,
            placeholder = {
                Text(if (isListening) "Listening…" else "Type or tap mic…")
            }
        )
        IconButton(
            onClick = onListenClick,
            enabled = enabled
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                contentDescription = if (isListening) "Stop Listening" else "Speak",
                tint = if (isListening) Color.Red else Color.Gray
            )
        }
        Button(
            onClick = onSendClick,
            enabled = value.isNotBlank() && enabled
        ) { Text("Send") }
    }
}

