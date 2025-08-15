package com.dailydevchallenge.devstreaks.sync

import com.dailydevchallenge.devstreaks.features.feed.UserStats
import com.dailydevchallenge.devstreaks.model.User
import com.dailydevchallenge.devstreaks.repository.PublicUserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime

actual fun getPlatformFirebaseUserHelper(): FirebaseUserHelper {
    return FirebaseUserHelperAndroid()
}

class FirebaseUserHelperAndroid : FirebaseUserHelper {
    val logger  = { getLogger()}
    override suspend fun getCurrentUserProfile(): PublicUserProfile? {
        val user = FirebaseAuth.getInstance().currentUser
        logger().d("FirebaseUserHelperAndroid", "getCurrentUserProfile: user = $user")
        return user?.let {
            PublicUserProfile(
                id = it.uid,
                displayName = it.displayName ?: "Unknown User",
                level = 1, // Default level, should be fetched from Firestore
                totalXp = 0, // Default XP, should be fetched from Firestore
                streak = 0, // Default streak, should be fetched from Firestore
                badges = emptyList() // Default badges, should be fetched from Firestore
            )
        }
    }
    override suspend fun getAllUserStats(): List<UserStats> {
            val snapshot = FirebaseFirestore.getInstance().collection("users").get().await()
            logger().d("FirebaseUserHelperAndroid", "getAllUserStats: snapshot = $snapshot")
            return snapshot.documents.mapNotNull { doc ->
                if (!doc.exists()) {
                    logger().w("FirebaseUserHelperAndroid", "getAllUserStats: Document ${doc.id} does not exist")
                    return@mapNotNull null
                }
                if (doc.getString("name") == null) {
                    logger().w("FirebaseUserHelperAndroid", "getAllUserStats: Document ${doc.id} has no name")
                }

                if (doc.getLong("xp") == null) {
                    logger().w("FirebaseUserHelperAndroid", "getAllUserStats: Document ${doc.id} has no xp")
                }
                if (doc.getLong("streak") == null) {
                    logger().w("FirebaseUserHelperAndroid", "getAllUserStats: Document ${doc.id} has no streak")
                }
                // Create UserStats object from document
                logger().d("FirebaseUserHelperAndroid", "getAllUserStats: Document ${doc.id} exists with name=${doc.getString("name")}, xp=${doc.getLong("xp")}, streak=${doc.getLong("streak")}")
                UserStats(
                    userId = doc.id,
                    name = doc.getString("name") ?: "(anon)",
                    xp = doc.getLong("xp")?.toInt() ?: 0,
                    streak = doc.getLong("streak")?.toInt() ?: 0,
                    logicScore = doc.getLong("logicScore")?.toInt() ?: 0,
                )
            }
        }
        override suspend fun updateUserProgress(userId: String, xp: Long, streak: Long?) {
            val db = FirebaseFirestore.getInstance()
            logger().d("FirebaseUserHelperAndroid", "updateUserProgress: userId = $userId, xp = $xp, streak = $streak")
            val userDoc = db.collection("users").document(userId)

            // Use set with merge instead of update to handle new users
            val userData = mutableMapOf<String, Any>(
                "xp" to xp,
                "streak" to (streak ?: 0)
            )

            // Add default fields for new users
            userData["name"] = userId.substringBefore("@") // Use email prefix as default name
            userData["logicScore"] = 0

            userDoc.set(userData, com.google.firebase.firestore.SetOptions.merge()).await()
            logger().d("FirebaseUserHelperAndroid", "updateUserProgress: Updated user $userId with xp=$xp, streak=$streak")
        }
        override suspend fun fetchUserProgress(userId: String): UserStats? {
            val doc = FirebaseFirestore.getInstance().collection("users").document(userId).get().await()
            logger().d("FirebaseUserHelperAndroid", "fetchUserProgress: userId = $userId, doc = $doc")
            logger().d("FirebaseUserHelperAndroid", "fetchUserProgress: Document exists = ${doc.exists()}")
            logger().d("FirebaseUserHelperAndroid", "fetchUserProgress: Document data = ${doc.data}")
            logger().d("FirebaseUserHelperAndroid", "fetchUserProgress: Document id = ${doc.id}")
            logger().d("FirebaseUserHelperAndroid", "fetchUserProgress: Document name = ${doc.getString("name")}")
            logger().d("FirebaseUserHelperAndroid", "fetchUserProgress: Document xp = ${doc.getLong("xp")}")
            logger().d("FirebaseUserHelperAndroid", "fetchUserProgress: Document streak = ${doc.getLong("streak")}")
            return if (doc.exists()) {
                UserStats(
                    userId = doc.id,
                    name = doc.getString("name") ?: "(anon)",
                    xp = doc.getLong("xp")?.toInt() ?: 0,
                    streak = doc.getLong("streak")?.toInt() ?: 0,
                    logicScore = doc.getLong("logicScore")?.toInt() ?: 0
                )
            } else null
        }

