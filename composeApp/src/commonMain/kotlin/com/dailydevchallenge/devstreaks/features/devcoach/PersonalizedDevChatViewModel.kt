package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfilePreferences
import com.dailydevchallenge.devstreaks.llm.ChatUIMessage
import com.dailydevchallenge.devstreaks.ai.UnifiedAICoachService
import com.dailydevchallenge.devstreaks.ai.UserContextManager
import com.dailydevchallenge.devstreaks.ai.CoachingContext
import com.dailydevchallenge.devstreaks.repository.Conversation
import com.dailydevchallenge.devstreaks.repository.MemoryRepository
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.service.PersonalizedAICoachingService
import com.dailydevchallenge.devstreaks.service.WeakAreaDetectionService
import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Enhanced DevChatViewModel with Personalized AI Coaching
 * Integrates performance data, weak area detection, and contextual coaching
 */
class PersonalizedDevChatViewModel(
    private val memoryRepo: MemoryRepository,
    private val unifiedAIService: UnifiedAICoachService,
    private val profilePreferences: LearningProfilePreferences,
    private val legacyAIService: com.dailydevchallenge.devstreaks.llm.LLMService,
    private val userContextManager: UserContextManager,
    private val challengeRepository: ChallengeRepository,
    private val personalizedCoachingService: PersonalizedAICoachingService,
    private val weakAreaDetectionService: WeakAreaDetectionService
) : ViewModel() {

    val chatMessages = mutableStateListOf<ChatUIMessage>()
    var isTyping by mutableStateOf(false)
        private set

    var currentCoachingContext by mutableStateOf<PersonalizedCoachingContext?>(null)
        private set

    var weakAreas by mutableStateOf<List<WeakArea>>(emptyList())
        private set

    var lastPersonalizedResponse by mutableStateOf<PersonalizedCoachingResponse?>(null)
        private set

    private val logger = { getLogger() }

    init {
        viewModelScope.launch {
            loadChatHistory()
            initializePersonalizedContext()
        }
    }

    private suspend fun loadChatHistory() {
        try {
            val history = memoryRepo.getAllConversations()
            chatMessages.clear()

            if (history.isEmpty()) {
                // Generate personalized welcome message
                val welcomeMessage = generatePersonalizedWelcomeMessage()
                chatMessages.add(ChatUIMessage.Received(welcomeMessage))
            } else {
                chatMessages.addAll(
                    history.flatMap { conv ->
                        listOf(
                            ChatUIMessage.Sent(conv.userMessage),
                            ChatUIMessage.Received(conv.botResponse)
                        )
                    }
                )
            }
        } catch (e: Exception) {
            logger().e("PersonalizedDevChat", e,"Error loading chat history")
            chatMessages.add(ChatUIMessage.Received("Welcome back! I'm your personalized AI coding coach. How can I help you today?"))
        }
    }

    private suspend fun initializePersonalizedContext() {
        try {
            val userId = UserPreferences.getSafeUserId()
            val profile = profilePreferences.getProfile()

            // Build personalized coaching context
            currentCoachingContext = personalizedCoachingService.buildPersonalizedContext(userId, profile)

            // Load user's weak areas
            weakAreas = weakAreaDetectionService.detectWeakAreas(userId)

            logger().d("PersonalizedDevChat",
                "Initialized context for user $userId: " +
                "Difficulty=${currentCoachingContext?.currentDifficultyLevel?.name}, " +
                "WeakAreas=${weakAreas.size}, " +
                "State=${currentCoachingContext?.motivationalState?.name}"
            )
        } catch (e: Exception) {
            logger().e("PersonalizedDevChat", e,"Error initializing personalized context: ${e
                .message}")
        }
    }

    private suspend fun generatePersonalizedWelcomeMessage(): String {
        val userId = UserPreferences.getSafeUserId()
        val profile = profilePreferences.getProfile()

        return try {
            val response = personalizedCoachingService.generatePersonalizedResponse(
                userMessage = "welcome",
                userId = userId,
                learningProfile = profile
            )

            val context = currentCoachingContext
            val welcomeBuilder = StringBuilder()

            welcomeBuilder.append("🎯 Welcome back to your personalized coding journey! ")

            // Add performance-based context
            context?.let { ctx ->
                when (ctx.motivationalState) {
                    MotivationalState.CONFIDENT -> {
                        welcomeBuilder.append("You've been absolutely crushing it with a ${(ctx.recentPerformance.successRate * 100).toInt()}% success rate! ")
                    }
                    MotivationalState.IMPROVING -> {
                        welcomeBuilder.append("I can see your skills improving steadily - that's the spirit! ")
                    }
                    MotivationalState.STRUGGLING -> {
                        welcomeBuilder.append("I know coding can be challenging, but you're making progress. ")
                    }
                    else -> {
                        welcomeBuilder.append("Your ${ctx.learningStreak}-day streak shows real commitment! ")
                    }
                }
            }

            // Add weak area awareness
            if (weakAreas.isNotEmpty()) {
                val primaryWeakArea = weakAreas.first()
                welcomeBuilder.append("I've been analyzing your progress and noticed we should focus on ${primaryWeakArea.skillArea} today. ")
            }

            welcomeBuilder.append("\n\n✨ What would you like to work on? I'm here to provide personalized guidance based on your unique learning journey!")

            // Add motivational boost
            response.motivationalBoost?.let { boost ->
                welcomeBuilder.append("\n\n$boost")
            }

            welcomeBuilder.toString()
        } catch (e: Exception) {
            "🎯 Welcome back! I'm your AI coding coach, ready to provide personalized guidance based on your learning journey. What would you like to work on today?"
        }
    }

    fun sendMessage(userInput: String) {
        val profile = profilePreferences.getProfile()
        if (profile == null) {
            chatMessages += ChatUIMessage.Received("Please complete your learning profile first so I can provide personalized coaching.")
            return
        }

        viewModelScope.launch {
            chatMessages.add(ChatUIMessage.Sent(userInput))
            isTyping = true

            try {
                val userId = UserPreferences.getSafeUserId()

                // Generate personalized coaching response
                val personalizedResponse = personalizedCoachingService.generatePersonalizedResponse(
                    userMessage = userInput,
                    userId = userId,
                    learningProfile = profile
                )

                lastPersonalizedResponse = personalizedResponse

                // Format the response with personalized insights
                val formattedResponse = formatPersonalizedResponse(personalizedResponse)

                chatMessages.add(ChatUIMessage.Received(formattedResponse))

                // Save conversation with enhanced context
                saveConversationWithContext(userInput, formattedResponse, personalizedResponse)

                logger().d("PersonalizedDevChat",
                    "Generated personalized response: Style=${personalizedResponse.coachingStyle}, " +
                    "WeakAreaFocus=${personalizedResponse.weakAreaFocus.joinToString()}"
                )

            } catch (e: Exception) {
                logger().e("PersonalizedDevChat", e,"Error generating personalized response: ${e
                    .message}")

                // Fallback to basic AI service
                val fallbackResponse = "I'm having some trouble accessing your personalized data right now, but I can still help! " +
                        "Based on your question about '$userInput', here's what I can share..."

                chatMessages.add(ChatUIMessage.Received(fallbackResponse))
                memoryRepo.saveConversation(userInput, fallbackResponse)
            } finally {
                isTyping = false
            }
        }
    }

    private fun formatPersonalizedResponse(response: PersonalizedCoachingResponse): String {
        val formatted = StringBuilder()

        // Main response
        formatted.append(response.responseText) // Fixed: use responseText instead of response
        formatted.append("\n\n")

        // Add personalized insight if available
        response.personalizedInsight?.let { insight ->
            formatted.append("💡 **Personal Insight**: $insight")
            formatted.append("\n\n")
        }

        // Add recommended actions if any
        if (response.personalizedRecommendations.isNotEmpty()) {
            formatted.append("📋 **Recommended Actions**:\n")
            response.personalizedRecommendations.forEachIndexed { index, action ->
                formatted.append("${index + 1}. $action\n")
            }
            formatted.append("\n")
        }

        // Add weak area focus if relevant
        if (response.weakAreaFocus.isNotEmpty()) {
            formatted.append("🎯 **Focus Areas**: ${response.weakAreaFocus.joinToString(", ")}")
            formatted.append("\n\n")
        }

        // Add next challenge hint if available
        response.nextChallengeHint?.let { hint ->
            formatted.append("💡 **Next Challenge Tip**: $hint")
        }

        return formatted.toString().trim()
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun saveConversationWithContext(
        userInput: String,
        response: String,
        personalizedResponse: PersonalizedCoachingResponse
    ) {
        try {
            // Save basic conversation
            memoryRepo.saveConversation(userInput, response)

            // Update coaching context based on interaction
            currentCoachingContext?.let { context ->
                val updatedContext = context.copy(
                    lastInteractionTime = Clock.System.now().toEpochMilliseconds(),
                    contextSummary = "${context.contextSummary} Last discussed: ${personalizedResponse.weakAreaFocus.joinToString()}"
                )
                currentCoachingContext = updatedContext
            }

        } catch (e: Exception) {
            logger().e("PersonalizedDevChat", e,"Error saving conversation context: ${e.message}")
        }
    }

    /**
     * Get current user performance summary for UI display
     */
    fun getUserPerformanceSummary(): UserPerformanceSummary? {
        return currentCoachingContext?.recentPerformance
    }

    /**
     * Get coaching insights for the current session
     */
    fun getCoachingInsights(): List<String> {
        val insights = mutableListOf<String>()

        currentCoachingContext?.let { context ->
            insights.add("Current Level: ${context.currentDifficultyLevel.name}")
            insights.add("Success Rate: ${(context.recentPerformance.successRate * 100).toInt()}%")
            insights.add("Learning Streak: ${context.learningStreak} days")

            if (weakAreas.isNotEmpty()) {
                insights.add("Primary Focus: ${weakAreas.first().skillArea}")
            }
        }

        return insights
    }

    /**
     * Manually refresh personalized context (useful after completing challenges)
     */
    fun refreshPersonalizedContext() {
        viewModelScope.launch {
            initializePersonalizedContext()
        }
    }

    /**
     * Get coaching style explanation for user
     */
    fun getCoachingStyleExplanation(): String {
        return currentCoachingContext?.preferredCoachingStyle?.let { style ->
            when (style) {
                CoachingStyle.ENCOURAGING -> "I'm using an encouraging approach to boost your confidence and motivation."
                CoachingStyle.PATIENT -> "I'm taking a patient approach, breaking things down step-by-step."
                CoachingStyle.CHALLENGING -> "I'm pushing you with challenging content since you're performing well!"
                CoachingStyle.ANALYTICAL -> "I'm providing data-driven insights based on your performance patterns."
                CoachingStyle.PRACTICAL -> "I'm focusing on practical, actionable guidance you can apply immediately."
                CoachingStyle.STRATEGIC -> "I'm helping you develop a long-term learning strategy based on your goals and performance."
                CoachingStyle.SUPPORTIVE -> "I'm here to support you through challenges, providing resources and encouragement."
                CoachingStyle.MOTIVATIONAL -> "I'm using motivational techniques to keep you engaged and excited about learning."
            }
        } ?: "I adapt my coaching style based on your performance and preferences."
    }
}
