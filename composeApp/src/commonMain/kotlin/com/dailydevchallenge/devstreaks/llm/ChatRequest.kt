package com.dailydevchallenge.devstreaks.llm
import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

enum class Role(val value: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant")
}

@Serializable
data class GenerateCourseRequest(
    val goal: String,
    val skills: List<String>,
    val experience: String,
    val timePerDay: Int,
    val days: Int,
    val style: String,
    val fear: String
)

sealed class ChatUIMessage {
    data class Sent(val text: String) : ChatUIMessage()
    data class Received(val text: String) : ChatUIMessage()
}

