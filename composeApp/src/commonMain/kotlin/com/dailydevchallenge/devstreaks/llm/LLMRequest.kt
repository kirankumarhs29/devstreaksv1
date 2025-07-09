package com.dailydevchallenge.devstreaks.llm

import kotlinx.serialization.Serializable

@Serializable
data class LLMMessage(
    val role: String,
    val content: String
)

@Serializable
data class LLMRequest(
    val model: String,
    val messages: List<LLMMessage>
)
