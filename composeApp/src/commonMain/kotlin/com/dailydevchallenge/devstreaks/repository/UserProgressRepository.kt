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

class UserProgressRepositoryImpl : UserProgressRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _userStats = MutableStateFlow(UserStats())
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()

    private val _challengeProgress = MutableStateFlow<Map<String, UserChallengeProgress>>(emptyMap())
    val challengeProgress: StateFlow<Map<String, UserChallengeProgress>> = _challengeProgress.asStateFlow()

    // Track completed tasks
    private val _completedTasks = MutableStateFlow<Set<String>>(emptySet())

    suspend fun updateUser(user: User) {
        _currentUser.value = user
        updateUserStats(user)
    }

    suspend fun completeChallenge(pathId: String, day: Int, xpEarned: Int) {
        val currentUser = _currentUser.value ?: return
        val now = Clock.System.now()

        // Create a simple challenge progress entry using the actual model structure
        val progressId = "$pathId-$day"
        val challengeProgress = UserChallengeProgress(
            userId = currentUser.userId,
            challengeId = progressId,
            status = ChallengeStatus.COMPLETED,
            startedAt = now,
            completedAt = now,
            xpEarned = xpEarned.toLong()
        )

        _challengeProgress.value = _challengeProgress.value + (progressId to challengeProgress)

        // Update user stats
        val newTotalXp = currentUser.xp + xpEarned
        val newLevel = calculateLevel(newTotalXp)
        val newStreak = calculateStreak(currentUser.dailyStreak?.toInt() ?: 0)

        val updatedUser = currentUser.copy(
            xp = newTotalXp,
            level = newLevel.toLong(),
            dailyStreak = newStreak.toLong(),
            lastLogin = Clock.System.now().toEpochMilliseconds()
        )

        _currentUser.value = updatedUser
        updateUserStats(updatedUser)
    }

    override suspend fun markTaskCompleted(taskId: String, xp: Int, userId: String) {
        _completedTasks.value = _completedTasks.value + taskId

        // Update user XP and stats
        val currentUser = _currentUser.value
        if (currentUser != null) {
            val newTotalXp = currentUser.xp + xp
            val newLevel = calculateLevel(newTotalXp)
            val newStreak = calculateStreak(currentUser.dailyStreak?.toInt() ?: 0) // Fixed: Remove the extra parameter

            val updatedUser = currentUser.copy(
                xp = newTotalXp,
                level = newLevel.toLong(),
                dailyStreak = newStreak.toLong(),
                lastLogin = Clock.System.now().toEpochMilliseconds()
            )

            _currentUser.value = updatedUser
            updateUserStats(updatedUser)
        }
    }

    override fun isTaskCompleted(taskId: String, userId: String): Boolean {
        return _completedTasks.value.contains(taskId)
    }

    private fun calculateStreak(currentStreak: Int): Int {
        // Simple streak calculation - in real app, you'd want more sophisticated date handling
        return currentStreak + 1
    }

    private fun updateUserStats(user: User) {
        val currentStats = _userStats.value
        val updatedStats = currentStats.copy(
            totalXp = user.xp,
            level = user.level?.toInt() ?: 1,
            currentStreak = user.dailyStreak?.toInt() ?: 0,
            longestStreak = maxOf(currentStats.longestStreak, user.dailyStreak?.toInt() ?: 0),
            totalChallengesCompleted = currentStats.totalChallengesCompleted + 1
        )
        _userStats.value = updatedStats
    }

    fun getUserProgress(pathId: String): UserChallengeProgress? {
        return _challengeProgress.value[pathId]
    }

    suspend fun initializeUser(userId: String, email: String, displayName: String?) {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        val now = Clock.System.now().toEpochMilliseconds()
        val newUser = User(
            userId = userId,
            email = email,
            passwordHash = "", // Default empty for Firebase auth
            username = displayName ?: "User",
            createdAt = now,
            lastLogin = now
        )
        updateUser(newUser)
    }

    override fun markChallengeCompleted(id: String) {
        TODO("Not yet implemented")
    }

    override fun isChallengeCompleted(id: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getTodayChallenge(): Challenge {
        TODO("Not yet implemented")
    }

    override fun getXp(): Int {
        TODO("Not yet implemented")
    }

    override fun getStreak(): Int {
        TODO("Not yet implemented")
    }

    override fun resetProgress() {
        TODO("Not yet implemented")
    }

    override fun advanceDay() {
        TODO("Not yet implemented")
    }
}
