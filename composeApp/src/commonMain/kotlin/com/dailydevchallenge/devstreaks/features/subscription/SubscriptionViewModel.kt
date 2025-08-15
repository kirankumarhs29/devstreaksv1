package com.dailydevchallenge.devstreaks.features.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionPlan(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    val priceAmount: Double,
    val currency: String = "INR",
    val duration: String, // "monthly", "lifetime"
    val features: List<String>,
    val isPopular: Boolean = false,
    val discountPercentage: Int? = null
)

@Serializable
data class UserSubscription(
    val planId: String,
    val isActive: Boolean,
    val startDate: Long,
    val endDate: Long?,
    val isLifetime: Boolean,
    val paymentMethod: String?,
    val transactionId: String?
)

data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val plans: List<SubscriptionPlan> = emptyList(),
    val currentSubscription: UserSubscription? = null,
    val isPurchasing: Boolean = false,
    val purchaseSuccess: Boolean = false,
    val showPaywall: Boolean = false
)

class SubscriptionViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val billingService: BillingService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    private val logger = getLogger()

    init {
        loadSubscriptionData()
    }

    private fun loadSubscriptionData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Load available plans
                val plans = getAvailablePlans()

                // Load user's current subscription
                val currentSubscription = subscriptionRepository.getCurrentSubscription()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    plans = plans,
                    currentSubscription = currentSubscription.getOrNull(),
                    error = null
                )

                logger.d("Subscription data loaded successfully")

            } catch (e: Exception) {
                logger.e("Failed to load subscription data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load subscription data: ${e.message}"
                )
            }
        }
    }

    fun purchaseSubscription(planId: String) {
        val plan = _uiState.value.plans.find { it.id == planId }
        if (plan == null) {
            _uiState.value = _uiState.value.copy(error = "Plan not found")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isPurchasing = true, error = null)

            try {
                val purchaseResult = billingService.purchasePlan(plan)

                purchaseResult.fold(
                    onSuccess = { transactionId ->
                        // Verify purchase with backend
                        subscriptionRepository.verifyPurchase(planId, transactionId)

                        // Reload subscription data
                        val updatedSubscription = subscriptionRepository.getCurrentSubscription()

                        _uiState.value = _uiState.value.copy(
                            isPurchasing = false,
                            purchaseSuccess = true,
                            currentSubscription = updatedSubscription.getOrNull(),
                            error = null
                        )

                        logger.d("Purchase successful: $transactionId")
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isPurchasing = false,
                            error = "Purchase failed: ${error.message}"
                        )
                        logger.e("Purchase failed", error)
                    }
                )

            } catch (e: Exception) {
                logger.e("Purchase error", e)
                _uiState.value = _uiState.value.copy(
                    isPurchasing = false,
                    error = "Purchase error: ${e.message}"
                )
            }
        }
    }

    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val restoreResult = billingService.restorePurchases()

                restoreResult.fold(
                    onSuccess = { restoredPurchases ->
                        if (restoredPurchases.isNotEmpty()) {
                            // Sync with backend
                            subscriptionRepository.syncRestoredPurchases(restoredPurchases)

                            // Reload subscription data
                            loadSubscriptionData()

                            logger.d("Purchases restored: ${restoredPurchases.size}")
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "No purchases found to restore"
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to restore purchases: ${error.message}"
                        )
                        logger.e("Restore failed", error)
                    }
                )

            } catch (e: Exception) {
                logger.e("Restore error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Restore error: ${e.message}"
                )
            }
        }
    }

    fun showPaywall() {
        _uiState.value = _uiState.value.copy(showPaywall = true)
    }

    fun hidePaywall() {
        _uiState.value = _uiState.value.copy(showPaywall = false)
    }

    fun clearPurchaseSuccess() {
        _uiState.value = _uiState.value.copy(purchaseSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun isFeatureLocked(feature: String): Boolean {
        val currentSubscription = _uiState.value.currentSubscription
        return currentSubscription?.isActive != true
    }

    fun isPremiumUser(): Boolean {
        val currentSubscription = _uiState.value.currentSubscription
        return currentSubscription?.isActive == true
    }

    private fun getAvailablePlans(): List<SubscriptionPlan> {
        return listOf(
            SubscriptionPlan(
                id = "devstreak_monthly",
                name = "DevStreak Pro Monthly",
                description = "Unlock all premium features",
                price = "₹129/month",
                priceAmount = 129.0,
                duration = "monthly",
                features = listOf(
                    "Unlimited AI-generated challenges",
                    "Advanced progress analytics",
                    "Priority customer support",
                    "Exclusive challenge packs",
                    "Friend leaderboards",
                    "Custom learning paths"
                )
            ),
            SubscriptionPlan(
                id = "devstreak_lifetime",
                name = "DevStreak Pro Lifetime",
                description = "One-time payment, lifetime access",
                price = "₹999",
                priceAmount = 999.0,
                duration = "lifetime",
                isPopular = true,
                discountPercentage = 84, // Compared to 12 months of monthly
                features = listOf(
                    "Everything in Monthly Pro",
                    "Lifetime updates",
                    "Future premium features",
                    "VIP community access",
                    "Personal AI coach",
                    "Advanced code review"
                )
            )
        )
    }
}

// Interface for platform-specific billing
interface BillingService {
    suspend fun purchasePlan(plan: SubscriptionPlan): Result<String>
    suspend fun restorePurchases(): Result<List<String>>
    suspend fun checkSubscriptionStatus(): Result<Boolean>
}

// Repository for subscription data
interface SubscriptionRepository {
    suspend fun getCurrentSubscription(): Result<UserSubscription?>
    suspend fun verifyPurchase(planId: String, transactionId: String): Result<Boolean>
    suspend fun syncRestoredPurchases(transactionIds: List<String>): Result<Boolean>
}
