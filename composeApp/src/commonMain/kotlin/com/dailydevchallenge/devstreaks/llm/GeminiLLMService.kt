package com.dailydevchallenge.devstreaks.llm

import com.dailydevchallenge.devstreaks.model.ActivityType
import com.dailydevchallenge.devstreaks.model.ChallengeActivity
import com.dailydevchallenge.devstreaks.model.ChallengePathResponse
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.model.ResumeAnalysis
import com.dailydevchallenge.devstreaks.model.RemoteResumeAnalysis
import com.dailydevchallenge.devstreaks.utils.PlatformUtils
import com.dailydevchallenge.devstreaks.utils.getLogger
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.dailydevchallenge.devstreaks.model.InterviewQuestion
import com.dailydevchallenge.devstreaks.model.InterviewSessionContext
import com.dailydevchallenge.devstreaks.model.InterviewStepResult
import com.dailydevchallenge.devstreaks.model.QAHistory
import com.dailydevchallenge.devstreaks.model.RemoteInterviewStepResult
import com.dailydevchallenge.devstreaks.model.StepInterviewPayload
import com.dailydevchallenge.devstreaks.model.StartInterviewPayload
import com.dailydevchallenge.devstreaks.utils.generateUUID
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import com.dailydevchallenge.devstreaks.settings.UserPreferences


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
    private val client: HttpClient
) : LLMService {

    override suspend fun generateGeminiPlan(
        goal: String,
        skills: List<String>,
        experience: String,
        timePerDay: Int,
        days: Int,
        style: String,
        fear: String,
        requestId: String
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
                        fear = fear,
                        requestId = requestId
                    )
                )

            }
        }


        val body = response.bodyAsText()
        logger.d("Gemini response:\n$body")

        // Check if the response contains an error
        return try {
            val jsonResponse = Json.parseToJsonElement(body).jsonObject

            // Check for error in response
            if (jsonResponse.containsKey("error")) {
                val errorMessage = jsonResponse["error"]?.jsonPrimitive?.content ?: "Unknown error"
                logger.e("Gemini API returned error: $errorMessage")
                throw Exception("Gemini API error: $errorMessage")
            }

            // Parse as ChallengePathResponse if no error
            jsonFormatter.decodeFromString(ChallengePathResponse.serializer(), body)
        } catch (e: Exception) {
            logger.e("Failed to parse Gemini plan response", e)
            // Return a fallback response instead of throwing
            createFallbackChallengeResponse(goal, skills, days, requestId)
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
        requestId: String,
        useOpenAI: Boolean
    ): ChallengePathResponse {
        logger.i("Generating plan using ${if (useOpenAI) "OpenAI" else "Gemini"}...")
        return if (useOpenAI) {
            generatePlanWithOpenAI(goal, skills, experience, timePerDay, days, style, fear, requestId)
        } else {
            generateGeminiPlan(goal, skills, experience, timePerDay, days, style, fear, requestId)
        }
    }

    private suspend fun generatePlanWithOpenAI(
        goal: String,
        skills: List<String>,
        experience: String,
        timePerDay: Int,
        days: Int,
        style: String,
        fear: String,
        requestId: String
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
                        fear = fear,
                        requestId = requestId
                    )
                )

            }
        }

        val body = response.bodyAsText()
        logger.d("OpenAI response length: ${body.length} characters")
        logger.d("OpenAI response preview:\n${body.take(500)}...")

        // Check if response appears to be truncated
        if (!body.trim().endsWith("}") && !body.trim().endsWith("]")) {
            logger.e("Response appears to be truncated. Last 100 chars: ${body.takeLast(100)}")
            throw Exception("Incomplete response received from AI service")
        }

        return try {
            // First, try to parse as a direct ChallengePathResponse
            jsonFormatter.decodeFromString(ChallengePathResponse.serializer(), body)
        } catch (directParseException: Exception) {
            // If that fails, check if it's an error wrapper with rawResponse
            try {
                val jsonElement = Json.parseToJsonElement(body)
                if (jsonElement.jsonObject.containsKey("error") && jsonElement.jsonObject.containsKey("rawResponse")) {
                    logger.d("Detected error wrapper, extracting rawResponse")
                    val rawResponse = jsonElement.jsonObject["rawResponse"]?.jsonPrimitive?.content
                    if (rawResponse != null) {
                        logger.d("Attempting to parse extracted rawResponse")
                        jsonFormatter.decodeFromString(ChallengePathResponse.serializer(), rawResponse)
                    } else {
                        throw Exception("rawResponse field is null")
                    }
                } else {
                    // Re-throw the original exception if it's not an error wrapper
                    throw directParseException
                }
            } catch (e: Exception) {
                logger.e("Failed to parse OpenAI plan response", e)
                logger.e("Original parse error: ${directParseException.message}")
                logger.e("Response body (first 1000 chars): ${body.take(1000)}")
                logger.e("Response body (last 200 chars): ${body.takeLast(200)}")
                throw Exception("Failed to parse AI response: ${e.message}")
            }
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

        // Firebase Cloud Function expects direct array of chat messages
        val payload = prompt.map { message ->
            mapOf(
                "role" to message.role,
                "content" to message.content
            )
        }

        val response = retryWithBackoff {
            client.post("https://us-central1-devsteaks.cloudfunctions.net/generateResponse") {
                contentType(ContentType.Application.Json)
                setBody(payload) // Send direct array of messages
            }
        }

        val body = response.bodyAsText()
        logger.d("Gemini raw chat response:\n$body")

        return try {
            val jsonResponse = Json.parseToJsonElement(body).jsonObject

            // Check for error first
            if (jsonResponse.containsKey("error")) {
                val errorMessage = jsonResponse["error"]?.jsonPrimitive?.content ?: "Unknown error"
                logger.e("Gemini API returned error: $errorMessage")
                return "I'm having trouble connecting to my AI brain right now. Please try again in a moment! 🤖"
            }

            // Extract reply from response
            jsonResponse["reply"]?.jsonPrimitive?.content
                ?: jsonResponse["response"]?.jsonPrimitive?.content // Try alternative field name
                ?: "No response from DevCoach."
        } catch (e: Exception) {
            logger.e("Failed to parse DevCoach response", e)
            "Sorry, I'm having trouble understanding the response. Let me try to help you differently! 🤗"
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
            require(jobRole.isNotBlank()) { "Job role cannot be blank" }
            require(resumeText.isNotBlank()) { "Resume text cannot be blank" }
            logger.d("Resume text length: ${resumeText.length}, job role: $jobRole")
            // Log the first 100 characters of the resume text for debugging
            if (resumeText.length > 100) {
                logger.d("Resume text preview: ${resumeText.take(100)}...")
            } else {
                logger.d("Resume text preview: $resumeText")
            }
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
                val apiResult = jsonFormatter.decodeFromString(RemoteResumeAnalysis.serializer(), body)
                ResumeAnalysis(
                    id = generateUUID(),
                    userId = UserPreferences.getSafeUserId(), // or whichever user id you use
                    summary = apiResult.summary,
                    skillsMatched = apiResult.skillsMatched,
                    skillsMissing = apiResult.skillsMissing,
                    jobMatchScore = apiResult.jobMatchScore.toLong(),
                    recommendations = apiResult.recommendations,
                    createdAt = Clock.System.now().toEpochMilliseconds()
                )
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
            require(role.isNotBlank()) { "Job role cannot be blank" }
            require(resumeSummary.isNotBlank()) { "Resume summary cannot be blank" }
            require(skills.isNotEmpty()) { "Skills list cannot be empty" }
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
                val apiResult = jsonFormatter.decodeFromString(
                    RemoteInterviewStepResult.serializer(),
                    body)
                InterviewStepResult(
                    question = apiResult.question?.let { remoteQ ->
                        InterviewQuestion(
                            id = generateUUID(),
                            question = remoteQ.question,
                            topic = remoteQ.topic,
                            difficulty = remoteQ.difficulty,
                            followUp = remoteQ.followUp
                        )
                    },
                    feedback = apiResult.feedback,
                    done = apiResult.done
                )
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
                val apiResult = jsonFormatter.decodeFromString(RemoteInterviewStepResult.serializer(), body)
                InterviewStepResult(
                    question = apiResult.question?.let { remoteQ ->
                        InterviewQuestion(
                            id = generateUUID(), // Always assign ID on the client!
                            question = remoteQ.question,
                            topic = remoteQ.topic,
                            difficulty = remoteQ.difficulty,
                            followUp = remoteQ.followUp
                        )
                    },
                    feedback = apiResult.feedback,
                    done = apiResult.done
                )
            }
        } catch (e: Exception) {
            logger.e("Failed to process interview step", e)
            throw e
        }
    }

    // Fallback method to create a basic challenge response when API fails
    private fun createFallbackChallengeResponse(
        goal: String,
        skills: List<String>,
        days: Int,
        requestId: String
    ): ChallengePathResponse {
        val fallbackTasks = (1..minOf(days, 7)).map { dayNumber ->
            ChallengeTask(
                id = "fallback-day-$dayNumber",
                pathId = requestId,
                day = dayNumber,
                title = "Day $dayNumber: $goal Fundamentals",
                type = goal,
                content = "Learn the basics of $goal on day $dayNumber. This is a fallback lesson while we work on getting the full AI-generated content.",
                xp = 50,
                checklist = listOf(
                    "Complete the reading material",
                    "Try the practice exercises",
                    "Review key concepts"
                ),
                whyItMatters = "Building strong fundamentals in $goal is essential for your learning journey.",
                tip = "Take your time and practice regularly for best results!",
                challenges = listOf(
                    ChallengeActivity(
                        id = "fallback-quiz-$dayNumber",
                        type = ActivityType.QUIZ,
                        prompt = "Test your understanding of $goal basics from day $dayNumber",
                        options = listOf(
                            "Option A: Basic concept",
                            "Option B: Intermediate concept",
                            "Option C: Advanced concept",
                            "Option D: Expert concept"
                        ),
                        correctAnswer = "Option A: Basic concept",
                        explanation = "This covers the fundamental concepts you need to master."
                    ),
                    ChallengeActivity(
                        id = "fallback-flashcard-$dayNumber",
                        type = ActivityType.FLASHCARD,
                        prompt = "Review key $goal terms and definitions",
                        explanation = "Flashcards help reinforce important concepts through spaced repetition."
                    )
                )
            )
        }

        return ChallengePathResponse(
            track = "Basic $goal Learning Path (Fallback)",
            days = fallbackTasks
        )
    }
}
