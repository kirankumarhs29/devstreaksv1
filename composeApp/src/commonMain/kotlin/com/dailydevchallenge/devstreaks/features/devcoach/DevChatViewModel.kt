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
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DevChatViewModel(
    private val memoryRepo: MemoryRepository,
    private val unifiedAIService: UnifiedAICoachService,
    private val profilePreferences: LearningProfilePreferences,
    private val legacyAIService: com.dailydevchallenge.devstreaks.llm.LLMService,
    private val userContextManager: UserContextManager
) : ViewModel() {

    val chatMessages = mutableStateListOf<ChatUIMessage>()
    var isTyping by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            loadChatHistory()
        }
    }

    private suspend fun loadChatHistory() {
        try {
            val history = memoryRepo.getAllConversations()
            chatMessages.clear()
            chatMessages.addAll(
                history.flatMap { conv ->
                    listOf(
                        ChatUIMessage.Sent(conv.userMessage),
                        ChatUIMessage.Received(conv.botResponse)
                    )
                }
            )
        } catch (e: Exception) {
            // Handle error gracefully
            chatMessages.add(ChatUIMessage.Received("Welcome! I'm your AI coding coach. How can I help you today?"))
        }
    }

    fun sendMessage(userInput: String) {
        val profile = profilePreferences.getProfile()
        if (profile == null) {
            chatMessages += ChatUIMessage.Received("Please complete your learning profile first.")
            return
        }

        viewModelScope.launch {
            chatMessages.add(ChatUIMessage.Sent(userInput))
            isTyping = true

            try {
                // Use the unified AI service with comprehensive context
                val result = unifiedAIService.sendMessage(
                    message = userInput,
                    context = CoachingContext.GENERAL
                )

                val response = result.getOrElse {
                    "I'm having trouble accessing my knowledge right now. Let me try to help you anyway with what I know about your coding journey."
                }

                chatMessages.add(ChatUIMessage.Received(response))

                // Save conversation to memory for future context
                memoryRepo.saveConversation(userInput, response)

            } catch (e: Exception) {
                chatMessages.add(ChatUIMessage.Received("I encountered an issue, but I'm still here to help! What coding challenge are you working on?"))
            } finally {
                isTyping = false
            }
        }
    }

    fun loadAllConversations(): List<Conversation> {
        return runBlocking {
            memoryRepo.getAllConversations()
        }
    }

    fun clearAllConversations(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            memoryRepo.deleteAllMemory()
            chatMessages.clear()

            // Add a welcome message with personalized context
            val welcomeMessage = generatePersonalizedWelcome()
            chatMessages.add(ChatUIMessage.Received(welcomeMessage))

            onDone()
        }
    }

    private suspend fun generatePersonalizedWelcome(): String {
        return try {
            val profile = userContextManager.getUserProfile()
            val resumeContext = userContextManager.getResumeContext()
            val challengeProgress = userContextManager.getChallengeProgress()

            buildString {
                append("Welcome back! ")

                if (challengeProgress.currentStreak > 0) {
                    append("I see you're on a ${challengeProgress.currentStreak}-day streak - fantastic! ")
                }

                if (challengeProgress.totalCompleted > 0) {
                    append("You've completed ${challengeProgress.totalCompleted} challenges so far. ")
                }

                resumeContext?.careerInsights?.targetRole?.let { role ->
                    append("I remember you're working towards becoming a $role. ")
                }

                append("What coding topic would you like to explore today?")
            }
        } catch (e: Exception) {
            "Chat cleared. I'm here to help with your coding journey - ask me anything!"
        }
    }

    fun clearChat() {
        chatMessages.clear()
        isTyping = false
        viewModelScope.launch {
            memoryRepo.deleteAllMemory()
            val welcomeMessage = generatePersonalizedWelcome()
            chatMessages.add(ChatUIMessage.Received(welcomeMessage))
        }
    }

    /**
     * Get contextual insights for the user based on all available data
     */
    fun getContextualInsights(): List<String> {
        return try {
            runBlocking {
                val insights = mutableListOf<String>()

                val challengeProgress = userContextManager.getChallengeProgress()
                if (challengeProgress.topicMastery.isNotEmpty()) {
                    val weakAreas = challengeProgress.topicMastery.entries
                        .filter { it.value < 0.6 }
                        .map { it.key }
                    if (weakAreas.isNotEmpty()) {
                        insights.add("💡 Consider practicing: ${weakAreas.take(2).joinToString()}")
                    }
                }

                val resumeContext = userContextManager.getResumeContext()
                resumeContext?.improvementAreas?.take(2)?.let { areas ->
                    if (areas.isNotEmpty()) {
                        insights.add("📄 Resume improvement: ${areas.joinToString()}")
                    }
                }

                val interviewHistory = userContextManager.getInterviewHistory()
                if (interviewHistory.weakTopics.isNotEmpty()) {
                    insights.add("🎯 Interview focus: ${interviewHistory.weakTopics.take(2).joinToString()}")
                }

                insights
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
