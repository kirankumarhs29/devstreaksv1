package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.database.InterviewSession
import com.dailydevchallenge.database.UserAnswer
import com.dailydevchallenge.devstreaks.database.toModel
import com.dailydevchallenge.devstreaks.llm.ChatUIMessage
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.model.InterviewQuestion
import com.dailydevchallenge.devstreaks.model.InterviewSessionContext
import com.dailydevchallenge.devstreaks.model.ResumeAnalysis
import com.dailydevchallenge.devstreaks.repository.InterviewRepository
import com.dailydevchallenge.devstreaks.repository.ResumeAnalysisRepository
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.utils.generateUUID
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

sealed class ChatMessage {
    data class User(val content: String) : ChatMessage()
    data class System(val content: String) : ChatMessage()
    data class InterviewQuestionMsg(val question: String, val questionIndex: Int) : ChatMessage()
    data class Followup(val feedback: String) : ChatMessage()
    data class FileUpload(val content: String) : ChatMessage()
}

class ResumeChatViewModel(
    private val llmService: LLMService,
    private val resumeRepo: ResumeAnalysisRepository,
    private val interviewRepo: InterviewRepository
) : ViewModel() {
    private val logger = getLogger()

    private val _resumeText = MutableStateFlow("")
    val resumeText: StateFlow<String> get() = _resumeText

    private val _analysis = MutableStateFlow<ResumeAnalysis?>(null)

    private val _skills = MutableStateFlow<List<String>>(emptyList())
    val skills: StateFlow<List<String>> get() = _skills

    // --- Chat / Coaching / Adaptive Interview ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())

    private val _answerHistory = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val answerHistory: StateFlow<List<Pair<String, String>>> get() = _answerHistory

    private val _currentQuestion = MutableStateFlow<InterviewQuestion?>(null)
    val currentQuestion: StateFlow<InterviewQuestion?> get() = _currentQuestion
    // Track the current session and resume analysis IDs
    private var currentSessionId: String? = null
    private var currentResumeAnalysisId: String? = null

    // ------- Added for showing history ---------
    private val _resumeHistory = MutableStateFlow<List<ResumeAnalysis>>(emptyList())
    val resumeHistory: StateFlow<List<ResumeAnalysis>> get() = _resumeHistory

    private val _interviewSessions = MutableStateFlow<List<InterviewSession>>(emptyList())
    val interviewSessions: StateFlow<List<InterviewSession>> get() = _interviewSessions

    private val _sessionAnswers = MutableStateFlow<List<UserAnswer>>(emptyList())
    val sessionAnswers: StateFlow<List<UserAnswer>> = _sessionAnswers
    val _uiState = MutableStateFlow(InterviewScreenState())
    val uiState: StateFlow<InterviewScreenState> = _uiState
    private val _showFilePicker = MutableStateFlow(false)
    val showFilePicker: StateFlow<Boolean> = _showFilePicker
    private var isInitializingInterview = false

    fun dismissCelebration() {
        logger.d("Celebration overlay dismissed")
        _uiState.update { it.copy(showCelebration = false) }
    }

    private fun setTargetRole(role: String) {
        logger.d("Target role set: $role")
        _uiState.update { it.copy(targetJobRole = role.trim()) }
    }
    private fun setLoading(loading: Boolean) {
        logger.d("Loading state changed: $loading")
        _uiState.update { it.copy(isLoading = loading) }
    }

    private fun showRoleInput() {
        logger.d("Show role input triggered")
        _uiState.update {
            it.copy(
                currentMode = InterviewMode.ROLE_INPUT,
                showRoleInput = true
            )
        }
    }

    fun onRoleSubmitted(role: String) {
        logger.d("Role submitted: $role")
        if (role.isBlank()) {
            logger.d("Role submission failed: blank role")
            _uiState.update { it.copy(errorMessage = "Please enter a valid job role") }
            return
        }

        setTargetRole(role)
        _uiState.update {
            it.copy(
                showRoleInput = false,
                currentMode = InterviewMode.RESUME_ANALYSIS,
                isLoading = true
            )
        }
        analyzeAndShow()
    }
    private fun determineCurrentMode(
        analysis: ResumeAnalysis?,
        currentQuestion: InterviewQuestion?
    ): InterviewMode {
        return when {
            _resumeText.value.isBlank() -> InterviewMode.RESUME_UPLOAD
            _uiState.value.targetJobRole.isBlank() -> InterviewMode.ROLE_INPUT
            analysis == null -> InterviewMode.RESUME_ANALYSIS
            _currentQuestion.value != null -> InterviewMode.LIVE_INTERVIEW
            _uiState.value.hasCompletedInterview -> InterviewMode.INTERVIEW_COMPLETE
            else -> InterviewMode.INTERVIEW_PREP
        }
    }
    fun uploadResume() {
        logger.d("Upload resume triggered")
        _uiState.update { it.copy(isLoading = true) }
        _showFilePicker.value = true
    }
    fun onResumeSelected(resumeText: String) {
        logger.d("Resume selected")
        _showFilePicker.value = false
        setResumeText(resumeText)
        _uiState.update { it.copy(isLoading = true) }
        moveToRoleInput()
    }

    fun onBackFromRoleInput() {
        setTargetRole("")
        _analysis.value = null
        _uiState.update { it.copy(currentMode = InterviewMode.RESUME_ANALYSIS) }
    }

    fun onResumePickerCancelled() {
        logger.d("Resume picker cancelled")
        _showFilePicker.value = false
        _uiState.update { it.copy(isLoading = false) }
    }

    fun startInterview() {
        logger.d("Start interview triggered")
        val analysis = _analysis.value
        val targetRole = _uiState.value.targetJobRole

        // Validate required fields
        when {
            analysis == null -> {
                logger.d("Start interview failed: analysis is null")
                _uiState.update { it.copy(errorMessage = "Please upload and analyze your resume first") }
                return
            }
            targetRole.isBlank() -> {
                logger.d("Start interview failed: target role is blank")
                showRoleInput()
                return
            }
            analysis.summary.isBlank() -> {
                logger.d("Start interview failed: analysis summary is blank")
                _uiState.update { it.copy(errorMessage = "Resume analysis incomplete. Please try uploading again.") }
                return
            }
        }
        isInitializingInterview = true
        _uiState.update { it.copy(currentMode = InterviewMode.INTERVIEW_PREP) }
        startTechnicalPrep()
    }

    fun startTechnicalPrep() {
        logger.d("Start technical prep triggered")
        _uiState.update { it.copy(currentMode = InterviewMode.INTERVIEW_PREP) }
        // Start technical questions
//        startAdaptiveInterview()
    }

    fun startBehavioralPrep() {
        logger.d("Start behavioral prep triggered")
        _uiState.update { it.copy(currentMode = InterviewMode.LIVE_INTERVIEW) }
        // Start behavioral questions
        startAdaptiveInterview()
    }

    fun startWeaknessPrep() {
        logger.d("Start weakness prep triggered")
        _uiState.update { it.copy(currentMode = InterviewMode.LIVE_INTERVIEW) }
        // Focus on weak areas
        startAdaptiveInterview()
    }

    fun startFullInterview() {
        logger.d("Start full interview triggered")
        _uiState.update { it.copy(currentMode = InterviewMode.LIVE_INTERVIEW) }
        startAdaptiveInterview()
    }

    fun startRecording() {
        logger.d("Start recording triggered")
        _uiState.update { it.copy(isRecording = true) }
        // Implement STT functionality
    }

    fun stopRecording() {
        logger.d("Stop recording triggered")
        _uiState.update { it.copy(isRecording = false) }
        // Stop STT and process result
    }

     fun sendMessage(message: String) {
        logger.d("Send message: $message")
         viewModelScope.launch {
             submitInterviewAnswer(message)
         }
    }

    fun clearError() {
        logger.d("Clear error triggered")
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun setResumeText(text: String) {
        logger.d("Resume text set")
        _resumeText.value = text
        _chatMessages.update { it + ChatMessage.FileUpload("📄 Resume uploaded") }

    }
    fun loadHistory() {
        logger.d("Load history triggered")
        viewModelScope.launch {
            val userId = UserPreferences.getSafeUserId()
            _resumeHistory.value = resumeRepo.getAllForUser(userId)
                .map { it.toModel() }
            _interviewSessions.value = interviewRepo.getSessionsForUser(userId)
        }
    }
    fun loadSessionAnswers(sessionId: String) {
        logger.d("Load session answers for sessionId: $sessionId")
        viewModelScope.launch {
            _sessionAnswers.value = interviewRepo.getAnswersForSession(sessionId)
        }
    }
    val latestResumeAnalysis: ResumeAnalysis?
        get() = resumeHistory.value.firstOrNull()


    // Call this when the screen starts
    fun loadLastOrPromptForResume() {
        logger.d("Load last or prompt for resume triggered")
        viewModelScope.launch {
            val userId = UserPreferences.getSafeUserId()
            val lastAnalysis = resumeRepo.getAllForUser(userId).firstOrNull()
            if (lastAnalysis != null) {
                logger.d("Last resume analysis found, loading analysis")
                _analysis.value = lastAnalysis.toModel()
                // Update chat messages/UI to offer direct access to interview or to upload again
            } else {
                logger.d("No resume analysis found, prompting for upload")
                // No analysis found, prompt for resume upload
                _chatMessages.value = listOf(
                    ChatMessage.System("👋 Hi! Let’s get you interview-ready."),
                    ChatMessage.System("Please upload your resume (PDF only).")
                )
            }
        }
    }

    fun startProfileSetup(greeting: Boolean = true) {
        logger.d("Start profile setup triggered")
        _chatMessages.value = emptyList()
        if (greeting) {
            _chatMessages.value = listOf(
                ChatMessage.System("👋 Hi! Let's get you interview-ready."),
                ChatMessage.System("What job role are you targeting?")
            )
        }
        _uiState.update { it.copy(targetJobRole = "") }
        _resumeText.value = ""
        _analysis.value = null
        _skills.value = emptyList()
        _answerHistory.value = emptyList()
        _currentQuestion.value = null
    }

    fun setJobRole(role: String) {
        logger.d("Set job role: $role")
        _uiState.update { it.copy(targetJobRole = role) }
        _chatMessages.update {
            it + ChatMessage.User(role) +
                    ChatMessage.System("Please upload your resume (PDF only).")
        }
    }

    fun setSkills(skills: List<String>) {
        logger.d("Set skills: $skills")
        _skills.value = skills
    }
    // Add to ViewModel
    fun toggleTTS(enabled: Boolean) {
        logger.d("TTS toggled: $enabled")
        _uiState.update { it.copy(isTTSEnabled = enabled) }
    }
    fun moveToRoleInput() {
        logger.d("Move to role input triggered")
        _uiState.update { it.copy(currentMode = InterviewMode.ROLE_INPUT) }
    }

    init {
        viewModelScope.launch {
            combine(
                _analysis,
                _chatMessages,
                _currentQuestion
            ) { analysis, messages, _ ->
                _uiState.value.copy(
                    analysisResults = analysis,
                    chatMessages = messages.map { it.toChatUIMessage() },
                    currentMode = determineCurrentMode(analysis, _currentQuestion.value),
                )

            }.collectLatest { newState ->
                _uiState.update { newState }
            }
        }
    }

    private fun analyzeAndShow() {
        val resume = _resumeText.value
        val role = _uiState.value.targetJobRole
        if (resume.isBlank()) return
        _uiState.update { it.copy(isLoading = true) }
        val analysisId = generateUUID()
        viewModelScope.launch {
            try {
                val result = llmService.analyzeResume(resume, role)
                _analysis.value = result
                _uiState.update {
                    it.copy(isLoading = false, currentMode = InterviewMode.RESUME_ANALYSIS)
                }
                // update job match score
                _uiState.update { it.copy(jobMatchScore = result.jobMatchScore) }
                // Save analysis to DB
                resumeRepo.saveResumeAnalysis(
                    id = analysisId,
                    userId = UserPreferences.getSafeUserId(),
                    resumeText = resume,
                    summary = result.summary,
                    skillsParsed = result.skillsMatched.joinToString(","),
                    skillsGaps = result.skillsMissing.joinToString(","),
                    recommendations = result.recommendations,
                    jobMatchScore = result.jobMatchScore,
                    createdAt = Clock.System.now().toEpochMilliseconds()
                )
//                _chatMessages.update { list ->
//                    list + listOf(
//                        ChatMessage.System("📝 Resume analysis for '$role':"),
//                        ChatMessage.System("🧠 ${result.summary}"),
//                        ChatMessage.System("✅ Strengths: ${result.skillsMatched.joinToString()}"),
//                        ChatMessage.System("⚠️ Weaknesses: ${result.skillsMissing.joinToString()}"),
//                        ChatMessage.System("🏆 Role Fit Score: ${result.jobMatchScore}"),
//                        ChatMessage.System("📚 Recommendations: ${result.recommendations}"),
//                    )
//                }
            } catch (e: Exception) {
                logger.d("Resume analysis failed: ${e.message}")
                _chatMessages.update { it + ChatMessage.System("❌ Couldn’t analyze your resume. Try again.") }
            } finally {
                setLoading(false)
            }
        }
    }

    private fun startAdaptiveInterview() {
        logger.d("Start adaptive interview triggered")
        val targetRole = _uiState.value.targetJobRole
        val analysis = _analysis.value
        val summary = _analysis.value?.summary ?: ""
        val skills = _analysis.value?.skillsMatched ?: emptyList()
        when {
            targetRole.isBlank() -> {
                logger.d("Adaptive interview failed: job role is blank")
                _uiState.update { it.copy(errorMessage = "Job role is required") }
                return
            }
            summary.isBlank() -> {
                logger.d("Adaptive interview failed: summary is blank")
                _uiState.update { it.copy(errorMessage = "Resume summary is missing") }
                return
            }
            skills.isEmpty() -> {
                logger.d("Adaptive interview failed: no skills found")
                _uiState.update { it.copy(errorMessage = "No skills found in resume") }
                return
            }
        }
        val resumeAnalysisId = analysis?.id ?: run {
            logger.d("Adaptive interview failed: resume analysis not found")
            _uiState.update { it.copy(errorMessage = "Resume analysis not found") }
            return
        }
        val sessionId = generateUUID()
        currentSessionId = sessionId
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                logger.d("Creating interview session in DB")
                interviewRepo.createSession(
                    userId = UserPreferences.getSafeUserId(),
                    resumeAnalysisId = resumeAnalysisId,
                    summary = summary,
                    sessionDate = Clock.System.now().toEpochMilliseconds()
                )
                val result = llmService.startInterviewSession(targetRole, summary, skills)
                result.question?.let { q ->
                    val questionId = generateUUID()
                    interviewRepo.saveQuestion(
                        id = questionId,
                        questionText = q.question,
                        topic = q.topic,
                        difficulty = q.difficulty,
                        followUp = q.followUp,
                        lastAskedAt = Clock.System.now().toEpochMilliseconds(),
                        timesAsked = 1
                    )
                    logger.d("First interview question saved and shown: ${q.question}")
                    _chatMessages.update { it + ChatMessage.InterviewQuestionMsg(q.question, 0) }
                    _currentQuestion.value = q
                    _answerHistory.value = emptyList()
                }?: run {
                    logger.d("Failed to get interview question from LLM")
                    _uiState.update { it.copy(errorMessage = "Failed to get interview question") }
                }
            } catch (e: Exception) {
                logger.d("Interview start failed: ${e.message}")
                _uiState.update { it.copy(errorMessage = "Interview start failed: ${e.message}") }
            } finally {
                setLoading(false)
                isInitializingInterview = false
            }
        }
    }

    private suspend fun submitInterviewAnswer(answer: String) {
        logger.d("Submit interview answer: $answer")
        val currentQ = _currentQuestion.value ?: return
        val role =  _uiState.value.targetJobRole
        val summary = _analysis.value?.summary ?: ""
        val skills = _analysis.value?.skillsMatched ?: emptyList()
        val sessionId = currentSessionId ?: return
        val avgScore = calculateAverageScore(sessionId)
        _uiState.update { it.copy(lastInterviewScore = avgScore) }
        val updatedHistory = _answerHistory.value + (currentQ.question to answer)
        setLoading(true)
        viewModelScope.launch {
            try {
                val ctx = InterviewSessionContext(role, summary, skills, updatedHistory)
                val stepResult = llmService.submitInterviewAnswer(answer, currentQ, ctx)
                logger.d("Interview answer submitted, feedback: ${stepResult.feedback}, score: ${stepResult.score}")
                interviewRepo.saveUserAnswer(
                    questionId = currentQ.id,
                    sessionId = sessionId,
                    answerText = answer,
                    feedback = stepResult.feedback,
                    score = stepResult.score,
                    topic = currentQ.topic,
                    timestamp = Clock.System.now().toEpochMilliseconds()
                )
                if (stepResult.feedback != null) {
                    interviewRepo.updateUserAnswerFeedback(
                        questionId = currentQ.id,
                        sessionId = sessionId,
                        feedback = stepResult.feedback
                    )
                }
                if (stepResult.question != null) {
                    interviewRepo.saveQuestion(
                        id = stepResult.question.id,
                        questionText = stepResult.question.question,
                        topic = stepResult.question.topic,
                        difficulty = stepResult.question.difficulty,
                        followUp = stepResult.question.followUp,
                        lastAskedAt = Clock.System.now().toEpochMilliseconds(),
                        timesAsked = 1
                    )
                }
                _chatMessages.update {
                    it + ChatMessage.User(answer) +
                            listOfNotNull(stepResult.feedback?.let { f -> ChatMessage.Followup(f) }) +
                            listOfNotNull(stepResult.question?.let { q -> ChatMessage.InterviewQuestionMsg(q.question, updatedHistory.size) })
                }
                _answerHistory.value = updatedHistory
                _currentQuestion.value = stepResult.question
                if (stepResult.done) {
                    logger.d("Interview complete!")
                    _chatMessages.update { it + ChatMessage.System("✅ Interview complete!") }
                    _uiState.update { it.copy(hasCompletedInterview = true) }
                    currentSessionId = null
                }
            } finally {
                setLoading(false)
            }
        }
    }
    fun clearSessionAnswers() {
        logger.d("Clear session answers triggered")
        _sessionAnswers.value = emptyList()
    }
    suspend fun getNextAdaptiveQuestion(userId: String): InterviewQuestion? {
        val answerHistory = interviewRepo.getAnswersForUser(userId)
        val weakTopic = answerHistory
            .groupBy { it.topic }
            .maxByOrNull { (_, answers) ->
                answers.count { ans ->
                    ans.score == null || ans.score < 60 || (ans.feedback ?: "").contains("incorrect", ignoreCase = true)
                }
            }?.key

        // All domain models here
        val allQuestions = interviewRepo.getAllQuestions()
        val askedQuestionIds = answerHistory.map { it.questionId }.toSet()
        return allQuestions
            .filter { it.id !in askedQuestionIds }
            .firstOrNull { weakTopic == null || it.topic == weakTopic }
    }
    private suspend fun calculateAverageScore(sessionId: String): Int {
        val answers = interviewRepo.getAnswersForSession(sessionId)
        val scores = answers.mapNotNull { it.score }
        return if (scores.isNotEmpty()) scores.average().toInt() else 0
    }
}
private fun ChatMessage.toChatUIMessage(): ChatUIMessage {
    return when (this) {
        is ChatMessage.User -> ChatUIMessage.Sent(this.content)
        is ChatMessage.System -> ChatUIMessage.Received(this.content)
        is ChatMessage.InterviewQuestionMsg -> ChatUIMessage.Received(this.question)
        is ChatMessage.Followup -> ChatUIMessage.Received(this.feedback)
        else -> ChatUIMessage.Received(this.toString())
    }
}
