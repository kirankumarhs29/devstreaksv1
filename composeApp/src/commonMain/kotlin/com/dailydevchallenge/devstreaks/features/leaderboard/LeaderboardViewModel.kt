package com.dailydevchallenge.devstreaks.features.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.model.User
import com.dailydevchallenge.devstreaks.repository.LeaderboardRepository
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class LeaderboardEntry(
    val userId: String,
    val username: String,
    val avatarUrl: String? = null,
    val xp: Int,
    val level: Long,
    val dailyStreak: Long,
    val rank: Int,
    val weeklyXp: Int = 0,
    val monthlyXp: Int = 0
)

enum class LeaderboardType {
    GLOBAL_XP,
    WEEKLY_XP,
    MONTHLY_XP,
    STREAK,
    FRIENDS
}

data class LeaderboardUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedType: LeaderboardType = LeaderboardType.GLOBAL_XP,
    val globalLeaderboard: List<LeaderboardEntry> = emptyList(),
    val weeklyLeaderboard: List<LeaderboardEntry> = emptyList(),
    val monthlyLeaderboard: List<LeaderboardEntry> = emptyList(),
    val streakLeaderboard: List<LeaderboardEntry> = emptyList(),
    val friendsLeaderboard: List<LeaderboardEntry> = emptyList(),
    val userRank: Int? = null,
    val userEntry: LeaderboardEntry? = null
)

class LeaderboardViewModel(
    private val leaderboardRepository: LeaderboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeaderboardUiState())
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private val logger = getLogger()

    init {
        loadLeaderboards()
    }

    fun selectLeaderboardType(type: LeaderboardType) {
        _uiState.value = _uiState.value.copy(selectedType = type)
        if (shouldRefreshLeaderboard(type)) {
            loadLeaderboard(type)
        }
    }

    fun refreshLeaderboards() {
        loadLeaderboards()
    }

    private fun loadLeaderboards() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Load all leaderboard types in parallel
                val globalDeferred = async { leaderboardRepository.getGlobalLeaderboard() }
                val weeklyDeferred = async { leaderboardRepository.getWeeklyLeaderboard() }
                val monthlyDeferred = async { leaderboardRepository.getMonthlyLeaderboard() }
                val streakDeferred = async { leaderboardRepository.getStreakLeaderboard() }
                val userRankDeferred = async { leaderboardRepository.getUserRank() }

                val global = globalDeferred.await().getOrThrow()
                val weekly = weeklyDeferred.await().getOrThrow()
                val monthly = monthlyDeferred.await().getOrThrow()
                val streak = streakDeferred.await().getOrThrow()
                val userRank = userRankDeferred.await().getOrNull()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    globalLeaderboard = global,
                    weeklyLeaderboard = weekly,
                    monthlyLeaderboard = monthly,
                    streakLeaderboard = streak,
                    userRank = userRank?.rank,
                    userEntry = userRank,
                    error = null
                )

                logger.d("Leaderboards loaded successfully")

            } catch (e: Exception) {
                logger.e("Failed to load leaderboards", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load leaderboards: ${e.message}"
                )
            }
        }
    }

    private fun loadLeaderboard(type: LeaderboardType) {
        viewModelScope.launch {
            try {
                when (type) {
                    LeaderboardType.FRIENDS -> {
                        val friends = leaderboardRepository.getFriendsLeaderboard().getOrThrow()
                        _uiState.value = _uiState.value.copy(friendsLeaderboard = friends)
                    }
                    else -> {
                        // Other types are loaded in loadLeaderboards()
                    }
                }
            } catch (e: Exception) {
                logger.e("Failed to load $type leaderboard", e)
            }
        }
    }

    private fun shouldRefreshLeaderboard(type: LeaderboardType): Boolean {
        return when (type) {
            LeaderboardType.FRIENDS -> _uiState.value.friendsLeaderboard.isEmpty()
            else -> false
        }
    }

    fun getCurrentLeaderboard(): List<LeaderboardEntry> {
        return when (_uiState.value.selectedType) {
            LeaderboardType.GLOBAL_XP -> _uiState.value.globalLeaderboard
            LeaderboardType.WEEKLY_XP -> _uiState.value.weeklyLeaderboard
            LeaderboardType.MONTHLY_XP -> _uiState.value.monthlyLeaderboard
            LeaderboardType.STREAK -> _uiState.value.streakLeaderboard
            LeaderboardType.FRIENDS -> _uiState.value.friendsLeaderboard
        }
    }
}

// Extension function to convert User to LeaderboardEntry
fun User.toLeaderboardEntry(rank: Int, weeklyXp: Int = 0, monthlyXp: Int = 0): LeaderboardEntry {
    return LeaderboardEntry(
        userId = userId,
        username = username.takeIf { it.isNotBlank() } ?: "User${userId.take(6)}",
        avatarUrl = avatarUrl,
        xp = xp,
        level = level?: 1L,
        dailyStreak = dailyStreak ?: 0,
        rank = rank,
        weeklyXp = weeklyXp,
        monthlyXp = monthlyXp
    )
}
