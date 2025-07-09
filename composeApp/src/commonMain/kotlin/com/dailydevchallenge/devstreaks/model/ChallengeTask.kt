package com.dailydevchallenge.devstreaks.model

import kotlinx.serialization.Serializable


// ChallengeTask.kt
@Serializable
data class ChallengeTask(
    val id: String,
    val pathId: String,
    val day: Int,
    val title: String,
    val type: String, // e.g., DSA, Project, SystemDesign, AI
    val content: String, // Overall summary or context
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
    val solutionCode: String? = null, // ✅ Add this line
    val videoUrl: String? = null //
)

@Serializable
enum class ActivityType {
    QUIZ, CODE, FLASHCARD, PROJECT
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
data class InterviewQuestion(
    val question: String,
    val type: String,
    val expectedAnswer: String,
    val followUp: String
)

@Serializable
data class ResumeAnalysis(
    val summary: String,
    val skillsMatched: List<String>,
    val skillsMissing: List<String>,
    val jobMatchScore: Int, // out of 100
    val recommendations: List<String>
)



