package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.devstreaks.features.leaderboard.LeaderboardEntry
import com.dailydevchallenge.devstreaks.features.leaderboard.toLeaderboardEntry
import com.dailydevchallenge.devstreaks.model.User
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.sync.FirebaseUserHelper
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*

class LeaderboardRepository(
    private val firebaseUserHelper: FirebaseUserHelper // Use the platform-specific helper
) {
    private val logger = getLogger()

    suspend fun getGlobalLeaderboard(limit: Int = 100): Result<List<LeaderboardEntry>> = withContext(Dispatchers.IO) {
        try {
            logger.d("Fetching global leaderboard from Firebase")
            val users = firebaseUserHelper.getTopUsersByXP(limit)
            val leaderboard = users.mapIndexed { index, user ->
                user.toLeaderboardEntry(rank = index + 1)
            }
            Result.success(leaderboard)
        } catch (e: Exception) {
            logger.e("Failed to fetch global leaderboard", e)
            Result.failure(e)
        }
    }

    suspend fun getWeeklyLeaderboard(limit: Int = 100): Result<List<LeaderboardEntry>> = withContext(Dispatchers.IO) {
        try {
            logger.d("Fetching weekly leaderboard from Firebase")
            val startOfWeek = getStartOfCurrentWeek()
            val usersWithWeeklyXp = firebaseUserHelper.getTopUsersByWeeklyXP(startOfWeek, limit)
            val leaderboard = usersWithWeeklyXp.mapIndexed { index, (user, weeklyXp) ->
                user.toLeaderboardEntry(rank = index + 1, weeklyXp = weeklyXp)
            }
            Result.success(leaderboard)
        } catch (e: Exception) {
            logger.e("Failed to fetch weekly leaderboard", e)
            Result.failure(e)
        }
    }

    suspend fun getMonthlyLeaderboard(limit: Int = 100): Result<List<LeaderboardEntry>> = withContext(Dispatchers.IO) {
        try {
            logger.d("Fetching monthly leaderboard from Firebase")
            val startOfMonth = getStartOfCurrentMonth()
            val usersWithMonthlyXp = firebaseUserHelper.getTopUsersByMonthlyXP(startOfMonth, limit)
            val leaderboard = usersWithMonthlyXp.mapIndexed { index, (user, monthlyXp) ->
                user.toLeaderboardEntry(rank = index + 1, monthlyXp = monthlyXp)
            }
            Result.success(leaderboard)
        } catch (e: Exception) {
            logger.e("Failed to fetch monthly leaderboard", e)
            Result.failure(e)
        }
    }

    suspend fun getStreakLeaderboard(limit: Int = 100): Result<List<LeaderboardEntry>> = withContext(Dispatchers.IO) {
        try {
            logger.d("Fetching streak leaderboard from Firebase")
            val users = firebaseUserHelper.getTopUsersByStreak(limit)
            val leaderboard = users.mapIndexed { index, user ->
                user.toLeaderboardEntry(rank = index + 1)
            }
            Result.success(leaderboard)
        } catch (e: Exception) {
            logger.e("Failed to fetch streak leaderboard", e)
            Result.failure(e)
        }
    }

    suspend fun getFriendsLeaderboard(): Result<List<LeaderboardEntry>> = withContext(Dispatchers.IO) {
        try {
            val userId = UserPreferences.getSafeUserId()
            logger.d("Fetching friends leaderboard for user: $userId")

            val friends = firebaseUserHelper.getUserFriends(userId)
            val friendsWithStats = friends.mapIndexed { index, friend ->
                friend.toLeaderboardEntry(rank = index + 1)
            }.sortedByDescending { it.xp }

            // Re-rank after sorting
            val rankedFriends = friendsWithStats.mapIndexed { index, entry ->
                entry.copy(rank = index + 1)
            }

            Result.success(rankedFriends)
        } catch (e: Exception) {
            logger.e("Failed to fetch friends leaderboard", e)
            Result.failure(e)
        }
    }

    suspend fun getUserRank(): Result<LeaderboardEntry> = withContext(Dispatchers.IO) {
        try {
            val userId = UserPreferences.getSafeUserId()
            logger.d("Fetching user rank for: $userId")

            val userRank = firebaseUserHelper.getUserGlobalRank(userId)
            val user = firebaseUserHelper.getUserProfile(userId)

            val entry = user.toLeaderboardEntry(rank = userRank)
            Result.success(entry)
        } catch (e: Exception) {
            logger.e("Failed to fetch user rank", e)
            Result.failure(e)
        }
    }

    suspend fun addFriend(friendUserId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = UserPreferences.getSafeUserId()
            logger.d("Adding friend: $friendUserId for user: $userId")

            firebaseUserHelper.addFriend(userId, friendUserId)
            Result.success(true)
        } catch (e: Exception) {
            logger.e("Failed to add friend", e)
            Result.failure(e)
        }
    }

    suspend fun removeFriend(friendUserId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = UserPreferences.getSafeUserId()
            logger.d("Removing friend: $friendUserId for user: $userId")

            firebaseUserHelper.removeFriend(userId, friendUserId)
            Result.success(true)
        } catch (e: Exception) {
            logger.e("Failed to remove friend", e)
            Result.failure(e)
        }
    }

    private fun getStartOfCurrentWeek(): Instant {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val dayOfWeek = today.dayOfWeek.value
        val startOfWeek = today.minus(DatePeriod(days = dayOfWeek - 1))
        return startOfWeek.atStartOfDayIn(TimeZone.currentSystemDefault())
    }

    private fun getStartOfCurrentMonth(): Instant {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val startOfMonth = LocalDate(today.year, today.month, 1)
        return startOfMonth.atStartOfDayIn(TimeZone.currentSystemDefault())
    }
}
