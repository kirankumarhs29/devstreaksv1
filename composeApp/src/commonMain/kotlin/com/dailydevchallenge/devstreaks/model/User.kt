package com.dailydevchallenge.devstreaks.model

import kotlinx.datetime.Instant

data class User(
    val userId: String,
    val email: String,
    val passwordHash: String,
    val username: String,
    val avatarUrl: String? = null,
    val createdAt: Instant,
    val lastLogin: Instant,
    val xp: Int = 0,
    val level: Int = 1,
    val dailyStreak: Int = 0,
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
    val attempts: Int = 0,
    val score: Int? = null,
    val feedback: String? = null,
    val hintUsed: Boolean = false,
    val xpEarned: Int = 0,
    val timeSpentSeconds: Int? = null
)
