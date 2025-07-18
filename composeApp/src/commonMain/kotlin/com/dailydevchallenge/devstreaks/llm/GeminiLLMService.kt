package com.dailydevchallenge.devstreaks.llm

import com.dailydevchallenge.devstreaks.model.ChallengePathResponse
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.model.ResumeAnalysis
import com.dailydevchallenge.devstreaks.utils.PlatformUtils
import com.dailydevchallenge.devstreaks.utils.getLogger
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.dailydevchallenge.devstreaks.model.InterviewQuestion
import com.dailydevchallenge.devstreaks.model.InterviewSessionContext
import com.dailydevchallenge.devstreaks.model.InterviewStepResult
import com.dailydevchallenge.devstreaks.model.QAHistory
import com.dailydevchallenge.devstreaks.model.StepInterviewPayload
import com.dailydevchallenge.devstreaks.model.StartInterviewPayload
import kotlinx.serialization.builtins.ListSerializer


private val jsonFormatter = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
}
private val logger = getLogger() // ✅ Correct

suspend fun <T> retryWithBackoff(
    retries: Int = 3,
    initialDelay: Long = 1000L,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(retries - 1) {
        try {
            return block()
        } catch (e: Exception) {
            println("Retry attempt failed: ${e.message}")
            logger.e("Retry attempt failed: ${e.message}", e , tag = "retryWithBackoff")
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
    }
    return block() // Final attempt
}

class GeminiLLMService(
    private val client: HttpClient,
    private val apiKey: String
) : LLMService {

    override suspend fun generateGeminiPlan(
        goal: String,
        skills: List<String>,
        experience: String,
        timePerDay: Int,
        days: Int,
        style: String,
        fear: String
    ): ChallengePathResponse {
        logger.d("Generating Gemini plan with goal: $goal, skills: $skills, experience: $experience, timePerDay: $timePerDay, days: $days, style: $style, fear: $fear")
        val response = retryWithBackoff {
            client.post("https://us-central1-devsteaks.cloudfunctions.net/generateCourseWithGemini") {
                contentType(ContentType.Application.Json)
                setBody(
                    GenerateCourseRequest(
                        goal = goal,
                        skills = skills,
                        experience = experience,
                        timePerDay = timePerDay,
                        days = days,
                        style = style,
                        fear = fear
                    )
                )

            }
        }


        val body = response.bodyAsText()
        logger.d("Gemini response:\n$body")
        return try {
            jsonFormatter.decodeFromString(ChallengePathResponse.serializer(), body)
        } catch (e: Exception) {
            logger.e("Failed to parse Gemini plan response", e)
            throw e
        }
    }

    override suspend fun generatePlan(
        goal: String,
        skills: List<String>,
        experience: String,
        timePerDay: Int,
        days: Int,
        style: String,
        fear: String,
        useOpenAI: Boolean
    ): ChallengePathResponse {
        logger.i("Generating plan using ${if (useOpenAI) "OpenAI" else "Gemini"}...")
        return if (useOpenAI) {
            generatePlanWithOpenAI(goal, skills, experience, timePerDay, days, style, fear)
        } else {
            generateGeminiPlan(goal, skills, experience, timePerDay, days, style, fear)
        }
    }

    private suspend fun generatePlanWithOpenAI(
        goal: String,
        skills: List<String>,
        experience: String,
        timePerDay: Int,
        days: Int,
        style: String,
        fear: String
    ): ChallengePathResponse {
        logger.d("Generating OpenAI plan with goal: $goal, skills: $skills, experience: $experience, timePerDay: $timePerDay, days: $days, style: $style, fear: $fear")
        val response = retryWithBackoff {
            client.post("https://us-central1-devsteaks.cloudfunctions.net/generateCourseWithOpenAI") {
                contentType(ContentType.Application.Json)
                setBody(
                    GenerateCourseRequest(
                        goal = goal,
                        skills = skills,
                        experience = experience,
                        timePerDay = timePerDay,
                        days = days,
                        style = style,
                        fear = fear
                    )
                )

            }
        }

        val body = response.bodyAsText()
        logger.d("OpenAI response:\n$body")

        return try {
            jsonFormatter.decodeFromString(ChallengePathResponse.serializer(), body)
        } catch (e: Exception) {
            logger.e("Failed to parse OpenAI plan response", e)
            throw e
        }
    }

    override suspend fun generateQuickPractice(skills: List<String>): ChallengeTask {
        val prompt = buildQuickPracticePrompt(skills)
        logger.i("Generating quick practice with skills: $skills")
        logger.d("Quick practice prompt: $prompt")

        val response = retryWithBackoff {
            client.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-pro:generateContent") {
                url.parameters.append("key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(mapOf("contents" to listOf(mapOf("parts" to listOf(mapOf("text" to prompt))))))
            }
        }

        val text = response.bodyAsText()
        logger.d("Gemini quick practice response raw: $text")
        val content = try {
            Json.parseToJsonElement(text)
                .jsonObject["candidates"]
                ?.jsonArray?.get(0)
                ?.jsonObject?.get("content")
                ?.jsonObject?.get("parts")
                ?.jsonArray?.get(0)
                ?.jsonObject?.get("text")
                ?.jsonPrimitive?.content ?: throw Exception("No content")
        } catch (e: Exception) {
            logger.e("Failed to extract quick practice content", e)
            throw e
        }

        val cleanedJson = content
            .removePrefix("```json")
            .removeSuffix("```")
            .trim()
        logger.d("Cleaned quick practice JSON: $cleanedJson")

        return try {
            jsonFormatter.decodeFromString(ChallengeTask.serializer(), cleanedJson)
        } catch (e: Exception) {
            logger.e("Failed to parse ChallengeTask from quick practice", e)
            throw e
        }
    }

    private fun buildQuickPracticePrompt(skills: List<String>): String {
        val skillsList = if (skills.isEmpty()) "None" else skills.joinToString(", ")
        return """
            Generate a single practice task as JSON (no markdown, no explanation). Structure:
            {
              "id": "String",
              "type": "QUIZ | FLASHCARD",
              "prompt": "String",
              "options": ["String", ...],  // only for QUIZ
              "correctAnswer": "String?",  // only for QUIZ
              "explanation": "String",
              "language": "String?",       // if code related
              "starterCode": "String?",    // optional
              "solutionCode": "String?"    // optional
            }

            Skills: $skillsList
            Tone: fun, Gen Z, emoji friendly
        """.trimIndent()
    }

    override suspend fun generateResponse(prompt: List<ChatMessage>): String {
        logger.d("Sending prompt to Gemini: $prompt")

        val response = retryWithBackoff {
            client.post("https://us-central1-devsteaks.cloudfunctions.net/generateResponse") {
                contentType(ContentType.Application.Json)
                setBody(prompt) // Sending List<ChatMessage> directly
            }
        }

        val body = response.bodyAsText()
        logger.d("Gemini raw chat response:\n$body")

        return try {
            Json.parseToJsonElement(body)
                .jsonObject["reply"]  // ✅ Matches Firebase format
                ?.jsonPrimitive?.content ?: "No response from DevCoach."
        } catch (e: Exception) {
            logger.e("Failed to parse DevCoach response", e)
            "Oops, DevCoach couldn't reply."
        }
    }
    override suspend fun reviewCode(activityPrompt: String, language: String?, userCode: String):
            String {
        val reviewPrompt = """
        Act as a friendly and helpful code reviewer for a Gen Z learner. Here's the task:
        
        Task Description: $activityPrompt
        Language: ${language ?: "Any"}
        Learner's Code:
        ```
        $userCode
        ```

        Review the code with:
        - 2 strengths 💪
        - 2 improvement areas 🛠️
        - Suggestions if code is incorrect ❌
        - Explain gently and clearly 💡
    """.trimIndent()

        val messages = listOf(
            ChatMessage(role = Role.USER.value, content = reviewPrompt)
        )

        return generateResponse(messages)
    }
    override suspend fun analyzeResume(resumeText: String, jobRole: String): ResumeAnalysis {
        logger.i("Analyzing resume for role: $jobRole")
        return try {
            val response = retryWithBackoff {
                client.post("https://us-central1-devsteaks.cloudfunctions.net/analyzeResume") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("resumeText" to resumeText, "jobRole" to jobRole))
                }
            }
            val body = response.bodyAsText()
            logger.d("Resume analysis response: $body")
            if (body.trim().startsWith("<")) {
                throw IllegalStateException("Backend returned HTML (likely 404/not deployed/or error): $body")
            } else {
                jsonFormatter.decodeFromString(ResumeAnalysis.serializer(), body)
            }
        } catch (e: Exception) {
            logger.e("Failed to parse resume analysis", e)
            throw e
        }
    }

    override suspend fun generateMockInterview(
        role: String,
        experience: String,
        skills: List<String>
    ): List<InterviewQuestion> {
        logger.i("Generating mock interview for role: $role")

        val resumeMock = buildString {
            append("Role: $role\n")
            append("Experience: $experience\n")
            if (skills.isNotEmpty()) append("Skills: ${skills.joinToString(", ")}\n")
        }

        return try {
            val response = retryWithBackoff {
                client.post("https://us-central1-devsteaks.cloudfunctions.net/generateInterview") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("resumeText" to resumeMock, "jobRole" to role))
                }
            }
            val body = response.bodyAsText()
            logger.d("Interview questions raw JSON: $body")
            if (body.trim().startsWith("<")) {
                throw IllegalStateException("Backend returned HTML (likely 404/not deployed/or error): $body")
            } else {
                jsonFormatter.decodeFromString(ListSerializer(InterviewQuestion.serializer()), body)
            }
        } catch (e: Exception) {
            logger.e("Failed to parse interview questions", e)
            throw e
        }
    }


    override fun pickPdfAndExtractText(onExtracted: (String) -> Unit) {
        PlatformUtils.pickPdfAndExtract(onExtracted)
    }
    override suspend fun startInterviewSession(
        role: String,
        resumeSummary: String,
        skills: List<String>
    ): InterviewStepResult {
        logger.i("Starting interview session for role: $role")
        val payload = StartInterviewPayload(
            jobRole = role,
            resumeSummary = resumeSummary,
            skills = skills
        )
        return try {
            val response = retryWithBackoff {
                client.post("https://us-central1-devsteaks.cloudfunctions.net/startInterviewSession") {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
            }
            val body = response.bodyAsText()
            logger.d("Start interview session raw response: $body")
            if (body.trim().startsWith("<")) {
                throw IllegalStateException("Backend returned HTML (likely 404/not deployed/or error): $body")
            } else {
                jsonFormatter.decodeFromString(InterviewStepResult.serializer(), body)
            }
        } catch (e: Exception) {
            logger.e("Failed to start interview session", e)
            throw e
        }
    }

    override suspend fun submitInterviewAnswer(
        answer: String,
        previousQuestion: InterviewQuestion,
        context: InterviewSessionContext
    ): InterviewStepResult {
        logger.i("Submitting interview answer for question: ${previousQuestion.question}")
        val payload = StepInterviewPayload(
            jobRole = context.jobRole,
            resumeSummary = context.resumeSummary,
            skills = context.skills,
            lastQuestion = previousQuestion.question,
            userAnswer = answer,
            answerHistory = context.answerHistory.map { QAHistory(it.first, it.second) })
        return try {
            val response = retryWithBackoff {
                client.post("https://us-central1-devsteaks.cloudfunctions.net/stepInterview") {
                    contentType(ContentType.Application.Json)
                    setBody(payload)
                }
            }
            val body = response.bodyAsText()
            logger.d("Step interview raw response: $body")
            if (body.trim().startsWith("<")) {
                throw IllegalStateException("Backend returned HTML (likely 404/not deployed/or error): $body")
            } else {
                jsonFormatter.decodeFromString(InterviewStepResult.serializer(), body)
            }
        } catch (e: Exception) {
            logger.e("Failed to process interview step", e)
            throw e
        }
    }

}
