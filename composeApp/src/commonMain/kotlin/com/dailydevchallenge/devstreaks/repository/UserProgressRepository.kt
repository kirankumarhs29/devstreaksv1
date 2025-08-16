package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.devstreaks.model.Challenge
import com.dailydevchallenge.devstreaks.model.ChallengeStatus
import com.dailydevchallenge.devstreaks.model.User
import com.dailydevchallenge.devstreaks.model.UserStats
import com.dailydevchallenge.devstreaks.model.UserChallengeProgress
import com.dailydevchallenge.devstreaks.model.calculateLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable

interface UserProgressRepository {
    fun markChallengeCompleted(id: String)
    fun isChallengeCompleted(id: String): Boolean
    fun getTodayChallenge(): Challenge
    fun getXp(): Int
    fun getStreak(): Int
    fun resetProgress()
    fun advanceDay()
    suspend fun markTaskCompleted(taskId: String, xp: Int, userId: String)
    fun isTaskCompleted(taskId: String, userId: String): Boolean
}

interface UserInfoRepository {
    suspend fun getCurrentUserProfile(): PublicUserProfile?
}

@Serializable
data class PublicUserProfile(
    val id: String,
    val displayName: String,
    val level: Int,
    val totalXp: Int,
    val streak: Int,
    val badges: List<String>
)
