package com.dailydevchallenge.devstreaks.sync

import com.dailydevchallenge.devstreaks.features.leaderboard.LeaderboardEntry
import com.dailydevchallenge.devstreaks.model.User
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Enhanced Firebase User Helper with leaderboard and social features
 */
class EnhancedFirebaseUserHelper {
    private val firestore = FirebaseFirestore.getInstance()

    // Collections
    private val usersCollection = firestore.collection("users")
    private val leaderboardsCollection = firestore.collection("leaderboards")
    private val userStatsCollection = firestore.collection("user_stats")
    private val friendsCollection = firestore.collection("friends")

    // ==================== LEADERBOARD METHODS ====================

    /**
     * Get top users by total XP for global leaderboard
     */
    suspend fun getTopUsersByXP(limit: Int = 100): List<User> = withContext(Dispatchers.IO) {
        try {
            val snapshot = usersCollection
                .orderBy("xp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            return@withContext snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(userId = doc.id)
            }
        } catch (e: Exception) {
            throw Exception("Failed to fetch top users by XP: ${e.message}")
        }
    }

    /**
     * Get top users by weekly XP
     */
    suspend fun getTopUsersByWeeklyXP(startOfWeek: Instant, limit: Int = 100): List<Pair<User, Int>> = withContext(Dispatchers.IO) {
        try {
            val weeklyStatsSnapshot = userStatsCollection
                .whereGreaterThanOrEqualTo("timestamp", startOfWeek.toEpochMilliseconds())
                .get()
                .await()

            // Group by user and sum weekly XP
            val weeklyXpMap = mutableMapOf<String, Int>()
            weeklyStatsSnapshot.documents.forEach { doc ->
                val userId = doc.getString("userId") ?: return@forEach
                val xpGained = doc.getLong("xpGained")?.toInt() ?: 0
                weeklyXpMap[userId] = (weeklyXpMap[userId] ?: 0) + xpGained
            }

            // Get user details for top performers
            val topUserIds = weeklyXpMap.entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key }

            val users = mutableListOf<Pair<User, Int>>()
            topUserIds.forEach { userId ->
                val userDoc = usersCollection.document(userId).get().await()
                val user = userDoc.toObject(User::class.java)?.copy(userId = userId)
                if (user != null) {
                    users.add(user to (weeklyXpMap[userId] ?: 0))
                }
            }

            return@withContext users
        } catch (e: Exception) {
            throw Exception("Failed to fetch weekly leaderboard: ${e.message}")
        }
    }

    /**
     * Get top users by monthly XP
     */
    suspend fun getTopUsersByMonthlyXP(startOfMonth: Instant, limit: Int = 100): List<Pair<User, Int>> = withContext(Dispatchers.IO) {
        try {
            val monthlyStatsSnapshot = userStatsCollection
                .whereGreaterThanOrEqualTo("timestamp", startOfMonth.toEpochMilliseconds())
                .get()
                .await()

            // Group by user and sum monthly XP
            val monthlyXpMap = mutableMapOf<String, Int>()
            monthlyStatsSnapshot.documents.forEach { doc ->
                val userId = doc.getString("userId") ?: return@forEach
                val xpGained = doc.getLong("xpGained")?.toInt() ?: 0
                monthlyXpMap[userId] = (monthlyXpMap[userId] ?: 0) + xpGained
            }

            // Get user details for top performers
            val topUserIds = monthlyXpMap.entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key }

            val users = mutableListOf<Pair<User, Int>>()
            topUserIds.forEach { userId ->
                val userDoc = usersCollection.document(userId).get().await()
                val user = userDoc.toObject(User::class.java)?.copy(userId = userId)
                if (user != null) {
                    users.add(user to (monthlyXpMap[userId] ?: 0))
                }
            }

            return@withContext users
        } catch (e: Exception) {
            throw Exception("Failed to fetch monthly leaderboard: ${e.message}")
        }
    }

    /**
     * Get top users by streak
     */
    suspend fun getTopUsersByStreak(limit: Int = 100): List<User> = withContext(Dispatchers.IO) {
        try {
            val snapshot = usersCollection
                .orderBy("dailyStreak", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            return@withContext snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(userId = doc.id)
            }
        } catch (e: Exception) {
            throw Exception("Failed to fetch top users by streak: ${e.message}")
        }
    }

    /**
     * Get user's global rank
     */
    suspend fun getUserGlobalRank(userId: String): Int = withContext(Dispatchers.IO) {
        try {
            val userDoc = usersCollection.document(userId).get().await()
            val userXp = userDoc.getLong("xp") ?: 0

            val higherRankedUsers = usersCollection
                .whereGreaterThan("xp", userXp)
                .get()
                .await()

            return@withContext higherRankedUsers.size() + 1
        } catch (e: Exception) {
            throw Exception("Failed to get user rank: ${e.message}")
        }
    }

    // ==================== SOCIAL FEATURES ====================

    /**
     * Get user's friends list
     */
    suspend fun getUserFriends(userId: String): List<User> = withContext(Dispatchers.IO) {
        try {
            val friendsSnapshot = friendsCollection
                .document(userId)
                .collection("friends")
                .get()
                .await()

            val friendIds = friendsSnapshot.documents.map { it.id }
            val friends = mutableListOf<User>()

            friendIds.forEach { friendId ->
                val friendDoc = usersCollection.document(friendId).get().await()
                val friend = friendDoc.toObject(User::class.java)?.copy(userId = friendId)
                if (friend != null) {
                    friends.add(friend)
                }
            }

            return@withContext friends
        } catch (e: Exception) {
            throw Exception("Failed to get user friends: ${e.message}")
        }
    }

    /**
     * Add a friend
     */
    suspend fun addFriend(userId: String, friendUserId: String) = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()

            // Add friend to user's friends list
            val userFriendRef = friendsCollection
                .document(userId)
                .collection("friends")
                .document(friendUserId)
            batch.set(userFriendRef, mapOf("addedAt" to FieldValue.serverTimestamp()))

            // Add user to friend's friends list (mutual friendship)
            val friendUserRef = friendsCollection
                .document(friendUserId)
                .collection("friends")
                .document(userId)
            batch.set(friendUserRef, mapOf("addedAt" to FieldValue.serverTimestamp()))

            batch.commit().await()
        } catch (e: Exception) {
            throw Exception("Failed to add friend: ${e.message}")
        }
    }

    /**
     * Remove a friend
     */
    suspend fun removeFriend(userId: String, friendUserId: String) = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()

            // Remove friend from user's friends list
            val userFriendRef = friendsCollection
                .document(userId)
                .collection("friends")
                .document(friendUserId)
            batch.delete(userFriendRef)

            // Remove user from friend's friends list
            val friendUserRef = friendsCollection
                .document(friendUserId)
                .collection("friends")
                .document(userId)
            batch.delete(friendUserRef)

            batch.commit().await()
        } catch (e: Exception) {
            throw Exception("Failed to remove friend: ${e.message}")
        }
    }

    // ==================== USER STATS TRACKING ====================

    /**
     * Record XP gain for analytics and leaderboards
     */
    suspend fun recordXpGain(userId: String, xpGained: Int, source: String) = withContext(Dispatchers.IO) {
        try {
            val statEntry = mapOf(
                "userId" to userId,
                "xpGained" to xpGained,
                "source" to source, // "challenge", "bonus", "streak", etc.
                "timestamp" to Clock.System.now().toEpochMilliseconds(),
                "date" to Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
            )

            userStatsCollection.add(statEntry).await()
        } catch (e: Exception) {
            throw Exception("Failed to record XP gain: ${e.message}")
        }
    }

    /**
     * Update user's total stats
     */
    suspend fun updateUserStats(userId: String, xpGained: Int, streakIncrement: Int = 0) = withContext(Dispatchers.IO) {
        try {
            val userRef = usersCollection.document(userId)
            val updates = mutableMapOf<String, Any>(
                "xp" to FieldValue.increment(xpGained.toLong()),
                "lastActivity" to FieldValue.serverTimestamp()
            )

            if (streakIncrement > 0) {
                updates["dailyStreak"] = FieldValue.increment(streakIncrement.toLong())
                updates["streakStartDate"] = Clock.System.now().toEpochMilliseconds()
            }

            userRef.update(updates).await()

            // Record the XP gain for analytics
            recordXpGain(userId, xpGained, "challenge")

        } catch (e: Exception) {
            throw Exception("Failed to update user stats: ${e.message}")
        }
    }

    /**
     * Get user profile
     */
    suspend fun getUserProfile(userId: String): User = withContext(Dispatchers.IO) {
        try {
            val userDoc = usersCollection.document(userId).get().await()
            return@withContext userDoc.toObject(User::class.java)?.copy(userId = userId)
                ?: throw Exception("User not found")
        } catch (e: Exception) {
            throw Exception("Failed to get user profile: ${e.message}")
        }
    }
}
