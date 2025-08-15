package com.dailydevchallenge.devstreaks.model

import kotlinx.serialization.Serializable

@Serializable
data class UserStats(
    val totalXp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalChallengesCompleted: Int = 0,
    val totalTimeSpent: Long = 0, // in minutes
    val skillsProgress: Map<String, Int> = emptyMap(), // skill -> progress percentage
    val weeklyProgress: List<Int> = List(7) { 0 }, // last 7 days XP
    val monthlyProgress: List<Int> = List(30) { 0 } // last 30 days XP
)
