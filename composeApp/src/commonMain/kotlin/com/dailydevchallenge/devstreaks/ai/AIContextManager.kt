package com.dailydevchallenge.devstreaks.ai

import com.dailydevchallenge.devstreaks.model.UserStats
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Manages AI context and conversation memory across all AI features
 * Ensures consistent personality and context-aware responses
 */
class AIContextManager {
    private val logger = getLogger()

    private val _conversationContext = MutableStateFlow(ConversationContext())
    val conversationContext: StateFlow<ConversationContext> = _conversationContext

    private val _userProfile = MutableStateFlow<UserAIProfile?>(null)
    val userProfile: StateFlow<UserAIProfile?> = _userProfile

    fun updateUserProgress(userStats: UserStats) {
        _userProfile.value = _userProfile.value?.copy(
            currentLevel = userStats.level,
            totalXp = userStats.totalXp,
            currentStreak = userStats.currentStreak,
            skillsProgress = userStats.skillsProgress
        ) ?: UserAIProfile.fromUserStats(userStats)
    }

    // Remove @OptIn(ExperimentalTime::class) - not needed for Clock.System
    fun addInteraction(
        type: InteractionType,
        input: String,
        output: String,
        success: Boolean
    ) {
        val interaction = AIInteraction(
            type = type,
            input = input.take(500), // Prevent memory bloat from long inputs
            output = output.take(1000), // Limit output size
            success = success,
            timestamp = Clock.System.now().toEpochMilliseconds()
        )

        _conversationContext.value = _conversationContext.value.copy(
            recentInteractions = _conversationContext.value.recentInteractions
                .takeLast(9) + interaction,
            totalInteractions = _conversationContext.value.totalInteractions + 1
        )
    }

    fun getContextualPrompt(basePrompt: String): String {
        val context = _conversationContext.value
        val profile = _userProfile.value

        return buildString {
            appendLine("System Context:")
            appendLine("User Level: ${profile?.currentLevel ?: "Beginner"}")
            appendLine("Total XP: ${profile?.totalXp ?: 0}")
            appendLine("Current Streak: ${profile?.currentStreak ?: 0} days")

            val topSkills = profile?.skillsProgress?.entries
                ?.sortedByDescending { it.value }
                ?.take(3)
                ?.map { "${it.key} (${it.value}%)" }
                ?.joinToString()

            appendLine("Top Skills: ${topSkills ?: "None yet"}")

            if (context.recentInteractions.isNotEmpty()) {
                appendLine("\nRecent Context:")
                context.recentInteractions.takeLast(3).forEach { interaction ->
                    appendLine("- ${interaction.type}: ${interaction.input.take(50)}...")
                }
            }

            appendLine("\nUser Request:")
            appendLine(basePrompt)
        }
    }

    // Add method to clear old interactions for memory management
    fun clearOldInteractions(olderThanHours: Int = 24) {
        val cutoffTime = Clock.System.now().toEpochMilliseconds() - (olderThanHours * 3600000L)
        _conversationContext.value = _conversationContext.value.copy(
            recentInteractions = _conversationContext.value.recentInteractions
                .filter { it.timestamp > cutoffTime }
        )
    }
}

@Serializable
data class ConversationContext(
    val recentInteractions: List<AIInteraction> = emptyList(),
    val currentTopic: String? = null,
    val sessionStartTime: Long = Clock.System.now().toEpochMilliseconds(),
    val totalInteractions: Int = 0
)

@Serializable
data class AIInteraction(
    val type: InteractionType,
    val input: String,
    val output: String,
    val success: Boolean,
    val timestamp: Long
)

@Serializable
data class UserAIProfile(
    val currentLevel: Int,
    val totalXp: Int,
    val currentStreak: Int,
    val skillsProgress: Map<String, Int>,
    val preferredTopics: List<String>,
    val learningStyle: String = "visual"
) {
    companion object {
        fun fromUserStats(userStats: UserStats): UserAIProfile {
            // Derive strong areas from skillsProgress (skills with >70% progress)
            val strongAreas = userStats.skillsProgress
                .filter { it.value > 70 }
                .keys
                .toList()

            return UserAIProfile(
                currentLevel = userStats.level,
                totalXp = userStats.totalXp,
                currentStreak = userStats.currentStreak,
                skillsProgress = userStats.skillsProgress,
                preferredTopics = strongAreas
            )
        }
    }
}

enum class InteractionType {
    CHAT, RESUME_ANALYSIS, INTERVIEW, CODE_REVIEW, CHALLENGE_FEEDBACK
}
