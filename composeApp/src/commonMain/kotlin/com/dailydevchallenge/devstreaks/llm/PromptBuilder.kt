package com.dailydevchallenge.devstreaks.llm

import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import com.dailydevchallenge.devstreaks.features.devcoach.MemoryItem

object PromptBuilder {

    fun buildPrompt(memoryItems: List<MemoryItem>, userInput: String,
                    profile: LearningProfile? = null): List<ChatMessage> {
        val systemMessage = ChatMessage(
            role = Role.SYSTEM.value,
            content = buildSystemPrompt(profile)
        )

        val history = memoryItems.flatMap {
            listOf(
                ChatMessage(Role.USER.value, it.question),
                ChatMessage(Role.ASSISTANT.value, it.feedback.ifBlank { it.userAnswer })
            )
        }

        val latestInput = ChatMessage(Role.USER.value, userInput)

        return listOf(systemMessage) + history + latestInput
    }
}

private fun buildSystemPrompt(profile: LearningProfile?): String {
    return buildString {
        appendLine("You are DevCoach, a friendly AI that helps young developers stay motivated " +
                "and focused to improve daily. Respond with insights, encouragement, and " +
                "practical " +
                "suggestions.")
        appendLine("🎯 Your job is to:- Be concise and helpful (max 7-8 sentences)- Break content " +
                "into lists," +
                " tips, or bullet points - Use emojis or headers where appropriate - Be positive," +
                " relatable, and direct - End with 1-2 clear action or suggestion")

        if (profile != null) {
            appendLine("The user profile is:")
            appendLine("🎯 Goal: ${profile.goal}")
            appendLine("💪 Skills: ${profile.skills.joinToString()}")
            appendLine("🧠 Learning Style: ${profile.style}")
            appendLine("🧠 Experience Level: ${profile.experience}")
            appendLine("⏰ Time Available: ${profile.timePerDay} per day for ${profile.days} days")
            appendLine("😨 Biggest Fear: ${profile.fear}")
            appendLine("Use this context when responding to user input.")
        }
        appendLine("If you're unsure or don’t have enough context, ask a clarifying question " +
                "before answering.")

    }.trim()
}
