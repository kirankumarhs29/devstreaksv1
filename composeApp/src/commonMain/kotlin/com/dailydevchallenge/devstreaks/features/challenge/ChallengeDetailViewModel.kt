package com.dailydevchallenge.devstreaks.features.challenge

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.utils.getLogger

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
    val day: ChallengeTask,
    val isCompleted: Boolean = false
) : ViewModel() {
    private val logger = getLogger()
    private val _uiState = MutableStateFlow(
        ChallengeDetailUiState(
            started = false,
            showConfetti = false,
            viewedIds = emptyList(),
            allDone = false,
            isCompleted = isCompleted,
            items = buildFullPagerList(day),
            step = ChallengeStep.LEARN
        )
    )
    val uiState: StateFlow<ChallengeDetailUiState> = _uiState.asStateFlow()

    fun startTask() {
        logger.d("Task started for day ${day.day}")
        _uiState.value = _uiState.value.copy(started = true, step = ChallengeStep.DO)
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

    fun onActivityViewed(id: String) {
        val newViewed = _uiState.value.viewedIds + id
        val allDone = newViewed.size >= day.challenges.size
        _uiState.value = _uiState.value.copy(viewedIds = newViewed, allDone = allDone)
    }

    fun onAllCompleted() {
        logger.d("All activities completed for day ${day.day}")
        _uiState.value = _uiState.value.copy(showConfetti = true, isCompleted = true, step = ChallengeStep.COMPLETE)
    }

    fun dismissConfetti() {
        _uiState.value = _uiState.value.copy(showConfetti = false)
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
