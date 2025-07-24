package com.dailydevchallenge.devstreaks.sync

import com.dailydevchallenge.devstreaks.features.feed.UserStats
import com.dailydevchallenge.devstreaks.repository.PublicUserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.dailydevchallenge.devstreaks.utils.getLogger

actual fun getPlatformFirebaseUserHelper(): FirebaseUserHelper {
    return FirebaseUserHelperAndroid()
}

class FirebaseUserHelperAndroid : FirebaseUserHelper {
    val logger  = { getLogger()}
    override suspend fun getCurrentUserProfile(): PublicUserProfile? {
        val user = FirebaseAuth.getInstance().currentUser
        logger().d("FirebaseUserHelperAndroid", "getCurrentUserProfile: user = $user")
        return user?.let {
            PublicUserProfile(it.uid, it.displayName, it.email)
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
                    streak = doc.getLong("streak")?.toInt() ?: 0
                )
            }
        }
        override suspend fun updateUserProgress(userId: String, xp: Int, streak: Long?) {
            val db = FirebaseFirestore.getInstance()
            logger().d("FirebaseUserHelperAndroid", "updateUserProgress: userId = $userId, xp = $xp, streak = $streak")
            val userDoc = db.collection("users").document(userId)
            userDoc.update(mapOf("xp" to xp, "streak" to streak)).await()
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
                    streak = doc.getLong("streak")?.toInt() ?: 0
                )
            } else null
        }
}
