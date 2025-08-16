package com.dailydevchallenge.devstreaks.service

import com.dailydevchallenge.database.UserProgress
import com.dailydevchallenge.devstreaks.model.AdaptiveChallenge
import com.dailydevchallenge.devstreaks.model.PerformanceMetrics
import com.dailydevchallenge.devstreaks.model.CodeChallenge // Import CodeChallenge
import com.dailydevchallenge.devstreaks.utils.generateUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class AdaptiveChallengeGenerator(private val llmApi: LLMApi) {
    suspend fun generateChallenges(
        userId: String,
        userProgress: List<UserProgress>,
        performanceMetrics: List<PerformanceMetrics>
    ): List<AdaptiveChallenge> = withContext(Dispatchers.Default) {
        val prompt = buildPrompt(userId, userProgress, performanceMetrics)
        val llmResponse = llmApi.generateChallenges(prompt)
        parseChallengesFromLLM(llmResponse).map {
            AdaptiveChallenge(
                id = generateUUID(),
                title = it.title,
                description = it.description,
                difficulty = "medium",
                pathId = "generated-path",
                userId = userId
            )
        }
    }

    private fun buildPrompt(
        userId: String,
        userProgress: List<UserProgress>,
        performanceMetrics: List<PerformanceMetrics>
    ): String {
        return """
            Generate adaptive coding challenges for user $userId based on:
            - Progress: ${userProgress.size} completed
            - Performance: ${performanceMetrics.map { "time=${it.durationMillis}" }}
            Return JSON list of challenges with fields: id, title, description, difficulty, pathId, userId
        """.trimIndent()
    }

    private fun parseChallengesFromLLM(llmResponse: String): List<AdaptiveChallenge> {
        val codeChallenges = listOf(
            CodeChallenge(
                title = "Generated Challenge",
                description = "Solve a problem based on your recent progress.",
                starterCode = "// Starter code here",
                solution = "// Solution code here"
            )
        )

        println("LLM Response: $llmResponse")

        return codeChallenges.map {
            AdaptiveChallenge(
                id = generateUUID(),
                title = it.title,
                description = it.description,
                difficulty = "medium",
                pathId = "generated-path",
                userId = "placeholder-user"
            )
        }
    }
}

interface LLMApi {
    suspend fun generateChallenges(prompt: String): String
}
