package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.model.InterviewQuestion
import com.dailydevchallenge.devstreaks.model.InterviewSessionContext
import com.dailydevchallenge.devstreaks.model.ResumeAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResumeChatViewModel(
    private val llmService: LLMService
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


//    fun analyze(jobRole: String) {
//        val resume = _resumeText.value
//        if (resume.isBlank()) return
//
//        _isLoading.value = true
//        viewModelScope.launch {
//            try {
//                val result = llmService.analyzeResume(resume, jobRole)
//                _analysis.value = result
//            } catch (e: Exception) {
//                _analysis.value = null
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
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
        viewModelScope.launch {
            try {
                val result = llmService.analyzeResume(resume, role)
                _analysis.value = result
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
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = llmService.startInterviewSession(role, summary, skills)
                result.question?.let { q ->
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

        val updatedHistory = _answerHistory.value + (currentQ.question to answer)
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val ctx = InterviewSessionContext(role, summary, skills, updatedHistory)
                val stepResult = llmService.submitInterviewAnswer(answer, currentQ, ctx)
                _chatMessages.update {
                    it + ChatMessage.User(answer) +
                            listOfNotNull(stepResult.feedback?.let { f -> ChatMessage.Followup(f) }) +
                            listOfNotNull(stepResult.question?.let { q -> ChatMessage.InterviewQuestionMsg(q.question, updatedHistory.size) })
                }
                _answerHistory.value = updatedHistory
                _currentQuestion.value = stepResult.question
                if (stepResult.done) {
                    _chatMessages.update { it + ChatMessage.System("✅ Interview complete!") }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
