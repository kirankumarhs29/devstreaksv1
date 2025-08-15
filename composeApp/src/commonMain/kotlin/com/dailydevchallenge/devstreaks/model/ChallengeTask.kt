package com.dailydevchallenge.devstreaks.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.KeepGeneratedSerializer


// ChallengeTask.kt
@Serializable
data class ChallengeTask(
    val id: String,
    val pathId: String,
    val day: Int,
    val title: String,
    val type: String, // e.g., DSA, Project, SystemDesign, AI
    val content: String, // Overall summary or context
    val skill: String? = null,
    val xp: Int,
    val checklist: List<String> = emptyList(),
    val whyItMatters: String? = null,
    val bonus: String? = null,
    val tip: String? = null,
    val aiBreakdown: String? = null,
    val videoUrl: String? = null,
    val codeExample: String? = null,
    val challenges: List<ChallengeActivity> = emptyList()
)
fun ChallengeTask.effectiveChallenges(): List<ChallengeActivity> {
    return if (challenges.isEmpty() && !codeExample.isNullOrBlank()) {
        listOf(
            ChallengeActivity(
                id = "auto-code-${id}",
                type = ActivityType.CODE,
                prompt = "Code Example: Try implementing this logic.",
                starterCode = codeExample,
                language = "Kotlin",
                explanation = "Auto-generated from today's code example."
            )
        )
    } else challenges
}


@Serializable
data class ChallengeActivity(
    val id: String,
    val type: ActivityType,
    val prompt: String,
    val options: List<String>? = null,
    val correctAnswer: String? = null,
    val language: String? = null,
    val starterCode: String? = null,
    val explanation: String? = null,
    val solutionCode: String? = null,
    val videoUrl: String? = null,
    val insight: String? = null,        // Why this matters
    val goal: String? = null,           // What you should achieve
    val skillFocus: String? = null      // e.g., "Loops", "Debugging", "Edge Cases"
)

@Serializable
enum class ActivityType {
    QUIZ, CODE, FLASHCARD, PROJECT, AI_LESSON
}
@Serializable
data class ChallengePathResponse(
    val track: String,
    val days: List<ChallengeTask>
)

@Serializable
data class ChallengePath(
    val id: String,
    val track: String
)
@Serializable
data class ChallengePathWithTasks(
    val id: String,
    val track: String,
    val tasks: List<ChallengeTask>
)

data class CompletedChallenge(
    val pathId: String,
    val completedDate: String
)

data class TaskReflection(
    val id: String,
    val taskId: String,
    val reflection: String,
    val timestamp: String
)


data class UserLearningHistory(
    val priorGoals: List<String>,
    val priorSkills: List<String>,
    val experience: String,
    val styles: List<String>,
    val reflections: List<TaskReflection>
)

@Serializable
data class RemoteInterviewQuestion(
    val type: String,
    val question: String,
    val topic: String = "",
    val difficulty: Int = 0,
    val followUp: String = ""
)
@Serializable
data class InterviewQuestion(
    val id: String,                // Add this!
    val question: String,
    val topic: String = "",        // for adaptivity/personalization (optional)
    val difficulty: Int = 0,       // add if using for adaptive learning
    val followUp: String = ""

)

@Serializable
data class InterviewStepResult(
    val question: InterviewQuestion?, // null when finished
    val feedback: String?, // Feedback/critique for user's last answer
    val score: Int? = null,
    val done: Boolean
)
data class ResumeAnalysisModel(
    val id: String,
    val userId: String,
    val resumeText: String,
    val skillsMatched: List<String>,
    val skillsMissing: List<String>,
    val recommendations: String,
    val createdAt: Long
)


@Serializable
data class InterviewSessionContext(
    val jobRole: String,
    val resumeSummary: String,
    val skills: List<String>,
    val answerHistory: List<Pair<String, String>> // List of (question, answer) so far
)
@Serializable
data class ResumeAnalysis(
    val id: String,                 // Add this!
    val userId: String,
    val summary: String,
    val skillsMatched: List<String>,
    val skillsMissing: List<String>,
    val jobMatchScore: Long, // out of 100
    val recommendations: String,
    val createdAt: Long             // For sorting/history
)
@Serializable
data class RemoteResumeAnalysis(
    val summary: String,
    val skillsMatched: List<String>,
    val skillsMissing: List<String>,
    val jobMatchScore: Int,
    val recommendations: String
)


@Serializable
data class StartInterviewPayload(
    val jobRole: String,
    val resumeSummary: String,
    val skills: List<String>
)
@Serializable
data class StepInterviewPayload(
    val jobRole: String,
    val resumeSummary: String,
    val skills: List<String>,
    val lastQuestion: String,
    val userAnswer: String,
    val answerHistory: List<QAHistory>
)
@Serializable
data class QAHistory(
    val question: String,
    val answer: String
)
data class UserAnswer(
    val id: String,                  // Add this!
    val questionId: String,
    val sessionId: String,
    val answerText: String,
    val feedback: String? = null,
    val score: Int? = null,
    val topic: String? = null, // <- required for adaptation!
    val timestamp: Long
)

@Serializable
data class RemoteInterviewStepResult(
    val question: RemoteInterviewQuestion? = null,
    val feedback: String? = null,
    val score: Int? = null,  // Add this
    val done: Boolean = false, // Add default value
)

data class EngagementRecord(
    val taskId: String,
    val userId: String,
    val startTime: Long,
    val endTime: Long,
    val durationMillis: Long
)






