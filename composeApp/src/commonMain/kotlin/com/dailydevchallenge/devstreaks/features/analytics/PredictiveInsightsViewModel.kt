package com.dailydevchallenge.devstreaks.features.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.service.PredictiveAnalyticsEngine
import com.dailydevchallenge.devstreaks.service.SkillTreeEngine
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for predictive analytics dashboard with LLM-powered learning insights
 */
class PredictiveInsightsViewModel(
    private val predictiveEngine: PredictiveAnalyticsEngine,
    private val skillTreeEngine: SkillTreeEngine
) : ViewModel() {

    private val logger = getLogger()

    // UI State
    private val _uiState = MutableStateFlow(PredictiveInsightsUiState())
    val uiState: StateFlow<PredictiveInsightsUiState> = _uiState.asStateFlow()

    // Learning Predictions
    private val _learningPredictions = MutableStateFlow<LearningPredictions?>(null)
    val learningPredictions: StateFlow<LearningPredictions?> = _learningPredictions.asStateFlow()

    // Challenge Prediction
    private val _nextChallengePrediction = MutableStateFlow<ChallengePrediction?>(null)
    val nextChallengePrediction: StateFlow<ChallengePrediction?> = _nextChallengePrediction.asStateFlow()

    // Performance Trends
    private val _performanceTrends = MutableStateFlow<PerformanceTrends?>(null)
    val performanceTrends: StateFlow<PerformanceTrends?> = _performanceTrends.asStateFlow()

    // Learning Schedule
    private val _optimalSchedule = MutableStateFlow<LearningSchedule?>(null)
    val optimalSchedule: StateFlow<LearningSchedule?> = _optimalSchedule.asStateFlow()

    // Skill Mastery Predictions
    private val _skillMasteryPredictions = MutableStateFlow<List<SkillMasteryPrediction>>(emptyList())
    val skillMasteryPredictions: StateFlow<List<SkillMasteryPrediction>> = _skillMasteryPredictions.asStateFlow()

    init {
        loadPredictiveInsights()
    }

    /**
     * Load all predictive insights for the current user
     */
    fun loadPredictiveInsights() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val userId = "current_user_id" // TODO: Get from auth service

                // Load comprehensive learning predictions
                generateLearningPredictions(userId)

                // Load performance trends
                generatePerformanceTrends(userId)

                // Load next challenge prediction
                generateNextChallengePrediction(userId)

                // Load optimal schedule
                generateOptimalSchedule(userId)

                // Load skill mastery predictions
                loadSkillMasteryPredictions(userId)

                _uiState.update { it.copy(isLoading = false) }

            } catch (e: Exception) {
                logger.e("Failed to load predictive insights", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load predictive insights: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Generate comprehensive learning predictions using LLM
     */
    fun generateLearningPredictions(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPredictions = true) }

            try {
                val result = predictiveEngine.generateLearningPredictions(userId)

                result.onSuccess { predictions ->
                    _learningPredictions.value = predictions
                    _uiState.update {
                        it.copy(
                            isGeneratingPredictions = false,
                            successMessage = "Learning predictions generated successfully"
                        )
                    }
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isGeneratingPredictions = false,
                            error = "Failed to generate predictions: ${error.message}"
                        )
                    }
                }

            } catch (e: Exception) {
                logger.e("Failed to generate learning predictions", e)
                _uiState.update {
                    it.copy(
                        isGeneratingPredictions = false,
                        error = "Failed to generate predictions: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Generate performance trends analysis
     */
    fun generatePerformanceTrends(userId: String, timeframeDays: Int = 30) {
        viewModelScope.launch {
            try {
                val result = predictiveEngine.predictPerformanceTrends(userId, timeframeDays)

                result.onSuccess { trends ->
                    _performanceTrends.value = trends
                }.onFailure { error ->
                    logger.e("Failed to generate performance trends: ${error.message}")
                }

            } catch (e: Exception) {
                logger.e("Failed to generate performance trends", e)
            }
        }
    }

    /**
     * Predict next optimal challenge
     */
    fun generateNextChallengePrediction(userId: String) {
        viewModelScope.launch {
            try {
                val userMetrics = UserPerformanceSummary(
                    userId = userId,
                    averageCompletionTime = 0L,
                    successRate = 0.0,
                    averageAttempts = 0.0,
                    averageHintsUsed = 0.0,
                    averageErrorCount = 0.0,
                    currentDifficultyLevel = DifficultyLevel.MEDIUM,
                    recentTaskCount = 0
                )

                val result = predictiveEngine.predictLearningOutcomes(userId, userMetrics)

                result.onSuccess { prediction: ChallengePrediction ->
                    _nextChallengePrediction.value = prediction
                }.onFailure { error: Throwable ->
                    logger.e("Failed to predict next challenge: ${error.message}")
                }

            } catch (e: Exception) {
                logger.e("Failed to predict next challenge", e)
            }
        }
    }

    /**
     * Generate optimal learning schedule
     */
    fun generateOptimalSchedule(
        userId: String,
        availableTimePerDay: Int = 60,
        preferredDifficulty: DifficultyLevel? = null
    ) {
        viewModelScope.launch {
            try {
                val result = predictiveEngine.predictOptimalSchedule(
                    userId = userId,
                    availableTimePerDay = availableTimePerDay,
                    preferredDifficulty = preferredDifficulty
                )

                result.onSuccess { schedule ->
                    _optimalSchedule.value = schedule
                }.onFailure { error ->
                    logger.e("Failed to generate optimal schedule: ${error.message}")
                }

            } catch (e: Exception) {
                logger.e("Failed to generate optimal schedule", e)
            }
        }
    }

    /**
     * Load skill mastery predictions for user's active skills
     */
    fun loadSkillMasteryPredictions(userId: String) {
        viewModelScope.launch {
            try {
                val activeTree = skillTreeEngine.getActiveSkillTree(userId)
                val predictions = mutableListOf<SkillMasteryPrediction>()

                activeTree?.allNodes?.values?.take(5)?.forEach { skillNode ->
                    val result = predictiveEngine.predictSkillMastery(userId, skillNode.title)
                    result.onSuccess { prediction ->
                        predictions.add(prediction)
                    }
                }

                _skillMasteryPredictions.value = predictions

            } catch (e: Exception) {
                logger.e("Failed to load skill mastery predictions", e)
            }
        }
    }

    /**
     * Refresh all predictions
     */
    fun refreshPredictions() {
        viewModelScope.launch {
            val userId = "current_user_id" // TODO: Get from auth
            loadPredictiveInsights()
        }
    }

    /**
     * Update schedule preferences
     */
    fun updateSchedulePreferences(
        availableTimePerDay: Int,
        preferredDifficulty: DifficultyLevel?
    ) {
        viewModelScope.launch {
            val userId = "current_user_id" // TODO: Get from auth
            generateOptimalSchedule(userId, availableTimePerDay, preferredDifficulty)
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    /**
     * Get insight summary for dashboard
     */
    fun getInsightSummary(): InsightSummary {
        val predictions = _learningPredictions.value
        val trends = _performanceTrends.value
        val nextChallenge = _nextChallengePrediction.value

        return InsightSummary(
            performanceTrend = trends?.overallTrend ?: TrendDirection.STABLE,
            plateauRisk = predictions?.plateauRisk?.riskLevel ?: RiskLevel.MEDIUM,
            recommendedChallenge = nextChallenge?.challengeType ?: "General",
            optimalDifficulty = nextChallenge?.difficulty ?: DifficultyLevel.MEDIUM,
            confidenceScore = predictions?.confidenceScore ?: 0.5,
            nextMilestone = predictions?.difficultyProgression?.nextMilestone ?: DifficultyLevel.HARD
        )
    }
}

/**
 * UI state for predictive insights screen
 */
data class PredictiveInsightsUiState(
    val isLoading: Boolean = false,
    val isGeneratingPredictions: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

/**
 * Summary of key insights for dashboard display
 */
data class InsightSummary(
    val performanceTrend: TrendDirection,
    val plateauRisk: RiskLevel,
    val recommendedChallenge: String,
    val optimalDifficulty: DifficultyLevel,
    val confidenceScore: Double,
    val nextMilestone: DifficultyLevel
)
