package com.dailydevchallenge.devstreaks.features.subscription

import com.dailydevchallenge.devstreaks.utils.getLogger

class IosBillingService : BillingService {

    private val logger = getLogger()

    override suspend fun purchasePlan(plan: SubscriptionPlan): Result<String> {
        return try {
            // TODO: Implement App Store StoreKit integration
            logger.d("IosBillingService: Purchase plan: ${plan.id}")
            Result.success("mock_transaction_id_${plan.id}")
        } catch (e: Exception) {
            logger.e("IosBillingService: Failed to purchase plan: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun restorePurchases(): Result<List<String>> {
        return try {
            // TODO: Implement App Store restore purchases
            logger.d("IosBillingService: Restore purchases")
            Result.success(emptyList())
        } catch (e: Exception) {
            logger.e("IosBillingService: Failed to restore purchases: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun checkSubscriptionStatus(): Result<Boolean> {
        return try {
            // TODO: Implement App Store subscription status check
            logger.d("IosBillingService: Check subscription status")
            Result.success(false)
        } catch (e: Exception) {
            logger.e("IosBillingService: Failed to check subscription status: ${e.message}")
            Result.failure(e)
        }
    }
}
