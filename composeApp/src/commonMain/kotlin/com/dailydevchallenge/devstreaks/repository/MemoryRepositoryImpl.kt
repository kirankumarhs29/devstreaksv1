package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.database.ConversationMemoryQueries
import com.dailydevchallenge.devstreaks.features.devcoach.MemoryItem
import com.dailydevchallenge.devstreaks.utils.generateUUID
import kotlinx.datetime.Clock


class MemoryRepositoryImpl(
    private val memoryQueries: ConversationMemoryQueries
) : MemoryRepository {

    override suspend fun getRecentMemory(limit: Int): List<MemoryItem> {
        return memoryQueries
            .selectRecentMemories(limit.toLong())
            .executeAsList()
            .map {
                MemoryItem(
                    id = it.id,
                    question = it.question,
                    userAnswer = it.userAnswer,
                    feedback = it.feedback ?: "",
                    timestamp = it.timestamp ?: 0L
                )
            }

    }

    override suspend fun saveConversation(question: String, answer: String) {
        memoryQueries.insertMemory(
            id = generateUUID(),
            question = question,
            userAnswer = answer,
            feedback = null,
            timestamp = Clock.System.now().toEpochMilliseconds() // epoch seconds
        )

    }

    override suspend fun getAllMemory(): List<MemoryItem> {
        return memoryQueries.selectAll().executeAsList().map {
            MemoryItem(
                id = it.id,
                question = it.question,
                userAnswer = it.userAnswer,
                feedback = it.feedback ?: "",
                timestamp = it.timestamp?: 0L,
            )
        }
    }


    override suspend fun updateFeedback(id: String, feedback: String) {
        memoryQueries.updateFeedback(
            feedback = feedback,
            id = id
        )
    }

    override suspend fun deleteAllMemory() {
        memoryQueries.deleteAll()
    }
    override suspend fun getAllConversations(): List<Conversation> {
        return memoryQueries.selectAllMemories().executeAsList().map {
            Conversation(
                userMessage = it.question,
                botResponse = it.userAnswer
            )
        }
    }
}
