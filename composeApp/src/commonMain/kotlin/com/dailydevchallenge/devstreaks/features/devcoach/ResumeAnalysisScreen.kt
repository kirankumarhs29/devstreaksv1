package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import com.dailydevchallenge.devstreaks.utils.getPdfPickerHandler

// --- Enhanced ChatMessage Types ---
sealed class ChatMessage {
    data class User(val text: String) : ChatMessage()
    data class System(val text: String) : ChatMessage()
    data class FileUpload(val fileName: String) : ChatMessage()
    data class InterviewQuestionMsg(val question: String, val index: Int) : ChatMessage()
    data class Followup(val text: String) : ChatMessage()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeAndInterviewScreen(
    resumeChatViewModel: ResumeChatViewModel = koinInject()
) {
    val messages by resumeChatViewModel.chatMessages.collectAsState()
    val isLoading by resumeChatViewModel.isLoading.collectAsState()
    val inputText = remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    val scrollState = rememberLazyListState()

    // Ensures welcome message shows ONCE
    LaunchedEffect(Unit) { resumeChatViewModel.startProfileSetup() }
    LaunchedEffect(messages.size) { scrollState.animateScrollToItem(messages.size) }

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
        topBar = { TopAppBar(title = { Text("Resume & Interview Chat") }) },
        floatingActionButton = {
            if (shouldShowFab && !isLoading) {
                FloatingActionButton(onClick = { showPicker = true }) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Upload Resume")
                }
            }
        },
        bottomBar = {
            ChatInputBar(
                value = inputText.value,
                enabled = !isLoading,
                onValueChange = { inputText.value = it },
                onSendClick = {
                    val msg = inputText.value.trim()
                    if (msg.isNotEmpty()) {
                        // Infers next "step" by what's last in chat
                        when {
                            messages.lastOrNull() is ChatMessage.System &&
                                    (messages.lastOrNull() as ChatMessage.System).text.contains("job role", ignoreCase = true) ->
                                resumeChatViewModel.setJobRole(msg)
                            messages.lastOrNull() is ChatMessage.FileUpload ->
                                resumeChatViewModel.analyzeAndShow()
                            messages.lastOrNull() is ChatMessage.System &&
                                    (messages.lastOrNull() as ChatMessage.System).text.contains("mock interview", ignoreCase = true) &&
                                    msg.equals("yes", ignoreCase = true) ->
                                resumeChatViewModel.startAdaptiveInterview()
                            resumeChatViewModel.currentQuestion.value != null ->
                                resumeChatViewModel.submitInterviewAnswer(msg)
                            msg.equals("upload", ignoreCase = true) -> showPicker = true
                            // More steps (skills, but not shown here) can be added similarly
                        }
                        inputText.value = ""
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Standard chat bubbles
            items(messages) { msg -> ChatBubble(msg) }

            // Inline upload button as part of chat conversation (if expected)
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
            }

            // Loading feedback
            if (isLoading) {
                item { ChatBubble(ChatMessage.System("⏳ Processing…")) }
            }
        }
    }
}

// --- Chat UI Elements ---
@Composable
fun ChatBubble(message: ChatMessage) {
    val (bgColor, alignment, content) = when (message) {
        is ChatMessage.User -> Triple(Color(0xFFDCF8C6), Arrangement.End, message.text)
        is ChatMessage.System -> Triple(Color(0xFFECECEC), Arrangement.Start, message.text)
        is ChatMessage.FileUpload -> Triple(Color(0xFFD1EDFF), Arrangement.Start, message.fileName)
        is ChatMessage.InterviewQuestionMsg -> Triple(
            Color(0xFFF3E5F5), Arrangement.Start,
            "Q${message.index + 1}: ${message.question}"
        )
        is ChatMessage.Followup -> Triple(Color(0xFFF5F5DC), Arrangement.Start, "💡 ${message.text}")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = alignment
    ) {
        Text(
            text = content,
            color = Color.Black,
            modifier = Modifier
                .background(bgColor, shape = MaterialTheme.shapes.medium)
                .padding(12.dp)
        )
    }
}

@Composable
fun ChatInputBar(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("Type your message…") },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(6.dp))
            Button(
                onClick = onSendClick,
                enabled = value.isNotBlank() && enabled
            ) { Text("Send") }
        }
    }
}
