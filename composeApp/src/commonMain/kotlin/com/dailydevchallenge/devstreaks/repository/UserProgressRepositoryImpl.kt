package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.devstreaks.model.UserPerformanceMetrics
import com.dailydevchallenge.devstreaks.model.PerformanceMetrics
import com.dailydevchallenge.devstreaks.model.AdaptiveChallenge
import com.dailydevchallenge.devstreaks.service.AdaptiveChallengeGenerator
import com.dailydevchallenge.database.UserProfileQueries
import com.dailydevchallenge.database.UserProgress
import com.dailydevchallenge.devstreaks.model.Challenge
import com.dailydevchallenge.devstreaks.model.ChallengeStatus
import com.dailydevchallenge.devstreaks.model.DifficultyLevel
import com.dailydevchallenge.devstreaks.model.User
import com.dailydevchallenge.devstreaks.model.UserChallengeProgress
import com.dailydevchallenge.devstreaks.model.UserStats
import com.dailydevchallenge.devstreaks.model.calculateLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class UserProgressRepositoryImpl(
    private val userProfileQueries: UserProfileQueries,
    private val performanceMetricsProvider: suspend (taskId: String) -> PerformanceMetrics?,
    private val challengeRepository: ChallengeRepositoryImpl,
    private val adaptiveChallengeGenerator: AdaptiveChallengeGenerator
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _userStats = MutableStateFlow(UserStats())
    val userStats: StateFlow<UserStats> = _userStats.asStateFlow()

    private val _challengeProgress = MutableStateFlow<Map<String, UserChallengeProgress>>(emptyMap())
    val challengeProgress: StateFlow<Map<String, UserChallengeProgress>> = _challengeProgress.asStateFlow()

    private val _completedTasks = MutableStateFlow<Set<String>>(emptySet())

    suspend fun updateUser(user: User) {
        _currentUser.value = user
        updateUserStats(user)
    }

    suspend fun completeChallenge(pathId: String, day: Int, xpEarned: Int) {
        // Implementation logic here
    }

    suspend fun getUserPerformanceMetrics(userId: String): UserPerformanceMetrics = withContext(Dispatchers.Default) {
        val progressList = userProfileQueries.selectAllUserProgress().executeAsList().filter { it.userId == userId && it.completedTaskId != null }
        if (progressList.isEmpty()) {
            return@withContext UserPerformanceMetrics(0, 0.0, 0L)
        }
        // Updated mapping logic to handle the `score` property from the query result
        val metricsList = progressList.mapNotNull { progress ->
            PerformanceMetrics(
                id = progress.id,
                userId = progress.userId,
                taskId = progress.completedTaskId ?: "unknown",
                startTime = progress.completedDate?.toLong() ?: 0L, // Placeholder for start time
                endTime = progress.completedDate?.toLong() ?: 0L, // Placeholder for end time
                durationMillis = progress.completedDate?.toLong()?.minus(0L) ?: 0L, // Derived value
                attempts = 1, // Placeholder for attempts
                hintsUsed = 0, // Placeholder for hints used
                completed = true, // Placeholder for completion status
                errorCount = 0, // Placeholder for error count
                difficultyLevel = DifficultyLevel.EASY // Placeholder for difficulty level
            )
        }.filter { it.completed }
        if (metricsList.isEmpty()) {
            return@withContext UserPerformanceMetrics(0, 0.0, 0L)
        }
        val totalChallenges = metricsList.size
        val averageScore = metricsList.map { it.durationMillis.toDouble() / 1000.0 }.average() // Example: using durationMillis as a proxy for score
        val averageTime = metricsList.map { it.durationMillis.toDouble() }.average()
        UserPerformanceMetrics(
            totalChallenges = totalChallenges,
            averageScore = averageScore,
            averageTime = averageTime.toLong()
        )
    }

    suspend fun getAdaptiveChallenges(userId: String): List<AdaptiveChallenge> = withContext(Dispatchers.Default) {
        val cached = challengeRepository.getAdaptiveChallengesForUser(userId)
        if (cached.isNotEmpty()) return@withContext cached
        // Updated mapping logic to correctly map query results to UserProgress and PerformanceMetrics
        val userProgress = userProfileQueries.selectAllUserProgress().executeAsList().map {
            UserProgress(
                id = it.id,
                userId = it.userId,
                completedTaskId = it.completedTaskId,
                completedDate = it.completedDate
            )
        }

        // Correctly access `score` and resolve `durationMillis` in mapping logic
        val perfMetrics = userProgress.mapNotNull { progress ->
            performanceMetricsProvider(progress.completedTaskId ?: return@mapNotNull null)?.let {
                PerformanceMetrics(
                    id = progress.id,
                    userId = progress.userId,
                    taskId = progress.completedTaskId,
                    startTime = progress.completedDate?.toLong() ?: 0L, // Placeholder for start time
                    endTime = progress.completedDate?.toLong() ?: 0L, // Placeholder for end time
                    durationMillis = it.durationMillis, // Simplified
                    attempts = 1, // Placeholder for attempts
                    hintsUsed = 0, // Placeholder for hints used
                    completed = true, // Placeholder for completion status
                    errorCount = 0, // Placeholder for error count
                    difficultyLevel = DifficultyLevel.EASY // Placeholder for difficulty level
                )
            }
        }
        val generated = adaptiveChallengeGenerator.generateChallenges(userId, userProgress, perfMetrics)
        generated.forEach { challengeRepository.insertGeneratedChallenge(it as AdaptiveChallenge) }
        challengeRepository.getAdaptiveChallengesForUser(userId)
    }

    suspend fun markTaskCompleted(taskId: String, xp: Int, userId: String) {
        _completedTasks.value += taskId

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

    fun isTaskCompleted(taskId: String, userId: String): Boolean {
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
}
