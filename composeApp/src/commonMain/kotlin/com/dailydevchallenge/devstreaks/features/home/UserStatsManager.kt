package com.dailydevchallenge.devstreaks.features.home

import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.model.UserStats
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserStatsManager(private val repository: ChallengeRepository) {

    private val _userStats = MutableStateFlow(UserStats(name = "Dev", xp = 0, streak = 0))
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()

    private val _lastCompletedDate = MutableStateFlow<String?>(null)
    val lastCompletedDate: StateFlow<String?> = _lastCompletedDate.asStateFlow()

    suspend fun loadStats() {
        val (xpValue, streakValue, lastDate) = repository.getUserStats()
        // Update combined stats
        _userStats.value = _userStats.value.copy(xp = xpValue, streak = streakValue)
        _lastCompletedDate.value = lastDate
    }

    suspend fun refreshAfterTaskCompletion(taskId: String, xp: Int) {
        val userId = UserPreferences.getSafeUserId()
        repository.markTaskCompleted(taskId, xp , userId)
        loadStats()
    }
}
