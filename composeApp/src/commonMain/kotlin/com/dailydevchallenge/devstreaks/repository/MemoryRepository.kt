package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.devstreaks.features.devcoach.MemoryItem

interface MemoryRepository {
    suspend fun getRecentMemory(limit: Int = 3): List<MemoryItem>
    suspend fun getAllMemory(): List<MemoryItem>    // ✅ Add this
    suspend fun saveConversation(question: String, answer: String)
    suspend fun updateFeedback(id: String, feedback: String)
    suspend fun deleteAllMemory()
    suspend fun getAllConversations(): List<Conversation>

}

data class Conversation(
    val userMessage: String,
    val botResponse: String
)

