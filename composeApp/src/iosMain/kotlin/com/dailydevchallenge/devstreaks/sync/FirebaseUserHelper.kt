package com.dailydevchallenge.devstreaks.sync

import com.dailydevchallenge.devstreaks.features.feed.UserStats
import com.dailydevchallenge.devstreaks.model.User
import com.dailydevchallenge.devstreaks.repository.PublicUserProfile
import com.dailydevchallenge.devstreaks.utils.getLogger
import cocoapods.FirebaseAuth.FIRAuth
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.datetime.Clock

actual fun getPlatformFirebaseUserHelper(): FirebaseUserHelper {
    return FirebaseUserHelperIos()
}

class FirebaseUserHelperIos : FirebaseUserHelper {
    private val logger = { getLogger() }

    override suspend fun getCurrentUserProfile(): PublicUserProfile? {
        val user = FIRAuth.auth().currentUser
        return user?.let {
            PublicUserProfile(
                id = it.uid,
                displayName = it.displayName ?: "Unknown User",
                level = 1,
                totalXp = 0,
                streak = 0,
                badges = emptyList()
            )
        }
    }

    override suspend fun getAllUserStats(): List<UserStats> {
        return try {
            val snapshot = Firebase.firestore.collection("users").get()
            snapshot.documents.mapNotNull { doc ->
                UserStats(
                    userId = doc.id,
                    name = doc.data["name"] as? String ?: "(anon)",
                    xp = (doc.data["xp"] as? Long)?.toInt() ?: 0,
                    streak = (doc.data["streak"] as? Long)?.toInt() ?: 0,
                    logicScore = (doc.data["logicScore"] as? Long)?.toInt() ?: 0
                )
            }
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperIos", "Error getting all user stats: ${e.message}")
            emptyList()
        }
    }

    override suspend fun updateUserProgress(userId: String, xp: Long, streak: Long?) {
        try {
            val db = Firebase.firestore
            db.collection("users").document(userId).update(mapOf("xp" to xp, "streak" to (streak ?: 0)))
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperIos", "Error updating user progress: ${e.message}")
        }
    }

    override suspend fun fetchUserProgress(userId: String): UserStats? {
        return try {
            val doc = Firebase.firestore.collection("users").document(userId).get()
            if (doc.exists) {
                UserStats(
                    userId = doc.id,
                    name = doc.data["name"] as? String ?: "(anon)",
                    xp = (doc.data["xp"] as? Long)?.toInt() ?: 0,
                    streak = (doc.data["streak"] as? Long)?.toInt() ?: 0,
                    logicScore = (doc.data["logicScore"] as? Long)?.toInt() ?: 0
                )
            } else null
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperIos", "Error fetching user progress: ${e.message}")
            null
        }
    }

    override suspend fun getCurrentUserdata(userId: String, email: String): User {
        return try {
            val doc = Firebase.firestore.collection("users").document(userId).get()
            val name = doc.data["name"] as? String ?: "DevStreaker"
            val xp = (doc.data["xp"] as? Long)?.toInt() ?: 0
            val streak = (doc.data["streak"] as? Long)?.toInt() ?: 0
            val avatarUrl = doc.data["avatarUrl"] as? String
            val now = Clock.System.now().toEpochMilliseconds()

            User(
                userId = userId,
                email = email,
                passwordHash = "",
                username = name,
                avatarUrl = avatarUrl,
                createdAt = now,
                lastLogin = now,
                xp = xp,
                level = 1,
                dailyStreak = streak.toLong(),
                streakStartDate = null,
                preferences = emptyMap(),
                role = "user",
                badges = emptyList()
            )
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperIos", "Error getting current user data: ${e.message}")
            throw e
        }
    }

