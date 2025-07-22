package com.dailydevchallenge.devstreaks.sync

import com.dailydevchallenge.devstreaks.features.feed.UserStats
import com.dailydevchallenge.devstreaks.repository.PublicUserProfile
import cocoapods.FirebaseAuth.FIRAuth


actual fun getPlatformFirebaseUserHelper(): FirebaseUserHelper {
    return FirebaseUserHelperIos()
}

class FirebaseUserHelperIos : FirebaseUserHelper {
    override suspend fun getCurrentUserProfile(): PublicUserProfile? {
        // Use KMP Firebase (e.g. gitlive dev.gitlive:firebase-auth)
        val user =  FIRAuth.auth().currentUser
        return user?.let {
            PublicUserProfile(it.uid, it.displayName, it.email)
        }
    }
    override suspend fun getAllUserStats(): List<UserStats> {
        val snapshot = Firebase.firestore.collection("users").get()
        return snapshot.documents.mapNotNull { doc ->
            UserStats(
                userId = doc.id,
                name = doc.data["name"] as? String ?: "(anon)",
                xp = (doc.data["xp"] as? Long)?.toInt() ?: 0,
                streak = (doc.data["streak"] as? Long)?.toInt() ?: 0
            )
        }
    }
}
