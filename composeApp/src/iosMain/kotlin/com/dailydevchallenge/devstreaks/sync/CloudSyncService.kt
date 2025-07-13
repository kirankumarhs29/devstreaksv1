// shared/src/iosMain/kotlin/com/dailydevchallenge/devstreaks/sync/CloudSyncServiceImpl.kt

package com.dailydevchallenge.devstreaks.sync

import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import platform.Foundation.*
import kotlinx.cinterop.ExperimentalForeignApi
import cocoapods.FirebaseFirestore.* // Keep this for native access
import cocoapods.FirebaseFirestore.FIRFirestore
import cocoapods.FirebaseFirestore.FIRCollectionReference
import cocoapods.FirebaseFirestore.FIRDocumentReference

actual fun getCloudSyncService(): CloudSyncService = CloudSyncServiceIos()


@OptIn(ExperimentalForeignApi::class)
class CloudSyncServiceIos : CloudSyncService {

    // Correct way to get the native Firebase Firestore instance
    val db: FIRFirestore = FIRFirestore.firestore()


    private val dateFormatter = NSDateFormatter() // Still not used

    override suspend fun syncPushAll(
        profile: LearningProfile,
        xp: Int,
        streak: Int,
        lastCompletedDate: String?
    ) {
        // IMPORTANT: Use a robust userId, e.g., from Firebase Authentication
        val userId = "${profile.goal}_${profile.fear}" // Still problematic for real apps

        // Manually construct the data map with explicit casting to native types
        // This is complex for nested objects like LearningProfile itself.
        // For simplicity, I'm manually extracting primitive fields.
        val nativeDataMap: MutableMap<String, Any> = mutableMapOf(
            "goal" to (profile.goal as NSString),
            "skills" to (profile.skills.map { it as NSString } as NSArray), // List<String> to NSArray<NSString>
            "experience" to (profile.experience as NSString),
            "style" to (profile.style as NSString),
            "timePerDay" to (profile.timePerDay as NSNumber), // Assuming profile.timePerDay is Int
            "days" to (profile.days as NSNumber), // Assuming profile.days is Int
            "fear" to (profile.fear as NSString),
            "xp" to (xp as NSNumber), // Kotlin Int to NSNumber
            "streak" to (streak as NSNumber) // Kotlin Int to NSNumber
        )

        lastCompletedDate?.let {
            nativeDataMap["lastCompletedDate"] = it as NSString // Kotlin String to NSString
        }

        // Convert the Kotlin Map to NSDictionary for native Firebase SDK
        val firebaseData = nativeDataMap.toNSDictionary()

        val usersCollection: FIRCollectionReference = db.collectionWithPath("users")
        val userDocument: FIRDocumentReference = usersCollection.documentWithPath(userId)

        try {
            // ✅ Correct native Firebase API call: setData with NSDictionary
            userDocument.setData(data = firebaseData, merge = true)
            NSLog("iOS: Data synced to Firestore for user %@ successfully!", userId)
        } catch (e: Exception) {
            NSLog("iOS: Error syncing data to Firestore for user %@: %@", userId, e.message ?: "Unknown error")
            throw e
        }
    }
}

// This extension is now CRUCIAL and needs to handle all possible types
@OptIn(ExperimentalForeignApi::class)
fun Map<String, Any>.toNSDictionary(): NSDictionary {
    val dict = NSMutableDictionary()
    this.forEach { (key, value) ->
        val nsKey = key as NSString
        val nsValue: Any = when (value) {
            is String -> value as NSString
            is Int -> value as NSNumber
            is Boolean -> value as NSNumber
            is Double -> value as NSNumber
            is Float -> value as NSNumber
            is List<*> -> value.map { // Handle lists recursively if they contain complex types
                when(it) {
                    is String -> it as NSString
                    is Int -> it as NSNumber
                    // Add other types as needed
                    else -> it as Any // Fallback for types not explicitly handled
                }
            } as NSArray
            is Map<*, *> -> (value as Map<String, Any>).toNSDictionary() // Recursive call for nested maps
            // Add other type conversions (e.g., NSDate for Date objects)
            else -> value as Any // Be cautious with 'as Any' for unknown types
        }
        dict.setObject(nsValue, forKey = nsKey)
    }
    return dict
}