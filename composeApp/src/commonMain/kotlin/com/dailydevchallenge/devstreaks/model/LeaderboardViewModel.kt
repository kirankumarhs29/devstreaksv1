package com.dailydevchallenge.devstreaks.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.features.feed.UserStats
import com.dailydevchallenge.devstreaks.repository.LeaderboardRepository
import com.dailydevchallenge.devstreaks.repository.UserInfoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel(
    private val repo: LeaderboardRepository,
    private val userInfoRepo: UserInfoRepository
): ViewModel() {
    val leaderboard = MutableStateFlow<List<UserStats>>(emptyList())
    val loading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    val currentUserId = MutableStateFlow<String?>(null)
    init {
        viewModelScope.launch {
            currentUserId.value = userInfoRepo.getCurrentUserProfile()?.userId
        }
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            loading.value = true
            try {
                leaderboard.value = repo.getAllUserStats()
                error.value = null
            } catch (e: Exception) {
                error.value = e.message
                leaderboard.value = emptyList()
            }
            loading.value = false
        }
    }
}
