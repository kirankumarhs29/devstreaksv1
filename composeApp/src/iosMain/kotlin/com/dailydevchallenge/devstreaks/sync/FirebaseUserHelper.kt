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
    override suspend fun updateUserProgress(userId: String, xp: Long, streak: Long?) {
        val db = Firebase.firestore
        db.collection("users").document(userId).update(mapOf("xp" to xp, "streak" to streak))
    }
    override suspend fun fetchUserProgress(userId: String): UserStats? {
        val doc = Firebase.firestore.collection("users").document(userId).get()
        return if (doc.exists) {
            UserStats(
                userId = doc.id,
                name = doc.data["name"] as? String ?: "(anon)",
                xp = (doc.data["xp"] as? Long)?.toInt() ?: 0,
                streak = (doc.data["streak"] as? Long)?.toInt() ?: 0
            )
        } else null
    }

    override suspend fun updateUserInFirestore(user: User) {
        try {
            val userDoc = Firebase.firestore.collection("users").document(user.userId)

            val updateData = mapOf(
                "username" to user.username,
                "name" to user.username, // Keep both for compatibility
                "email" to user.email,
                "avatarUrl" to user.avatarUrl,
                "xp" to user.xp.toLong(),
                "level" to (user.level ?: 1),
                "streak" to (user.dailyStreak ?: 0),
                "lastLogin" to user.lastLogin,
                "role" to user.role
            )

            userDoc.update(updateData)
            println("iOS: Successfully updated user ${user.userId} in Firestore")
        } catch (e: Exception) {
            println("iOS: Failed to update user in Firestore: ${e.message}")
            throw e
        }
    }
}
