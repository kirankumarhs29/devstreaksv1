package com.dailydevchallenge.devstreaks.llm

import com.dailydevchallenge.devstreaks.model.ChallengePathResponse
import com.dailydevchallenge.devstreaks.model.InterviewQuestion
import com.dailydevchallenge.devstreaks.model.InterviewSessionContext
import com.dailydevchallenge.devstreaks.model.InterviewStepResult
import com.dailydevchallenge.devstreaks.model.ResumeAnalysis
import com.dailydevchallenge.devstreaks.utils.PlatformUtils

interface LLMService {
    suspend fun generateGeminiPlan(
        goal: String,
        skills: List<String>,
        experience: String,
        timePerDay: Int,
        days: Int,
        style: String,
        fear: String,
        requestId: String,
    ): ChallengePathResponse

    suspend fun generatePlan(
        goal: String,
        skills: List<String>,
        experience: String,
        timePerDay: Int,
        days: Int,
        style: String,
        fear: String,
        requestId: String,
        useOpenAI: Boolean = false
    ): ChallengePathResponse

    suspend fun generateResponse(prompt: List<ChatMessage>): String

    suspend fun reviewCode(activityPrompt: String, language: String?, userCode: String): String

    suspend fun generateMockInterview(role: String, experience: String, skills: List<String>): List<InterviewQuestion>
    suspend fun analyzeResume(resumeText: String, jobRole: String): ResumeAnalysis
    fun pickPdfAndExtractText(onExtracted: (String) -> Unit) {
        PlatformUtils.pickPdfAndExtract(onExtracted)
    }
    suspend fun startInterviewSession(
        role: String,
        resumeSummary: String,
        skills: List<String>
    ): InterviewStepResult

    suspend fun submitInterviewAnswer(
        answer: String,
        previousQuestion: InterviewQuestion,
        context: InterviewSessionContext
    ): InterviewStepResult




}
