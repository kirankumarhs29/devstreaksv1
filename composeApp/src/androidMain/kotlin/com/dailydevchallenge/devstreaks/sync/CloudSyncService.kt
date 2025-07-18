// androidMain
package com.dailydevchallenge.devstreaks.sync

import com.dailydevchallenge.database.CompletedChallenge
import com.dailydevchallenge.database.TaskReflection
import com.dailydevchallenge.database.UserProgress
import com.google.firebase.firestore.FirebaseFirestore
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile

actual fun getCloudSyncService(): CloudSyncService = CloudSyncServiceAndroid()


class CloudSyncServiceAndroid : CloudSyncService {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun syncPushAll(
        profile: LearningProfile,
        xp: Int,
        streak: Int,
        lastCompletedDate: String?
    ) {
        val userId = profile.goal + "_" + profile.fear // Replace with real userId
        val data = mapOf(
            "profile" to profile,
            "xp" to xp,
            "streak" to streak,
            "lastCompletedDate" to lastCompletedDate
        )

        db.collection("users")
            .document(userId)
            .set(data)
            .addOnSuccessListener {
                println("✅ Synced to Firebase")
            }
            .addOnFailureListener {
                println("❌ Sync failed: ${it.message}")
            }
    }
}
