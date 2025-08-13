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
}

expect fun getPlatformFirebaseUserHelper(): FirebaseUserHelper
