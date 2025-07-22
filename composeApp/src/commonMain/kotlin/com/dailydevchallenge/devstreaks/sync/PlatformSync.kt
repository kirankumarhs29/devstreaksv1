@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package com.dailydevchallenge.devstreaks.sync

import com.dailydevchallenge.database.UserProgress
import com.dailydevchallenge.devstreaks.features.feed.UserStats
import com.dailydevchallenge.devstreaks.model.ChallengePathResponse
import com.dailydevchallenge.devstreaks.model.CompletedChallenge
import com.dailydevchallenge.devstreaks.model.TaskReflection
import com.dailydevchallenge.devstreaks.repository.PublicUserProfile


// platform/PlatformSync.kt
expect object PlatformSync {
    suspend fun uploadUserProgress(progress: UserProgress)
    suspend fun uploadCompletedChallenge(entry: CompletedChallenge)
    suspend fun uploadReflection(reflection: TaskReflection)
    suspend fun fetchGeneratedCourse(requestId: String): ChallengePathResponse?
}


interface FirebaseUserHelper {
    suspend fun getCurrentUserProfile(): PublicUserProfile?
    suspend fun getAllUserStats(): List<UserStats>
}

expect fun getPlatformFirebaseUserHelper(): FirebaseUserHelper
