package com.dailydevchallenge.devstreaks.features.subscription

import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseSubscription(
    val planId: String = "",
    val isActive: Boolean = false,
    val startDate: Long = 0,
    val endDate: Long? = null,
    val isLifetime: Boolean = false,
    val paymentMethod: String? = null,
    val transactionId: String? = null,
    val purchaseToken: String? = null,
    val platform: String = "", // "android" or "ios"
    val createdAt: Long = Clock.System.now().toEpochMilliseconds()
)

class FirebaseSubscriptionRepository : SubscriptionRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val subscriptionsCollection = firestore.collection("subscriptions")
    private val usersCollection = firestore.collection("users")

    override suspend fun getCurrentSubscription(): Result<UserSubscription?> = withContext(Dispatchers.IO) {
        try {
            val userId = UserPreferences.getSafeUserId()
            val snapshot = subscriptionsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                return@withContext Result.success(null)
            }

            val doc = snapshot.documents.first()
            val firebaseSubscription = doc.toObject(FirebaseSubscription::class.java)
                ?: return@withContext Result.success(null)

            val userSubscription = UserSubscription(
                planId = firebaseSubscription.planId,
                isActive = firebaseSubscription.isActive,
                startDate = firebaseSubscription.startDate,
                endDate = firebaseSubscription.endDate,
                isLifetime = firebaseSubscription.isLifetime,
                paymentMethod = firebaseSubscription.paymentMethod,
                transactionId = firebaseSubscription.transactionId
            )

            Result.success(userSubscription)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get current subscription: ${e.message}"))
        }
    }

    override suspend fun verifyPurchase(planId: String, transactionId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = UserPreferences.getSafeUserId()
            val now = Clock.System.now().toEpochMilliseconds()

            // Determine subscription details based on plan
            val (isLifetime, endDate) = when (planId) {
                "devstreak_lifetime" -> true to null
                "devstreak_monthly" -> false to (now + (30L * 24 * 60 * 60 * 1000)) // 30 days
                else -> throw Exception("Unknown plan ID: $planId")
            }

            // Create subscription record
            val subscription = FirebaseSubscription(
                planId = planId,
                isActive = true,
                startDate = now,
                endDate = endDate,
                isLifetime = isLifetime,
                transactionId = transactionId,
                platform = getPlatform()
            )

            // Use batch to ensure atomicity
            val batch = firestore.batch()

            // Add subscription document
            val subscriptionRef = subscriptionsCollection.document()
            batch.set(subscriptionRef, subscription.copy().apply {
                // Add userId to the subscription document
                subscriptionRef.set(mapOf(
                    "userId" to userId,
                    "planId" to planId,
                    "isActive" to true,
                    "startDate" to now,
                    "endDate" to endDate,
                    "isLifetime" to isLifetime,
                    "transactionId" to transactionId,
                    "platform" to getPlatform(),
                    "createdAt" to FieldValue.serverTimestamp()
                ))
            })

            // Update user's subscription status
            val userRef = usersCollection.document(userId)
            batch.update(userRef, mapOf(
                "isPremium" to true,
                "subscriptionPlan" to planId,
                "subscriptionStart" to now,
                "subscriptionEnd" to endDate,
                "lastSubscriptionUpdate" to FieldValue.serverTimestamp()
            ))

            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to verify purchase: ${e.message}"))
        }
    }

    override suspend fun syncRestoredPurchases(transactionIds: List<String>): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val userId = UserPreferences.getSafeUserId()

            // Check each transaction ID against our records
            transactionIds.forEach { transactionId ->
                val existingSubscription = subscriptionsCollection
                    .whereEqualTo("transactionId", transactionId)
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                if (existingSubscription.isEmpty) {
                    // This is a new purchase that wasn't recorded
                    // You might want to verify with the platform store here
                    // For now, we'll create a lifetime subscription as fallback
                    verifyPurchase("devstreak_lifetime", transactionId)
                }
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to sync restored purchases: ${e.message}"))
        }
    }

    /**
     * Check if user has active premium subscription
     */
    suspend fun isPremiumUser(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val now = Clock.System.now().toEpochMilliseconds()

            val subscription = subscriptionsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            if (subscription.isEmpty) return@withContext false

            val sub = subscription.documents.first().toObject(FirebaseSubscription::class.java)
                ?: return@withContext false

            // Check if subscription is still valid
            val isValid = sub.isLifetime || (sub.endDate?.let { it > now } ?: false)

            // If subscription expired, mark as inactive
            if (!isValid && !sub.isLifetime) {
                subscription.documents.first().reference.update("isActive", false).await()
                return@withContext false
            }

            return@withContext isValid
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Cancel subscription
     */
    suspend fun cancelSubscription(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val activeSubscriptions = subscriptionsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val batch = firestore.batch()

            activeSubscriptions.documents.forEach { doc ->
                batch.update(doc.reference, "isActive", false)
            }

            // Update user record
            val userRef = usersCollection.document(userId)
            batch.update(userRef, mapOf(
                "isPremium" to false,
                "subscriptionCancelledAt" to FieldValue.serverTimestamp()
            ))

            batch.commit().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to cancel subscription: ${e.message}"))
        }
    }

    private fun getPlatform(): String {
        return try {
            val osName = System.getProperty("os.name")?.lowercase() ?: ""
            when {
                osName.contains("android") -> "android"
                osName.contains("ios") -> "ios"
                else -> "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
    }
}
