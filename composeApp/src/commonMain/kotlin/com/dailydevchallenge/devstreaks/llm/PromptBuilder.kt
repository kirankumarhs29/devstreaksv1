package com.dailydevchallenge.devstreaks.llm

import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import com.dailydevchallenge.devstreaks.features.devcoach.MemoryItem

object PromptBuilder {

    fun getSystemResponse(input : String): List<ChatMessage>{

        val prompt  = buildString {
            appendLine("You're a sharp, technical mentor (like a Staff Engineer at Google). Explain the following topic in a way that a developer can read, learn, and retain in 30 seconds to 2 minutes.\n" +
                    "\n" +
                    "Instructions:\n" +
                    "- Explain the topic below in a short, clear, and memorable way that a developer can read in under 2 minutes.\n" +
                    "- Use a relatable analogy, metaphor, or real-world comparison (like superheroes, food, sports, or gaming) to make it memorable.\n" +
                    "- Output only 8–10 lines.\n" +
                    "- Include **one fun fact**, **pro tip**, or **common pitfall**.\n" +
                    "- Keep the tone warm, conversational, and motivating (not robotic).\n" +
                    "\n" +
                    "Topic: \"${input}\"\n" +
                    "\n" +
                    "Return just plain text. No markdown, no JSON.\n")
            appendLine("Keep it concise, focused, and easy to scan.")

        }

        return listOf(
            ChatMessage(
                role = Role.SYSTEM.value,
                content = prompt
            ),
            ChatMessage(
                role = Role.USER.value,
                content = input
            )
        )
    }

    fun buildPrompt(memoryItems: List<MemoryItem>, userInput: String,
                    profile: LearningProfile? = null): List<ChatMessage> {
        val systemMessage = ChatMessage(
            role = Role.SYSTEM.value,
            content = buildSystemPrompt(profile)
        )

        val history = memoryItems.flatMap {
            listOf(
                ChatMessage(Role.USER.value, "User asked: ${it.question}"),
                ChatMessage(Role.ASSISTANT.value, "You responded: ${it.feedback.ifBlank { it.userAnswer }}")
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
        appendLine("Use all past questions and answers to understand user progress and context.")
        appendLine("Factor in the user's learning profile to tailor motivation and advice.")
        appendLine("Be positive, concise (max 4-6 sentences), break advice into bullet points, " +
                "use emojis and headers.")
        appendLine("Always refer to user's past feedback and goals when coaching.")
        appendLine("If user input is ambiguous or you need more info, ask clarifying questions before coaching.")
        appendLine("End responses with clear action items or suggestions.")

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