    override suspend fun getCurrentUserdata(userId: String, email: String): User {
        val db = FirebaseFirestore.getInstance()
        logger().d("FirebaseUserHelperAndroid", "getCurrentUserdata: userId = $userId, email = $email")
        val doc = db.collection("users").document(userId).get().await()
        val name = doc.getString("name") ?: "DevStreaker"
        val xp = doc.getLong("xp")?.toInt() ?: 0
        val streak = doc.getLong("streak")?.toInt() ?: 0
        val avatarUrl = doc.getString("avatarUrl") // FIX: Actually use avatarUrl from Firestore
        val now = Clock.System.now().toEpochMilliseconds()
        return User(
            userId = userId,
            email = email,
            passwordHash = "", // Not stored in Firestore
            username = name,
            avatarUrl = avatarUrl, // FIX: Use the actual avatarUrl
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
    }

    // Helper method to reduce code duplication
    private fun createUserFromDocument(doc: com.google.firebase.firestore.DocumentSnapshot): User {
        return User(
            userId = doc.id,
            email = doc.getString("email") ?: "",
            passwordHash = "",
            username = doc.getString("name") ?: doc.getString("username") ?: "User${doc.id.take(6)}",
            avatarUrl = doc.getString("avatarUrl"),
            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
            lastLogin = System.currentTimeMillis(),
            xp = doc.getLong("xp")?.toInt() ?: 0,
            level = doc.getLong("level") ?: 1,
            dailyStreak = doc.getLong("streak") ?: 0,
            streakStartDate = null,
            preferences = emptyMap(),
            role = "user",
            badges = emptyList()
        )
    }

    // Enhanced methods for leaderboards and social features
    override suspend fun getTopUsersByXP(limit: Int): List<User> {
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .orderBy("xp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            logger().d("FirebaseUserHelperAndroid", "getTopUsersByXP: Found ${snapshot.size()} users")

            snapshot.documents.mapNotNull { doc ->
                try {
                    User(
                        userId = doc.id,
                        email = doc.getString("email") ?: "",
                        passwordHash = "",
                        username = doc.getString("name") ?: doc.getString("username") ?: "User${doc.id.take(6)}",
                        avatarUrl = doc.getString("avatarUrl"),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        lastLogin = System.currentTimeMillis(),
                        xp = doc.getLong("xp")?.toInt() ?: 0,
                        level = doc.getLong("level") ?: 1,
                        dailyStreak = doc.getLong("streak") ?: 0,
                        streakStartDate = null,
                        preferences = emptyMap(),
                        role = "user",
                        badges = emptyList()
                    )
                } catch (e: Exception) {
                    logger().e("FirebaseUserHelperAndroid", e,"Error converting document ${doc
                        .id} to User: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperAndroid", e,"Failed to fetch top users by XP: ${e
                .message}")
            emptyList()
        }
    }

    override suspend fun getTopUsersByWeeklyXP(startOfWeek: kotlinx.datetime.Instant, limit: Int): List<Pair<User, Int>> {
        return try {
            // Get weekly stats from user_stats collection
            val weeklyStatsSnapshot = FirebaseFirestore.getInstance()
                .collection("user_stats")
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

            // Get user details for top performers - OPTIMIZED
            val topUserIds = weeklyXpMap.entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key }

            // Batch fetch users instead of individual queries
            val users = if (topUserIds.isNotEmpty()) {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereIn("__name__", topUserIds) // Batch query by document IDs
                    .get()
                    .await()
                    .documents
                    .mapNotNull { userDoc ->
                        try {
                            val user = createUserFromDocument(userDoc)
                            user to (weeklyXpMap[user.userId] ?: 0)
                        } catch (e: Exception) {
                            logger().e("FirebaseUserHelperAndroid", e,"Error creating user from doc: ${e.message}")
                            null
                        }
                    }
            } else {
                emptyList()
            }

            users
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperAndroid", e,"Failed to fetch weekly leaderboard: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getTopUsersByMonthlyXP(startOfMonth: kotlinx.datetime.Instant, limit: Int): List<Pair<User, Int>> {
        return try {
            // Similar implementation to weekly, but for monthly timeframe
            val monthlyStatsSnapshot = FirebaseFirestore.getInstance()
                .collection("user_stats")
                .whereGreaterThanOrEqualTo("timestamp", startOfMonth.toEpochMilliseconds())
                .get()
                .await()

            val monthlyXpMap = mutableMapOf<String, Int>()
            monthlyStatsSnapshot.documents.forEach { doc ->
                val userId = doc.getString("userId") ?: return@forEach
                val xpGained = doc.getLong("xpGained")?.toInt() ?: 0
                monthlyXpMap[userId] = (monthlyXpMap[userId] ?: 0) + xpGained
            }

            val topUserIds = monthlyXpMap.entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key }

            val users = mutableListOf<Pair<User, Int>>()
            topUserIds.forEach { userId ->
                try {
                    val userDoc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .get()
                        .await()

                    if (userDoc.exists()) {
                        val user = User(
                            userId = userId,
                            email = userDoc.getString("email") ?: "",
                            passwordHash = "",
                            username = userDoc.getString("name") ?: "User${userId.take(6)}",
                            avatarUrl = userDoc.getString("avatarUrl"),
                            createdAt = userDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                            lastLogin = System.currentTimeMillis(),
                            xp = userDoc.getLong("xp")?.toInt() ?: 0,
                            level = userDoc.getLong("level") ?: 1,
                            dailyStreak = userDoc.getLong("streak") ?: 0,
                            streakStartDate = null,
                            preferences = emptyMap(),
                            role = "user",
                            badges = emptyList()
                        )
                        users.add(user to (monthlyXpMap[userId] ?: 0))
                    }
                } catch (e: Exception) {
                    logger().e("FirebaseUserHelperAndroid", e,"Error fetching user $userId: ${e
                        .message}")
                }
            }

            users
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperAndroid", e,"Failed to fetch monthly leaderboard: ${e
                .message}")
            emptyList()
        }
    }

    override suspend fun getTopUsersByStreak(limit: Int): List<User> {
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .orderBy("streak", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    User(
                        userId = doc.id,
                        email = doc.getString("email") ?: "",
                        passwordHash = "",
                        username = doc.getString("name") ?: "User${doc.id.take(6)}",
                        avatarUrl = doc.getString("avatarUrl"),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        lastLogin = System.currentTimeMillis(),
                        xp = doc.getLong("xp")?.toInt() ?: 0,
                        level = doc.getLong("level") ?: 1,
                        dailyStreak = doc.getLong("streak") ?: 0,
                        streakStartDate = null,
                        preferences = emptyMap(),
                        role = "user",
                        badges = emptyList()
                    )
                } catch (e: Exception) {
                    logger().e("FirebaseUserHelperAndroid", e,"Error converting document ${doc
                        .id} to User: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperAndroid", e,"Failed to fetch top users by streak: ${e
                .message}")
            emptyList()
        }
    }

    override suspend fun getUserGlobalRank(userId: String): Int {
        return try {
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()

            val userXp = userDoc.getLong("xp") ?: 0

            val higherRankedUsers = FirebaseFirestore.getInstance()
                .collection("users")
                .whereGreaterThan("xp", userXp)
                .get()
                .await()

            higherRankedUsers.size() + 1
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperAndroid", e,"Failed to get user rank: ${e.message}")
            -1
        }
    }

    override suspend fun getUserFriends(userId: String): List<User> {
        return try {
            val friendsSnapshot = FirebaseFirestore.getInstance()
                .collection("friends")
                .document(userId)
                .collection("friends")
                .get()
                .await()

            val friends = mutableListOf<User>()
            friendsSnapshot.documents.forEach { friendDoc ->
                try {
                    val friendId = friendDoc.id
                    val userDoc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(friendId)
                        .get()
                        .await()

                    if (userDoc.exists()) {
                        val friend = User(
                            userId = friendId,
                            email = userDoc.getString("email") ?: "",
                            passwordHash = "",
                            username = userDoc.getString("name") ?: "User${friendId.take(6)}",
                            avatarUrl = userDoc.getString("avatarUrl"),
                            createdAt = userDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                            lastLogin = System.currentTimeMillis(),
                            xp = userDoc.getLong("xp")?.toInt() ?: 0,
                            level = userDoc.getLong("level") ?: 1,
                            dailyStreak = userDoc.getLong("streak") ?: 0,
                            streakStartDate = null,
                            preferences = emptyMap(),
                            role = "user",
                            badges = emptyList()
                        )
                        friends.add(friend)
                    }
                } catch (e: Exception) {
                    logger().e("FirebaseUserHelperAndroid", e,"Error fetching friend ${friendDoc
                        .id}: ${e.message}")
                }
            }

            friends
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperAndroid", e,"Failed to get user friends: ${e.message}")
            emptyList()
        }
    }

    override suspend fun addFriend(userId: String, friendUserId: String) {
        try {
            val batch = FirebaseFirestore.getInstance().batch()

            // Add friend to user's friends list
            val userFriendRef = FirebaseFirestore.getInstance()
                .collection("friends")
                .document(userId)
                .collection("friends")
                .document(friendUserId)
            batch.set(userFriendRef, mapOf("addedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))

            // Add user to friend's friends list (mutual friendship)
            val friendUserRef = FirebaseFirestore.getInstance()
                .collection("friends")
                .document(friendUserId)
                .collection("friends")
                .document(userId)
            batch.set(friendUserRef, mapOf("addedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))

            batch.commit().await()
            logger().d("FirebaseUserHelperAndroid", "Successfully added friend $friendUserId to user $userId")
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperAndroid", e,"Failed to add friend: ${e.message}")
            throw e
        }
    }

    override suspend fun removeFriend(userId: String, friendUserId: String) {
        try {
            val batch = FirebaseFirestore.getInstance().batch()

            // Remove friend from user's friends list
            val userFriendRef = FirebaseFirestore.getInstance()
                .collection("friends")
                .document(userId)
                .collection("friends")
                .document(friendUserId)
            batch.delete(userFriendRef)

            // Remove user from friend's friends list
            val friendUserRef = FirebaseFirestore.getInstance()
                .collection("friends")
                .document(friendUserId)
                .collection("friends")
                .document(userId)
            batch.delete(friendUserRef)

            batch.commit().await()
            logger().d("FirebaseUserHelperAndroid", "Successfully removed friend $friendUserId from user $userId")
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperAndroid", e,"Failed to remove friend: ${e.message}")
            throw e
        }
    }

    override suspend fun recordXpGain(userId: String, xpGained: Int, source: String) {
        try {
            val statEntry = mapOf(
                "userId" to userId,
                "xpGained" to xpGained,
                "source" to source,
                "timestamp" to Clock.System.now().toEpochMilliseconds(),
                "date" to Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                    .date.toString()
            )

            FirebaseFirestore.getInstance()
                .collection("user_stats")
                .add(statEntry)
                .await()

            logger().d("FirebaseUserHelperAndroid", "Recorded XP gain: userId=$userId, xp=$xpGained, source=$source")
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperAndroid", e,"Failed to record XP gain: ${e.message}")
            throw e
        }
    }

    override suspend fun updateUserStats(userId: String, xpGained: Int, streakIncrement: Int) {
        try {
            val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)
            val updates = mutableMapOf<String, Any>(
                "xp" to com.google.firebase.firestore.FieldValue.increment(xpGained.toLong()),
                "lastActivity" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            if (streakIncrement > 0) {
                updates["streak"] = com.google.firebase.firestore.FieldValue.increment(streakIncrement.toLong())
                updates["streakStartDate"] = Clock.System.now().toEpochMilliseconds()
            }

            userRef.update(updates).await()

            // Record the XP gain for analytics
            recordXpGain(userId, xpGained, "challenge")

            logger().d("FirebaseUserHelperAndroid", "Updated user stats: userId=$userId, xp=$xpGained, streak=$streakIncrement")
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperAndroid", e,"Failed to update user stats: ${e.message}")
            throw e
        }
    }

    override suspend fun getUserProfile(userId: String): User {
        return try {
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) {
                throw Exception("User not found")
            }

            User(
                userId = userId,
                email = userDoc.getString("email") ?: "",
                passwordHash = "",
                username = userDoc.getString("name") ?: userDoc.getString("username") ?: "User${userId.take(6)}",
                avatarUrl = userDoc.getString("avatarUrl"),
                createdAt = userDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                lastLogin = System.currentTimeMillis(),
                xp = userDoc.getLong("xp")?.toInt() ?: 0,
                level = userDoc.getLong("level") ?: 1,
                dailyStreak = userDoc.getLong("streak") ?: 0,
                streakStartDate = null,
                preferences = emptyMap(),
                role = "user",
                badges = emptyList()
            )
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperAndroid", e,"Failed to get user profile: ${e.message}")
            throw e
        }
    }

    override suspend fun updateUserInFirestore(user: User) {
        try {
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.userId)

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

            userDoc.update(updateData).await()
            logger().d("FirebaseUserHelperAndroid", "Successfully updated user ${user.userId} in Firestore")
        } catch (e: Exception) {
            logger().e("FirebaseUserHelperAndroid", e, "Failed to update user in Firestore: ${e.message}")
            throw e
        }
    }
}
