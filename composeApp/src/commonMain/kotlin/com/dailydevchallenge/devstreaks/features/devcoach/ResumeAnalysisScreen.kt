package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dailydevchallenge.devstreaks.tts.SpeechToTextHelper
import com.dailydevchallenge.devstreaks.tts.TTSHelper
import org.koin.compose.koinInject
import com.dailydevchallenge.devstreaks.utils.getPdfPickerHandler
import com.mohamedrejeb.calf.core.LocalPlatformContext

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
    val speechHelper: SpeechToTextHelper = koinInject()
    val context = LocalPlatformContext.current
    val ttsHelper = remember { TTSHelper(context) }

    // Ensures welcome message shows ONCE
    LaunchedEffect(Unit) { resumeChatViewModel.startProfileSetup() }
    LaunchedEffect(messages.size) { scrollState.animateScrollToItem(messages.size) }
    DisposableEffect(Unit) { onDispose { ttsHelper.shutdown() } }
    val lastMsg = messages.lastOrNull()

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
        topBar = { TopAppBar(title = { Text("Resume & Interview Chat") }) },
        floatingActionButton = {
            if (shouldShowFab && !isLoading) {
                FloatingActionButton(onClick = { showPicker = true }) {
                    Icon(Icons.Default.UploadFile, contentDescription = "Upload Resume")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding() // This will ensure both chat and input move with keyboard
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
                    item { ChatBubble(ChatMessage.System("⏳ Processing…")) }
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

