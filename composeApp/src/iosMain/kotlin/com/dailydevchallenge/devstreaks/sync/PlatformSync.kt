package com.dailydevchallenge.devstreaks.sync
// platform/PlatformSync.ios.kt

import com.dailydevchallenge.database.UserProgress
import com.dailydevchallenge.devstreaks.model.*

actual object PlatformSync {
    actual suspend fun uploadUserProgress(progress: UserProgress) {}

    actual suspend fun uploadCompletedChallenge(entry: CompletedChallenge) {

    }

    actual suspend fun uploadReflection(reflection: TaskReflection) {
    }
}