    override suspend fun updateUserInFirestore(user: User) {
        try {
            val userDoc = Firebase.firestore.collection("users").document(user.userId)
            val updateData = mapOf(
                "username" to user.username,
                "name" to user.username,
                "email" to user.email,
                "avatarUrl" to user.avatarUrl,
                "xp" to user.xp.toLong(),
                "level" to (user.level ?: 1),
                "streak" to (user.dailyStreak ?: 0),
                "lastLogin" to user.lastLogin,
                "role" to user.role
            )
            userDoc.update(updateData)
            logger().d("FirebaseUserHelperIos", "Successfully updated user ${user.userId} in Firestore")
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperIos", "Failed to update user in Firestore: ${e.message}")
            throw e
        }
    }

    // NEW: Implementation of getUserById method
    override suspend fun getUserById(userId: String): User? {
        return try {
            logger().d("FirebaseUserHelperIos", "getUserById: userId = $userId")
            val doc = Firebase.firestore.collection("users").document(userId).get()

            if (!doc.exists) {
                logger().w("FirebaseUserHelperIos", "getUserById: User document does not exist for userId = $userId")
                return null
            }

            createUserFromDocument(doc)
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperIos", "Error getting user by ID: ${e.message}")
            null
        }
    }

    // NEW: Implementation of updateUser method
    override suspend fun updateUser(user: User): Boolean {
        return try {
            logger().d("FirebaseUserHelperIos", "updateUser: userId = ${user.userId}")

            val userMap = mapOf(
                "name" to user.username,
                "email" to user.email,
                "avatarUrl" to user.avatarUrl,
                "xp" to user.xp,
                "level" to user.level,
                "streak" to user.dailyStreak,
                "lastLogin" to user.lastLogin,
                "preferences" to user.preferences,
                "role" to user.role,
                "badges" to user.badges
            )

            Firebase.firestore.collection("users").document(user.userId).set(userMap)
            logger().d("FirebaseUserHelperIos", "updateUser: Successfully updated user ${user.userId}")
            true
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperIos", "Error updating user: ${e.message}")
            false
        }
    }

    // Helper method to create User from document
    private fun createUserFromDocument(doc: dev.gitlive.firebase.firestore.DocumentSnapshot): User {
        return User(
            userId = doc.id,
            email = doc.data["email"] as? String ?: "",
            passwordHash = "",
            username = doc.data["name"] as? String ?: doc.data["username"] as? String ?: "User${doc.id.take(6)}",
            avatarUrl = doc.data["avatarUrl"] as? String,
            createdAt = (doc.data["createdAt"] as? Long) ?: System.currentTimeMillis(),
            lastLogin = System.currentTimeMillis(),
            xp = (doc.data["xp"] as? Long)?.toInt() ?: 0,
            level = (doc.data["level"] as? Long) ?: 1,
            dailyStreak = (doc.data["streak"] as? Long) ?: 0,
            streakStartDate = null,
            preferences = emptyMap(),
            role = "user",
            badges = emptyList()
        )
    }

    // Stub implementations for other required methods
    override suspend fun getTopUsersByXP(limit: Int): List<User> = emptyList()
    override suspend fun getTopUsersByWeeklyXP(startOfWeek: kotlinx.datetime.Instant, limit: Int): List<Pair<User, Int>> = emptyList()
    override suspend fun getTopUsersByMonthlyXP(startOfMonth: kotlinx.datetime.Instant, limit: Int): List<Pair<User, Int>> = emptyList()
    override suspend fun getTopUsersByStreak(limit: Int): List<User> = emptyList()
    override suspend fun getUserGlobalRank(userId: String): Int = -1
    override suspend fun getUserFriends(userId: String): List<User> = emptyList()
    override suspend fun addFriend(userId: String, friendUserId: String) {}
    override suspend fun removeFriend(userId: String, friendUserId: String) {}
    override suspend fun recordXpGain(userId: String, xpGained: Int, source: String) {}
    override suspend fun updateUserStats(userId: String, xpGained: Int, streakIncrement: Int) {}
    override suspend fun getUserProfile(userId: String): User = throw NotImplementedError("Use getUserById instead")
}
