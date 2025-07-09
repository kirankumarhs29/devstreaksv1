package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.model.ResumeAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ResumeChatViewModel(
    private val llmService: LLMService
) : ViewModel() {

    private val _resumeText = MutableStateFlow("")
    val resumeText: StateFlow<String> get() = _resumeText

    private val _analysis = MutableStateFlow<ResumeAnalysis?>(null)
    val analysis: StateFlow<ResumeAnalysis?> get() = _analysis

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    fun pickResumeFile() {
        llmService.pickPdfAndExtractText { extracted ->
            _resumeText.value = extracted
        }
    }
    fun setResumeText(text: String) {
        _resumeText.value = text
    }


    fun analyze(jobRole: String) {
        val resume = _resumeText.value
        if (resume.isBlank()) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val result = llmService.analyzeResume(resume, jobRole)
                _analysis.value = result
            } catch (e: Exception) {
                _analysis.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}
