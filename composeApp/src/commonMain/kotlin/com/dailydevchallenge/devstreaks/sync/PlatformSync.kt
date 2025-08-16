@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.dailydevchallenge.devstreaks.sync

import com.dailydevchallenge.database.UserProgress
import com.dailydevchallenge.devstreaks.features.feed.UserStats
import com.dailydevchallenge.devstreaks.model.ChallengePathResponse
import com.dailydevchallenge.devstreaks.model.CompletedChallenge
import com.dailydevchallenge.devstreaks.model.EngagementRecord
import com.dailydevchallenge.devstreaks.model.TaskReflection
import com.dailydevchallenge.devstreaks.model.User
import com.dailydevchallenge.devstreaks.repository.PublicUserProfile


// platform/PlatformSync.kt
expect object PlatformSync {
    suspend fun uploadUserProgress(progress: UserProgress)
    suspend fun uploadCompletedChallenge(entry: CompletedChallenge)
    suspend fun uploadReflection(reflection: TaskReflection)
    suspend fun fetchGeneratedCourse(requestId: String): ChallengePathResponse?
    suspend fun uploadEngagementData(record: EngagementRecord)
}


interface FirebaseUserHelper {
    suspend fun getCurrentUserProfile(): PublicUserProfile?
    suspend fun getAllUserStats(): List<UserStats>
    suspend fun updateUserProgress(userId: String, xp: Long, streak: Long?)
    suspend fun fetchUserProgress(userId: String): UserStats?
    suspend fun getCurrentUserdata(userId: String, email: String) : User
    suspend fun updateUserInFirestore(user: User) // Add this method

    // NEW: Methods needed for ProfileEditViewModel
    suspend fun getUserById(userId: String): User?
    suspend fun updateUser(user: User): Boolean

    // Enhanced methods for leaderboards and social features
    suspend fun getTopUsersByXP(limit: Int = 100): List<User>
    suspend fun getTopUsersByWeeklyXP(startOfWeek: kotlinx.datetime.Instant, limit: Int = 100): List<Pair<User, Int>>
    suspend fun getTopUsersByMonthlyXP(startOfMonth: kotlinx.datetime.Instant, limit: Int = 100): List<Pair<User, Int>>
    suspend fun getTopUsersByStreak(limit: Int = 100): List<User>
    suspend fun getUserGlobalRank(userId: String): Int
    suspend fun getUserFriends(userId: String): List<User>
    suspend fun addFriend(userId: String, friendUserId: String)
    suspend fun removeFriend(userId: String, friendUserId: String)
    suspend fun recordXpGain(userId: String, xpGained: Int, source: String)
    suspend fun updateUserStats(userId: String, xpGained: Int, streakIncrement: Int = 0)
    suspend fun getUserProfile(userId: String): User
}

expect fun getPlatformFirebaseUserHelper(): FirebaseUserHelper
