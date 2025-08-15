package com.dailydevchallenge.devstreaks.model

import kotlinx.datetime.Instant

data class User(
    val userId: String,
    val email: String,
    val passwordHash: String,
    val username: String,
    val avatarUrl: String? = null,
    val createdAt: Long,
    val lastLogin: Long,
    val xp: Int = 0,
    val level: Long? = 1,
    val dailyStreak: Long? = 0,
    val streakStartDate: Instant? = null,
    val preferences: Map<String, String> = emptyMap(),
    val role: String = "user",
    val badges: List<String> = emptyList()
)

enum class ChallengeDifficulty {
    EASY, MEDIUM, HARD
}

enum class ChallengeStatus {
    PENDING, COMPLETED, FAILED
}

data class UserChallengeProgress(
    val userId: String,
    val challengeId: String,
    val status: ChallengeStatus = ChallengeStatus.PENDING,
    val startedAt: Instant,
    val completedAt: Instant? = null,
    val attempts: Long? = 0,
    val score: Int? = null,
    val feedback: String? = null,
    val hintUsed: Boolean = false,
    val xpEarned: Long? = 0,
    val timeSpentSeconds: Long? = null
)

// Level calculation helper function
fun calculateLevel(xp: Int): Int {
    return when {
        xp < 100 -> 1
        xp < 300 -> 2
        xp < 600 -> 3
        xp < 1000 -> 4
        xp < 1500 -> 5
        xp < 2100 -> 6
        xp < 2800 -> 7
        xp < 3600 -> 8
        xp < 4500 -> 9
        else -> 10 + ((xp - 4500) / 1000)
    }
}

fun xpForNextLevel(currentXp: Int): Int {
    val currentLevel = calculateLevel(currentXp)
    return when (currentLevel) {
        1 -> 100 - currentXp
        2 -> 300 - currentXp
        3 -> 600 - currentXp
        4 -> 1000 - currentXp
        5 -> 1500 - currentXp
        6 -> 2100 - currentXp
        7 -> 2800 - currentXp
        8 -> 3600 - currentXp
        9 -> 4500 - currentXp
        else -> 1000 - ((currentXp - 4500) % 1000)
    }
}
