package com.dailydevchallenge.devstreaks.sync

// platform/PlatformSync.android.kt
import com.dailydevchallenge.database.UserProgress
import com.dailydevchallenge.devstreaks.model.CompletedChallenge
import com.dailydevchallenge.devstreaks.model.TaskReflection

actual object PlatformSync {
    actual suspend fun uploadUserProgress(progress: UserProgress) {
        FirestoreHelper.uploadUserProgress(progress)
    }

    actual suspend fun uploadCompletedChallenge(entry: CompletedChallenge) {
        FirestoreHelper.uploadCompletedChallenge(entry)
    }

    actual suspend fun uploadReflection(reflection: TaskReflection) {
        FirestoreHelper.uploadReflection(reflection)
    }
}
