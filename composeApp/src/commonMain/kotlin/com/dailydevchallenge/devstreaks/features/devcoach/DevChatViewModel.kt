package com.dailydevchallenge.devstreaks.features.devcoach

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfilePreferences
import com.dailydevchallenge.devstreaks.llm.ChatUIMessage
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.llm.PromptBuilder
import com.dailydevchallenge.devstreaks.repository.Conversation
import com.dailydevchallenge.devstreaks.repository.MemoryRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class DevChatViewModel(
    private val memoryRepo: MemoryRepository,
    private val aiService: LLMService,
    private val profilePreferences: LearningProfilePreferences
) : ViewModel() {

    private var messageId = 0
    val chatMessages = mutableStateListOf<ChatUIMessage>()
    var isTyping by mutableStateOf(false)
        private set

    private var messageInput by mutableStateOf("")

    init {
        viewModelScope.launch {
            val history = memoryRepo.getAllConversations() // Adjust this if method name differs
            chatMessages.clear()
            chatMessages.addAll(
                history.flatMap { conv ->
                    listOf(
                        ChatUIMessage.Sent(conv.userMessage),
                        ChatUIMessage.Received(conv.botResponse)
                    )
                }
            )
        }
    }
    fun onMessageChange(newText: String) {
        messageInput = newText
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

            val memory = memoryRepo.getRecentMemory() // Load from SQLDelight
            val prompt = PromptBuilder.buildPrompt(memory, userInput, profile)

            try {
                val response = aiService.generateResponse(prompt)
                chatMessages.add(ChatUIMessage.Received(response))
                memoryRepo.saveConversation(userInput, response)
            } catch (e: Exception) {
                chatMessages.add(ChatUIMessage.Received("Oops! Something went wrong."))
            } finally {
                isTyping = false
            }
        }
        messageInput = ""
    }
    fun loadAllConversations(): List<Conversation> {
        return runBlocking {
            memoryRepo.getAllConversations()
        }
    }

    fun clearAllConversations(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            memoryRepo.deleteAllMemory()
            onDone()
        }
    }
    fun clearChat() {
        chatMessages.clear()
        isTyping = false
        viewModelScope.launch {
            memoryRepo.deleteAllMemory()
        }
        chatMessages.add(ChatUIMessage.Received("Chat cleared. Ask me anything"))
    }

}
