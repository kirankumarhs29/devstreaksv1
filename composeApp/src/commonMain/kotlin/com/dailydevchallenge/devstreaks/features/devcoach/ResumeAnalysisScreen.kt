package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dailydevchallenge.database.InterviewSession
import com.dailydevchallenge.database.UserAnswer
import com.dailydevchallenge.devstreaks.features.dailyCoach.DevCoachLottieAvatar
import com.dailydevchallenge.devstreaks.llm.ChatUIMessage
import com.dailydevchallenge.devstreaks.model.ResumeAnalysis
import com.dailydevchallenge.devstreaks.tts.TTSHelper
import com.dailydevchallenge.devstreaks.utils.getPdfPickerHandler
import com.mohamedrejeb.calf.core.LocalPlatformContext
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import org.koin.compose.koinInject
import kotlinx.datetime.Instant
fun formatTime(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val timeString = instant.toString()
        timeString.substringAfter("T").substringBefore(".")
    } catch (e: Exception) {
        "Unknown"
    }
}

fun formatDate(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        instant.toString().substringBefore("T")
    } catch (e: Exception) {
        "Unknown Date"
    }
}

// --- Enhanced ChatMessage Types ---
data class InterviewScreenState(
    val currentMode: InterviewMode = InterviewMode.RESUME_UPLOAD,
    val isRecording: Boolean = false,
    val audioPermissionGranted: Boolean = false,
    val currentSession: InterviewSession? = null,
    val analysisResults: ResumeAnalysis? = null,
    val chatMessages: List<ChatUIMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val uploadProgress: Float = 0f,
    val isAnalyzing: Boolean = false,
    val targetJobRole: String = "", // Add this
    val showRoleInput: Boolean = false ,// Add this
    val isTTSEnabled: Boolean = true, // Add this
    val hasCompletedInterview: Boolean = false,
    val streakCount: Int = 0,
    val showCelebration: Boolean = false,
    val lastInterviewScore: Int = 0,
    val jobMatchScore:Long = 0
)
enum class InterviewMode {
    RESUME_UPLOAD,
    RESUME_ANALYSIS,
    ROLE_INPUT, // Add new mode
    INTERVIEW_PREP,
    LIVE_INTERVIEW,
    INTERVIEW_COMPLETE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeAndInterviewScreen(
    resumeChatViewModel: ResumeChatViewModel = koinInject(),
    navController: NavController
) {
    val uiState by resumeChatViewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        resumeChatViewModel.loadLastOrPromptForResume()
        resumeChatViewModel.loadHistory()
    }
    Column(modifier = Modifier.fillMaxSize()) {
        // Top navigation bar
        TopAppBar(
            title = {
                Text(
                    when (uiState.currentMode) {
                        InterviewMode.RESUME_UPLOAD -> "Upload Resume"
                        InterviewMode.RESUME_ANALYSIS -> "Analysis Results"
                        InterviewMode.ROLE_INPUT -> "Target Role"
                        InterviewMode.INTERVIEW_PREP -> "Interview Prep"
                        InterviewMode.LIVE_INTERVIEW -> "Live Interview"
                        InterviewMode.INTERVIEW_COMPLETE -> "Interview Complete"
                    }
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        // Main content with smooth transitions
        Box(modifier = Modifier.weight(1f)) {
            AnimatedModeTransition(
                currentMode = uiState.currentMode,
                modifier = Modifier.fillMaxSize()
            ) {
                when (uiState.currentMode) {
                    InterviewMode.RESUME_UPLOAD -> ResumeUploadSection(
                        viewModel = resumeChatViewModel,
                        state = uiState
                    )

                    InterviewMode.RESUME_ANALYSIS -> ResumeAnalysisSection(
                        viewModel = resumeChatViewModel,
                        state = uiState
                    )

                    InterviewMode.ROLE_INPUT -> RoleInputSection( // Add this
                        viewModel = resumeChatViewModel,
                        state = uiState
                    )

                    InterviewMode.INTERVIEW_PREP -> InterviewPrepSection(
                        viewModel = resumeChatViewModel,
                        state = uiState
                    )

                    InterviewMode.LIVE_INTERVIEW -> LiveInterviewSection(
                        viewModel = resumeChatViewModel,
                        state = uiState
                    )
                    InterviewMode.INTERVIEW_COMPLETE -> {
                        // Show completion screen
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Interview Completed! 🎉",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Your performance score: ${uiState.lastInterviewScore}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            LinearProgressIndicator(
                                progress = {
                                    (uiState.lastInterviewScore.coerceIn(0, 100) / 100f)
                                },
                            )

                            Spacer(Modifier.height(24.dp))
                                InterviewStartCard(
                                    onStartInterview = { resumeChatViewModel.startInterview() }
                                )
                        }
                    }
                }
            }
            CelebrationOverlay(
                show = uiState.showCelebration,
                score = uiState.lastInterviewScore,
                onDismiss = { resumeChatViewModel.dismissCelebration() }
            )
        }

        // Error handling
        uiState.errorMessage?.let { error ->
            ErrorStateCard(
                error = error,
                onRetry = { resumeChatViewModel.clearError() },
                onDismiss = { resumeChatViewModel.clearError() }
            )
        }
    }
}
@Composable
fun AnimatedModeTransition(
    currentMode: InterviewMode,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedContent(
        targetState = currentMode,
        modifier = modifier,
        transitionSpec = {
            (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
        }
    ) {
        content()
    }
}
@Composable
fun ResumeUploadSection(
    viewModel: ResumeChatViewModel,
    state: InterviewScreenState
) {
    val showFilePicker by viewModel.showFilePicker.collectAsState()
    if (showFilePicker) {
        getPdfPickerHandler().PickPdf { resumeText ->
            viewModel.onResumeSelected(resumeText)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "Upload Your Resume",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "Upload your resume to get personalized analysis and interview preparation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                Spacer(Modifier.height(24.dp))

                if (state.isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Analyzing your resume...")
                        if (state.uploadProgress > 0) {
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { state.uploadProgress },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.onResumePickerCancelled() }
                        ) {
                            Text("Cancel")
                        }
                    }
                } else {
                    Button(
                        onClick = { viewModel.uploadResume() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Choose Resume File")
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Supported formats: PDF, DOC, DOCX",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
@Composable
fun ResumeAnalysisSection(
    viewModel: ResumeChatViewModel,
    state: InterviewScreenState
) {
    val resumeHistory by viewModel.resumeHistory.collectAsState()
    val interviewSessions by viewModel.interviewSessions.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnalysisOverviewCard(state.analysisResults)
        }
        if (resumeHistory.isNotEmpty()) {
            item {
                Text(
                    "Previous Resume Analyses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(resumeHistory) { analysis ->
                        ResumeHistoryCard(
                            analysis = analysis,
                            onClick = { viewModel.latestResumeAnalysis }
                        )
                    }
                }
            }
        }

        // Interview Sessions History - MISSING FEATURE
        if (interviewSessions.isNotEmpty()) {
            item {
                Text(
                    "Past Interview Sessions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(interviewSessions) { session ->
                        InterviewSessionCard(
                            session = session,
                            onClick = { viewModel.loadSessionAnswers(session.id) }
                        )
                    }
                }
            }
        }

        state.analysisResults?.let { analysis ->
            item {
                StrengthsCard(analysis.skillsMatched)
            }

            item {
                WeaknessesCard(analysis.skillsMissing)
            }
            item {
                RecommendationsCard(analysis.recommendations)
            }
        }

        item {
            InterviewStartCard(
                onStartInterview = { viewModel.startInterview() }
            )
        }
    }
}
@Composable
fun ResumeHistoryCard(
    analysis: ResumeAnalysis,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (analysis.jobMatchScore > 75)
                Color(0xFFE8F5E8) else Color(0xFFFFF3E0)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Score: ${analysis.jobMatchScore}%",
                    fontWeight = FontWeight.Bold,
                    color = if (analysis.jobMatchScore > 75) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
                Text(
                    formatDate(analysis.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                analysis.summary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun InterviewSessionCard(
    session: InterviewSession,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Interview Session",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                formatDate(session.sessionDate),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                session.summary ?: "Practice Session",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AnalysisOverviewCard(analysis: ResumeAnalysis?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Resume Analysis",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Overall Assessment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                analysis?.jobMatchScore?.let { score ->
                    ScoreIndicator(score = score)
                }
            }

            Spacer(Modifier.height(16.dp))

            analysis?.summary?.let { summary ->
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                )
            }
        }
    }
}

@Composable
fun ScoreIndicator(score: Long) {
    val color = when {
        score >= 80 -> MaterialTheme.colorScheme.primary
        score >= 60 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Text(
            "${score}%",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun StrengthsCard(strengths: List<String>?) {
    InfoCard(
        title = "Your Strengths",
        icon = Icons.Default.CheckCircle,
        iconColor = MaterialTheme.colorScheme.primary,
        items = strengths ?: emptyList()
    )
}

@Composable
fun WeaknessesCard(weaknesses: List<String>?) {
    InfoCard(
        title = "Areas for Improvement",
        icon = Icons.Default.Warning,
        iconColor = MaterialTheme.colorScheme.error,
        items = weaknesses ?: emptyList()
    )
}

@Composable
fun SkillsGapCard(skillsGap: List<String>?) {
    InfoCard(
        title = "Skills to Develop",
        icon = Icons.Default.TrendingUp,
        iconColor = MaterialTheme.colorScheme.secondary,
        items = skillsGap ?: emptyList()
    )
}

@Composable
fun RecommendationsCard(recommendations: String) {
    InfoCard(
        title = "Recommendations",
        icon = Icons.Default.Lightbulb,
        iconColor = MaterialTheme.colorScheme.tertiary,
        items = recommendations.split("\n").filter { it.isNotBlank() }
    )
}

@Composable
fun InfoCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    items: List<String>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            items.forEach { item ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        "•",
                        modifier = Modifier.padding(end = 8.dp),
                        color = iconColor
                    )
                    Text(
                        item,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun InterviewStartCard(onStartInterview: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Ready for Interview Practice?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Start your personalized mock interview based on your resume analysis",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onStartInterview,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Interview Practice")
            }
        }
    }
}
@Composable
fun LiveInterviewSection(
    viewModel: ResumeChatViewModel,
    state: InterviewScreenState
) {
    var hasInitialized by remember { mutableStateOf(false) }
    val context = LocalPlatformContext.current
    val ttsHelper = remember { TTSHelper(context) }
    var lastSpokenMessageCount by remember { mutableStateOf(0) }
    LaunchedEffect(state.chatMessages.size) {
        if (state.chatMessages.size > lastSpokenMessageCount) {
            val lastMessage = state.chatMessages.lastOrNull()
            if (lastMessage is ChatUIMessage.Received &&
                lastMessage.text.contains("?")) {
                ttsHelper.speak(lastMessage.text)
                lastSpokenMessageCount = state.chatMessages.size
            }
        }
    }
    // Clean up TTS when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Progress indicator
        state.currentSession?.let {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Interview Progress",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Question ${state.chatMessages.count { it is ChatUIMessage.Received } + 1}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress =  { 0.5f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Chat messages
        ChatMessagesSection(
            messages = state.chatMessages,
            modifier = Modifier.weight(1f)
        )

        // Enhanced input section
        InterviewInputSection(
            isRecording = state.isRecording,
            onStartRecording = { viewModel.startRecording() },
            onStopRecording = { viewModel.stopRecording() },
            onTextInput = { viewModel.sendMessage(it) }
        )
    }
}

@Composable
fun ChatMessagesSection(
    messages: List<ChatUIMessage>,
    modifier: Modifier = Modifier
) {
    val context = LocalPlatformContext.current
    val ttsHelper = remember { TTSHelper(context) }
    DisposableEffect(ttsHelper) {
        onDispose {
            ttsHelper.shutdown()
        }
    }
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = true
    ) {
        items(messages.reversed()) { message ->
            ChatBubble(message = message, onSpeakClick = { text -> ttsHelper.speak(text) })
        }
    }
}

