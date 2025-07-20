package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.database.InterviewSession
import com.dailydevchallenge.database.UserAnswer
import com.dailydevchallenge.devstreaks.database.toModel
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.model.InterviewQuestion
import com.dailydevchallenge.devstreaks.model.InterviewSessionContext
import com.dailydevchallenge.devstreaks.model.ResumeAnalysis
import com.dailydevchallenge.devstreaks.repository.InterviewRepository
import com.dailydevchallenge.devstreaks.repository.ResumeAnalysisRepository
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.utils.generateUUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

class ResumeChatViewModel(
    private val llmService: LLMService,
    private val resumeRepo: ResumeAnalysisRepository,
    private val interviewRepo: InterviewRepository
) : ViewModel() {

    private val _resumeText = MutableStateFlow("")
    val resumeText: StateFlow<String> get() = _resumeText

    private val _analysis = MutableStateFlow<ResumeAnalysis?>(null)

    // --- User Profile ---
    private val _jobRole = MutableStateFlow("")
    val jobRole: StateFlow<String> get() = _jobRole

    private val _skills = MutableStateFlow<List<String>>(emptyList())
    val skills: StateFlow<List<String>> get() = _skills

    // --- Chat / Coaching / Adaptive Interview ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> get() = _chatMessages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

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


    //    fun pickResumeFile() {
//        llmService.pickPdfAndExtractText { extracted ->
//            _resumeText.value = extracted
//        }
//    }
    fun setResumeText(text: String) {
        _resumeText.value = text
        _chatMessages.update { it + ChatMessage.FileUpload("📄 Resume uploaded") }
        analyzeAndShow() // Now, will immediately analyze after upload

    }
    fun loadHistory() {
        viewModelScope.launch {
            val userId = UserPreferences.getSafeUserId()
            _resumeHistory.value = resumeRepo.getAllForUser(userId)
                .map { it.toModel() }
            _interviewSessions.value = interviewRepo.getSessionsForUser(userId)
            // .map { entity -> entity.toModel() } // If you want your own model format
        }
    }
    fun loadSessionAnswers(sessionId: String) {
        viewModelScope.launch {
            _sessionAnswers.value = interviewRepo.getAnswersForSession(sessionId)
        }
    }
    val latestResumeAnalysis: ResumeAnalysis?
        get() = resumeHistory.value.firstOrNull()


    // Call this when the screen starts
    fun loadLastOrPromptForResume() {
        viewModelScope.launch {
            val userId = UserPreferences.getSafeUserId()
            val lastAnalysis = resumeRepo.getAllForUser(userId).firstOrNull()
            if (lastAnalysis != null) {
                _analysis.value = lastAnalysis.toModel()
                // Update chat messages/UI to offer direct access to interview or to upload again
            } else {
                // No analysis found, prompt for resume upload
                _chatMessages.value = listOf(
                    ChatMessage.System("👋 Hi! Let’s get you interview-ready."),
                    ChatMessage.System("Please upload your resume (PDF only).")
                )
            }
        }
    }

    fun startProfileSetup(greeting: Boolean = true) {
        _chatMessages.value = emptyList()
        if (greeting) {
            _chatMessages.value = listOf(
                ChatMessage.System("👋 Hi! Let's get you interview-ready."),
                ChatMessage.System("What job role are you targeting?")
            )
        }
        _jobRole.value = ""
        _resumeText.value = ""
        _analysis.value = null
        _skills.value = emptyList()
        _answerHistory.value = emptyList()
        _currentQuestion.value = null
    }

    fun setJobRole(role: String) {
        _jobRole.value = role
        _chatMessages.update {
            it + ChatMessage.User(role) +
                    ChatMessage.System("Please upload your resume (PDF only).")
        }
    }

    fun setSkills(skills: List<String>) {
        _skills.value = skills
    }

    fun analyzeAndShow() {
        val resume = _resumeText.value
        val role = _jobRole.value
        if (resume.isBlank()) return

        _isLoading.value = true
        val analysisId = generateUUID()
        viewModelScope.launch {
            try {
                val result = llmService.analyzeResume(resume, role)
                _analysis.value = result
                // Save analysis to DB
                resumeRepo.saveResumeAnalysis(
                    id = analysisId,
                    userId = UserPreferences.getSafeUserId(),
                    resumeText = resume,
                    summary = result.summary,
                    skillsParsed = result.skillsMatched.joinToString(","),
                    skillsGaps = result.skillsMissing.joinToString(","),
                    recommendations = result.recommendations,
                    createdAt = Clock.System.now().toEpochMilliseconds()
                )
                _chatMessages.update { list ->
                    list + listOf(
                        ChatMessage.System("📝 Resume analysis for '$role':"),
                        ChatMessage.System("🧠 ${result.summary}"),
                        ChatMessage.System("✅ Strengths: ${result.skillsMatched.joinToString()}"),
                        ChatMessage.System("⚠️ Weaknesses: ${result.skillsMissing.joinToString()}"),
                        ChatMessage.System("🏆 Role Fit Score: ${result.jobMatchScore}"),
                        ChatMessage.System("📚 Recommendations: ${result.recommendations}"),
                        ChatMessage.System("Ready for a mock interview? Type 'yes' to start.")
                    )
                }
            } catch (e: Exception) {
                _chatMessages.update { it + ChatMessage.System("❌ Couldn’t analyze your resume. Try again.") }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startAdaptiveInterview() {
        val role = _jobRole.value
        val summary = _analysis.value?.summary ?: ""
        val skills = _analysis.value?.skillsMatched ?: emptyList()
        val resumeAnalysisId = _analysis.value?.id ?: return
        val sessionId = generateUUID()
        currentSessionId = sessionId
        _isLoading.value = true
        viewModelScope.launch {
            try {
                interviewRepo.createSession(
                    userId = UserPreferences.getSafeUserId(),
                    resumeAnalysisId = resumeAnalysisId,
                    summary = summary,
                    sessionDate = Clock.System.now().toEpochMilliseconds()
                )
                val result = llmService.startInterviewSession(role, summary, skills)
                result.question?.let { q ->
                    val questionId = generateUUID()
                    // Save the first interview question
                    interviewRepo.saveQuestion(
                        id = questionId,
                        questionText = q.question,
                        topic = q.topic,
                        difficulty = q.difficulty,
                        followUp = q.followUp,
                        lastAskedAt = Clock.System.now().toEpochMilliseconds(),
                        timesAsked = 1
                    )
                    _chatMessages.update { it + ChatMessage.InterviewQuestionMsg(q.question, 0) }
                    _currentQuestion.value = q
                    _answerHistory.value = emptyList()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitInterviewAnswer(answer: String) {
        val currentQ = _currentQuestion.value ?: return
        val role = _jobRole.value
        val summary = _analysis.value?.summary ?: ""
        val skills = _analysis.value?.skillsMatched ?: emptyList()
        val sessionId = currentSessionId ?: return // Ensure session exists

        val updatedHistory = _answerHistory.value + (currentQ.question to answer)
        _isLoading.value = true
        viewModelScope.launch {
            try {
                interviewRepo.saveUserAnswer(
                    questionId = currentQ.id,             // InterviewQuestion.id required in your model!
                    sessionId = sessionId,
                    answerText = answer,
                    feedback = null,                      // Feedback comes after LLM returns it
                    score = null,                         // Or score if you have it from LLM
                    topic = currentQ.topic,
                    timestamp = Clock.System.now().toEpochMilliseconds()
                )
                val ctx = InterviewSessionContext(role, summary, skills, updatedHistory)
                val stepResult = llmService.submitInterviewAnswer(answer, currentQ, ctx)
                if (stepResult.feedback != null) {
                    // Optionally, update answer row with feedback after LLM
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
                    _chatMessages.update { it + ChatMessage.System("✅ Interview complete!") }
                    currentSessionId = null // Reset session
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun clearSessionAnswers() {
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





}
