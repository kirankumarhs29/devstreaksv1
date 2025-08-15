package com.dailydevchallenge.devstreaks.features.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.features.home.UserStatsManager
import com.dailydevchallenge.devstreaks.llm.AIFeedbackService
import com.dailydevchallenge.devstreaks.llm.ChallengeFeedback
import com.dailydevchallenge.devstreaks.model.Challenge
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Add ChallengeStep enum if not already present
enum class ChallengeStep { LEARN, DO, COMPLETE }

// UI state for ChallengeDetailScreen
 data class ChallengeDetailUiState(
    val started: Boolean = false,
    val showConfetti: Boolean = false,
    val viewedIds: List<String> = emptyList(),
    val allDone: Boolean = false,
    val isCompleted: Boolean = false,
    val items: List<ActivityPagerItem> = emptyList(),
    val step: ChallengeStep = ChallengeStep.LEARN
)

class ChallengeDetailViewModel(
    private val challengeRepository: ChallengeRepository,
    private val userStatsManager: UserStatsManager,
    private val aiFeedbackService: AIFeedbackService
) : ViewModel() {

    private val _currentTask = MutableStateFlow<ChallengeTask?>(null)
    val currentTask: StateFlow<ChallengeTask?> = _currentTask.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // AI Feedback state
    private val _aiFeedback = MutableStateFlow<ChallengeFeedback?>(null)
    val aiFeedback: StateFlow<ChallengeFeedback?> = _aiFeedback.asStateFlow()

    private val _isGeneratingFeedback = MutableStateFlow(false)
    val isGeneratingFeedback: StateFlow<Boolean> = _isGeneratingFeedback.asStateFlow()

    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog: StateFlow<Boolean> = _showFeedbackDialog.asStateFlow()

    private val _completedActivities = MutableStateFlow<Set<String>>(emptySet())

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val task = challengeRepository.getTaskById(taskId)
                _currentTask.value = task

                val userId = UserPreferences.getSafeUserId()
                val isTaskCompleted = challengeRepository.isTaskCompleted(taskId, userId)
                _isCompleted.value = isTaskCompleted

                // Synchronize the UI state with the completion status
                if (isTaskCompleted) {
                    _uiState.value = _uiState.value.copy(
                        started = true,
                        isCompleted = true,
                        allDone = true,
                        step = ChallengeStep.COMPLETE
                    )
                } else {
                    // Reset to initial state for uncompleted tasks
                    _uiState.value = _uiState.value.copy(
                        started = false,
                        isCompleted = false,
                        allDone = false,
                        showConfetti = false,
                        viewedIds = emptyList(),
                        step = ChallengeStep.LEARN
                    )
                }

                _error.value = null
            } catch (e: Exception) {
                _error.value = "Failed to load task: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun completeChallenge(challenge: Challenge, userResponse: String, isCorrect: Boolean, completionTimeMs: Long = 0) {
        val task = _currentTask.value ?: return
        val userId = UserPreferences.getSafeUserId()

        viewModelScope.launch {
            try {
                // Mark task as completed in repository
                challengeRepository.markTaskCompleted(task.id, task.xp, userId)

                // Update user stats through the stats manager
                userStatsManager.onChallengeCompleted(task.pathId, task.day, task.xp)

                _isCompleted.value = true

                // Generate AI feedback for the completed challenge
                generateAIFeedback(challenge, userResponse, isCorrect, completionTimeMs)

                _error.value = null

            } catch (e: Exception) {
                _error.value = "Failed to complete challenge: ${e.message}"
            }
        }
    }

    private fun generateAIFeedback(
        challenge: Challenge,
        userResponse: String,
        isCorrect: Boolean,
        completionTimeMs: Long
    ) {
        viewModelScope.launch {
            try {
                _isGeneratingFeedback.value = true
                _showFeedbackDialog.value = true

                val feedbackResult = aiFeedbackService.generateChallengeFeedback(
                    challenge = challenge,
                    userResponse = userResponse,
                    isCorrect = isCorrect,
                    completionTime = completionTimeMs
                )

                feedbackResult.fold(
                    onSuccess = { feedback ->
                        _aiFeedback.value = feedback
                        logger.d("AI feedback generated successfully")
                    },
                    onFailure = { error ->
                        logger.e("Failed to generate AI feedback", error)
                        // Provide fallback feedback
                        _aiFeedback.value = createFallbackFeedback(isCorrect)
                    }
                )
            } finally {
                _isGeneratingFeedback.value = false
            }
        }
    }

    private fun createFallbackFeedback(isCorrect: Boolean): ChallengeFeedback {
        return ChallengeFeedback(
            score = if (isCorrect) 85 else 60,
            strengths = if (isCorrect) listOf("Correct answer!", "Good problem-solving approach")
                       else listOf("You attempted the challenge", "Learning from mistakes is valuable"),
            improvements = if (isCorrect) listOf("Try to optimize your solution", "Consider edge cases")
                          else listOf("Review the concept", "Practice similar problems"),
            personalizedTips = listOf("Keep practicing daily", "Focus on understanding the fundamentals"),
            nextSteps = listOf("Continue with the next challenge", "Review related topics"),
            encouragement = if (isCorrect) "Excellent work! Keep building your streak! 🚀"
                           else "Great effort! Every challenge makes you stronger! 💪"
        )
    }

    fun clearError() {
        _error.value = null
    }

    private val logger = getLogger()
    private val _uiState = MutableStateFlow(
        ChallengeDetailUiState(
            started = false,
            showConfetti = false,
            viewedIds = emptyList(),
            allDone = false,
            isCompleted = false,
            items = emptyList(),
            step = ChallengeStep.LEARN
        )
    )
    val uiState: StateFlow<ChallengeDetailUiState> = _uiState.asStateFlow()

    fun startTask() {
        logger.d("Task started for day ${currentTask.value?.day}")
        val task = _currentTask.value
        if (task != null) {
            val items = buildFullPagerList(task)
            _uiState.value = _uiState.value.copy(
                started = true,
                step = ChallengeStep.DO,
                items = items
            )
        } else {
            _uiState.value = _uiState.value.copy(started = true, step = ChallengeStep.DO)
        }
    }

    fun goToLearn() {
        _uiState.value = _uiState.value.copy(step = ChallengeStep.LEARN)
    }

    fun goToDo() {
        _uiState.value = _uiState.value.copy(step = ChallengeStep.DO)
    }

    fun goToComplete() {
        _uiState.value = _uiState.value.copy(step = ChallengeStep.COMPLETE)
    }

    fun onActivityCompleted(activityId: String, isCorrect: Boolean) {
        if (isCorrect) {
            val newCompleted = _completedActivities.value + activityId
            _completedActivities.value = newCompleted

            val totalActivities = currentTask.value?.challenges?.size ?: 0
            val allCompleted = newCompleted.size >= totalActivities

            _uiState.value = _uiState.value.copy(
                viewedIds = _uiState.value.viewedIds + activityId,
                allDone = allCompleted
            )

            if (allCompleted) {
                onAllCompleted()
            }
        }
    }

    fun onActivityViewed(id: String) {
        val newViewed = _uiState.value.viewedIds + id
        _uiState.value = _uiState.value.copy(viewedIds = newViewed)
    }

    fun onAllCompleted() {
        logger.d("All activities completed for day ${currentTask.value?.day}")
        val task = _currentTask.value ?: return
        val userId = UserPreferences.getSafeUserId()

        viewModelScope.launch {
            try {
                // Single point of completion - handles both repository and stats
                challengeRepository.markTaskCompleted(task.id, task.xp, userId)
                userStatsManager.onChallengeCompleted(task.pathId, task.day, task.xp)

                // Update UI state
                _uiState.value = _uiState.value.copy(
                    showConfetti = true,
                    isCompleted = true,
                    step = ChallengeStep.COMPLETE
                )
                _isCompleted.value = true

            } catch (e: Exception) {
                logger.e("Failed to complete challenge: ${e.message}")
            }
        }
    }

    fun dismissConfetti() {
        _uiState.value = _uiState.value.copy(showConfetti = false)
    }

    fun dismissFeedbackDialog() {
        _showFeedbackDialog.value = false
        _aiFeedback.value = null
    }
}

fun getInjectedInsights(day: ChallengeTask): List<ActivityPagerItem> {
    val insightCards = mutableListOf<ActivityPagerItem>()
    day.whyItMatters?.let { insightCards += ActivityPagerItem("why", "why", it ) }
    day.tip?.let       { insightCards += ActivityPagerItem("tip", "tip", it) }
    day.bonus?.let     { insightCards += ActivityPagerItem("bonus", "bonus", it) }
    day.aiBreakdown?.let { insightCards += ActivityPagerItem("aiBreakdown", "aiBreakdown", it) }
    return insightCards
}

fun buildFullPagerList(day: ChallengeTask): List<ActivityPagerItem> {
    val insights = getInjectedInsights(day)
    val activities = day.challenges.map {
        ActivityPagerItem(
            id = it.id,
            type = it.type.toString(),
            content = it.prompt ,
            challenge = it
        )
    }
    return buildList {
        var i = 0
        if (insights.isNotEmpty()) add(insights[0])
        for (a in activities) {
            add(a)
            i++
            if (i < insights.size) add(insights[i])
        }
    }
}
