package com.dailydevchallenge.devstreaks.sync

import com.dailydevchallenge.devstreaks.features.feed.UserStats
import com.dailydevchallenge.devstreaks.repository.PublicUserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.lifecycle.viewModelScope
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
}