@Composable
fun ChatBubble(message: ChatUIMessage , onSpeakClick: (String) -> Unit = {}) {
    val isUser = message is ChatUIMessage.Sent
    val messageText = when (message) {
        is ChatUIMessage.Sent -> message.text
        is ChatUIMessage.Received -> message.text
    }
    val timestamp = when (message) {
        is ChatUIMessage.Sent -> message.timestamp
        is ChatUIMessage.Received -> message.timestamp
    }

    Row(
        modifier = Modifier.fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
       if (!isUser) {
            Card(
                modifier = Modifier.size(40.dp),
                shape = CircleShape
            ) {
                DevCoachLottieAvatar()
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Card(
                modifier = Modifier.widthIn(max = 280.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isUser -> Color(0xFFDCF8C6)
                        messageText.contains("?") -> Color(0xFFF3E5F5) // Questions
                        messageText.contains("feedback", ignoreCase = true) -> Color(0xFFFFF3E0) //
                        // Feedback
                        else -> Color(0xFFECECEC) // General AI responses
                    }

                ),
                shape = RoundedCornerShape(
                    topStart = if (isUser) 18.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 18.dp,
                    bottomStart = 18.dp,
                    bottomEnd = 18.dp
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = messageText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )

                    // Timestamp
                    Text(
                        text = formatTime(timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.align(if (isUser) Alignment.End else Alignment.Start)
                    )
                }
            }

            // TTS button for AI messages
            if (!isUser && message is ChatUIMessage.Received) {
                IconButton(
                    onClick = { onSpeakClick(messageText) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Read aloud",
                        tint = Color(0xFF2196F3),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (isUser) {
            Spacer(Modifier.width(8.dp))
            // User Avatar
            Card(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun InterviewInputSection(
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onTextInput: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice input button with visual feedback
                FloatingActionButton(
                    onClick = if (isRecording) onStopRecording else onStartRecording,
                    containerColor = if (isRecording)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "Stop recording" else "Start recording"
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Text input field
                var textInput by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Type your answer or use voice...") },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (textInput.isNotBlank()) {
                                    onTextInput(textInput)
                                    textInput = ""
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                )
            }

            if (isRecording) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Recording... Speak clearly into your microphone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun InterviewPrepSection(
    viewModel: ResumeChatViewModel,
    state: InterviewScreenState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        if (state.hasCompletedInterview) "Interview Completed! 🎉" else "Interview Preparation",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.hasCompletedInterview)
                            "Great job! You can start another practice session or review your performance."
                        else
                            "Based on your resume analysis, here are some topics you should focus on",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            PrepTopicCard(
                title = "Technical Skills",
                description = "Practice questions related to your technical expertise",
                icon = Icons.Default.Code,
                onClick = { viewModel.startTechnicalPrep() }
            )
        }

        item {
            PrepTopicCard(
                title = "Behavioral Questions",
                description = "Common behavioral interview questions and frameworks",
                icon = Icons.Default.Psychology,
                onClick = { viewModel.startBehavioralPrep() }
            )
        }

        item {
            PrepTopicCard(
                title = "Areas for Improvement",
                description = "Focus on your identified weak areas",
                icon = Icons.Default.TrendingUp,
                onClick = { viewModel.startWeaknessPrep() }
            )
        }

        item {
            Button(
                onClick = { viewModel.startFullInterview() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.hasCompletedInterview) "Start Another Interview" else "Start Full Mock Interview")
            }
        }
    }
}

@Composable
fun PrepTopicCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun LoadingStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ErrorStateCard(
    error: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Something went wrong",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Try Again")
            }
        }
    }
}
@Composable
fun RoleInputSection(
    viewModel: ResumeChatViewModel,
    state: InterviewScreenState
) {
    var roleText by remember { mutableStateOf(state.targetJobRole) }
    var showSuggestions by remember { mutableStateOf(false) }

    val commonRoles = listOf(
        "Software Engineer",
        "Frontend Developer",
        "Backend Developer",
        "Full Stack Developer",
        "Mobile Developer",
        "Data Scientist",
        "Product Manager",
        "UI/UX Designer",
        "DevOps Engineer",
        "QA Engineer"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Work,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    "What role are you interviewing for?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "This helps us tailor interview questions to your target position",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = roleText,
                    onValueChange = { roleText = it },
                    label = { Text("Job Role") },
                    placeholder = { Text("e.g., Software Engineer") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showSuggestions = !showSuggestions }) {
                            Icon(
                                if (showSuggestions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Show suggestions"
                            )
                        }
                    }
                )

                // Role suggestions
                if (showSuggestions) {
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(commonRoles.filter {
                            it.contains(roleText, ignoreCase = true) || roleText.isBlank()
                        }) { role ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable {
                                        roleText = role
                                        showSuggestions = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Text(
                                    role,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel._uiState.update {
                                it.copy(currentMode = InterviewMode.RESUME_ANALYSIS)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }

                    Button(
                        onClick = {
                            viewModel.onRoleSubmitted(roleText)
                        },
                        enabled = roleText.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}

@Composable
fun InterviewControlBar(
    isTTSEnabled: Boolean,
    onTTSToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Auto-read questions",
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = isTTSEnabled,
                onCheckedChange = onTTSToggle
            )
        }
    }
}
@Composable
fun QAReviewSection(
    viewModel: ResumeChatViewModel,
    state: InterviewScreenState
) {
    val sessionAnswers by viewModel.sessionAnswers.collectAsState()

    if (sessionAnswers.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Interview Q&A Review",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.clearSessionAnswers() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }

            items(sessionAnswers) { answer ->
                QAReviewCard(answer = answer)
            }
        }
    }
}

@Composable
fun QAReviewCard(answer: UserAnswer) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Question
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    answer.questionId, // You may need to store actual question text
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2196F3)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Answer
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    answer.answerText,
                    color = Color.DarkGray
                )
            }

            // Feedback if available
            if (!answer.feedback.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        answer.feedback,
                        color = Color(0xFFFF9800),
                        fontStyle = FontStyle.Italic
                    )
                }
            }

            // Score if available
            answer.score?.let { score ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Score: ")
                    LinearProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.width(100.dp),
                        color = when {
                            score >= 80 -> Color(0xFF4CAF50)
                            score >= 60 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                    Text(" ${score}%")
                }
            }
        }
    }
}
@Composable
fun CelebrationOverlay(
    show: Boolean,
    score: Int,
    onDismiss: () -> Unit
) {
    if (show) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50))
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🎉 Interview Complete! 🎉",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Great job! Your score: ${score}%",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("Continue", color = Color(0xFF4CAF50))
                    }
                }
            }
        }
    }
}

@Composable
fun StreakBanner(streakCount: Int) {
    if (streakCount > 1) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🔥", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Interview streak: $streakCount days!",
                    color = Color(0xFFFF9800),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
