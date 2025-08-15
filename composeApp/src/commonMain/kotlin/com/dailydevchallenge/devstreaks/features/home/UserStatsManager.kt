package com.dailydevchallenge.devstreaks.features.home

import com.dailydevchallenge.devstreaks.model.UserStats
import com.dailydevchallenge.devstreaks.model.calculateLevel
import com.dailydevchallenge.devstreaks.repository.UserProgressRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserStatsManager(
    private val userProgressRepository: UserProgressRepositoryImpl
) {
    private val _userStats = MutableStateFlow(UserStats())
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()

    val currentUser = userProgressRepository.currentUser

    suspend fun onChallengeCompleted(pathId: String, day: Int, xpEarned: Int) {
        userProgressRepository.completeChallenge(pathId, day, xpEarned)
        loadStats() // Refresh stats after completion
    }

    suspend fun initializeUser(userId: String, email: String, displayName: String?) {
        userProgressRepository.initializeUser(userId, email, displayName)
    }

    suspend fun loadStats() {
        // Get stats from Firebase/Repository and update the StateFlow
        val userId = getCurrentUserId()
        try {
            // This should get the actual stats from Firebase via the repository
            val firebaseHelper = com.dailydevchallenge.devstreaks.sync.getPlatformFirebaseUserHelper()
            val feedStats = firebaseHelper.fetchUserProgress(userId)

            if (feedStats != null) {
                // Convert feed.UserStats to model.UserStats
                val modelStats = com.dailydevchallenge.devstreaks.model.UserStats(
                    totalXp = feedStats.xp,
                    level = calculateLevel(feedStats.xp),
                    currentStreak = feedStats.streak,
                    longestStreak = feedStats.streak, // You might want to track this separately
                    totalChallengesCompleted = 0 // You might want to track this
                )
                _userStats.value = modelStats
                com.dailydevchallenge.devstreaks.utils.getLogger().d("UserStatsManager", "Stats loaded: XP=${feedStats.xp}, Level=${modelStats.level}, Streak=${feedStats.streak}")
            } else {
                com.dailydevchallenge.devstreaks.utils.getLogger().w("UserStatsManager", "No stats found for user $userId")
            }
        } catch (e: Exception) {
            com.dailydevchallenge.devstreaks.utils.getLogger().e("UserStatsManager", e,"Failed to" +
                    " load stats: ${e.message}")
        }
    }

    suspend fun refreshAfterTaskCompletion(taskId: String, xpEarned: Int) {
        val userId = getCurrentUserId()
        userProgressRepository.markTaskCompleted(taskId, xpEarned, userId)
        loadStats() // Refresh stats after task completion
    }

    fun getProgressPercentage(currentXp: Int): Float {
        val currentLevel = calculateLevel(currentXp)
        val xpForCurrentLevel = getXpRequiredForLevel(currentLevel)
        val xpForNextLevel = getXpRequiredForLevel(currentLevel + 1)

        val progressInCurrentLevel = currentXp - xpForCurrentLevel
        val xpNeededForNext = xpForNextLevel - xpForCurrentLevel

        return if (xpNeededForNext > 0) {
            (progressInCurrentLevel.toFloat() / xpNeededForNext.toFloat()).coerceIn(0f, 1f)
        } else 1f
    }

    private fun getXpRequiredForLevel(level: Int): Int {
        return when (level) {
            1 -> 0
            2 -> 100
            3 -> 300
            4 -> 600
            5 -> 1000
            6 -> 1500
            7 -> 2100
            8 -> 2800
            9 -> 3600
            10 -> 4500
            else -> 4500 + ((level - 10) * 1000)
        }
    }

    private fun getCurrentUserId(): String {
        // This should get the current user ID from preferences or auth
        return com.dailydevchallenge.devstreaks.settings.UserPreferences.getSafeUserId()
    }
}
