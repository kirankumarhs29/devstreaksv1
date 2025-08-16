package com.dailydevchallenge.devstreaks.features.skilltree

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.service.SkillTreeEngine
import com.dailydevchallenge.devstreaks.service.AdaptiveIntelligenceOrchestrator
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing skill tree state and user interactions
 * Integrates with Phase 2 adaptive intelligence features
 */
class SkillTreeViewModel(
    private val skillTreeEngine: SkillTreeEngine,
    private val adaptiveOrchestrator: AdaptiveIntelligenceOrchestrator
) : ViewModel() {

    private val logger = getLogger()
    private val userId = UserPreferences.getSafeUserId()

    // UI State
    private val _uiState = MutableStateFlow(SkillTreeUiState())
    val uiState: StateFlow<SkillTreeUiState> = _uiState.asStateFlow()

    // Current skill tree
    private val _skillTree = MutableStateFlow<SkillTree?>(null)
    val skillTree: StateFlow<SkillTree?> = _skillTree.asStateFlow()

    // User's skill masteries
    private val _skillMasteries = MutableStateFlow<Map<String, SkillMastery>>(emptyMap())
    val skillMasteries: StateFlow<Map<String, SkillMastery>> = _skillMasteries.asStateFlow()

    init {
        loadUserSkillTree()
    }

    /**
     * Load or generate user's skill tree
     */
    fun loadUserSkillTree() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Try to load existing skill tree first
                val existingTree = loadExistingSkillTree()

                if (existingTree != null) {
                    _skillTree.value = existingTree
                    loadSkillMasteries()
                } else {
                    // Generate new skill tree if none exists
                    generateNewSkillTree()
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hasSkillTree = _skillTree.value != null
                )

            } catch (e: Exception) {
                logger.e("Failed to load skill tree", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load skill tree: ${e.message}"
                )
            }
        }
    }

    /**
     * Generate new personalized skill tree based on user performance
     */
    fun generateNewSkillTree(
        learningGoals: List<String> = emptyList(),
        timeAvailablePerDay: Int = 30
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)

            skillTreeEngine.generatePersonalizedSkillTree(
                userId = userId,
                learningGoals = learningGoals,
                timeAvailablePerDay = timeAvailablePerDay
            ).fold(
                onSuccess = { newTree ->
                    _skillTree.value = newTree
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        hasSkillTree = true,
                        successMessage = "New skill tree generated! ${newTree.allNodes.size} skills ready to explore."
                    )
                    loadSkillMasteries()
                },
                onFailure = { error ->
                    logger.e("Failed to generate skill tree", error)
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = "Failed to generate skill tree: ${error.message}"
                    )
                }
            )
        }
    }

    /**
     * Handle skill node selection and start practice
     */
    fun onSkillNodeSelected(node: SkillNode) {
        if (node.isLocked) {
            _uiState.value = _uiState.value.copy(
                error = "This skill is locked. Complete prerequisite skills first."
            )
            return
        }

        viewModelScope.launch {
            try {
                // Get adaptive challenge configuration for this skill
                val adaptiveConfig = adaptiveOrchestrator.initializeAdaptiveChallenge(
                    userId = userId,
                    challengeTask = createSkillBasedChallenge(node),
                    learningProfile = null // Could be enhanced with user's learning profile
                )

                _uiState.value = _uiState.value.copy(
                    selectedNode = node,
                    adaptiveConfig = adaptiveConfig,
                    showChallengeDialog = true
                )

            } catch (e: Exception) {
                logger.e("Failed to prepare skill challenge", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to prepare challenge: ${e.message}"
                )
            }
        }
    }

    /**
     * Update skill mastery after challenge completion
     */
    fun onChallengeCompleted(
        skillNodeId: String,
        completionMetrics: ChallengeCompletionMetrics
    ) {
        viewModelScope.launch {
            try {
                val updatedMastery = skillTreeEngine.updateSkillMastery(
                    userId = userId,
                    skillNodeId = skillNodeId,
                    challengeMetrics = completionMetrics
                )

                // Update local state
                val currentMasteries = _skillMasteries.value.toMutableMap()
                currentMasteries[skillNodeId] = updatedMastery
                _skillMasteries.value = currentMasteries

                // Update skill tree progress
                updateSkillTreeProgress()

                // Check if we should expand the skill tree
                if (updatedMastery.masteryLevel >= MasteryLevel.INTERMEDIATE) {
                    expandSkillTree(skillNodeId)
                }

                _uiState.value = _uiState.value.copy(
                    successMessage = "Skill progress updated! ${updatedMastery.masteryLevel.title} level achieved."
                )

            } catch (e: Exception) {
                logger.e("Failed to update skill mastery", e)
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update progress: ${e.message}"
                )
            }
        }
    }

    /**
     * Expand skill tree with new LLM-generated nodes
     */
    fun expandSkillTree(completedNodeId: String) {
        viewModelScope.launch {
            try {
                val newNodes = skillTreeEngine.expandSkillTree(userId, completedNodeId)

                if (newNodes.isNotEmpty()) {
                    val currentTree = _skillTree.value
                    if (currentTree != null) {
                        val updatedNodes = currentTree.allNodes.toMutableMap()
                        newNodes.forEach { node ->
                            updatedNodes[node.id] = node
                        }

                        _skillTree.value = currentTree.copy(
                            allNodes = updatedNodes,
                            lastUpdated = kotlinx.datetime.Clock.System.now()
                        )

                        _uiState.value = _uiState.value.copy(
                            successMessage = "New skills unlocked! ${newNodes.size} advanced topics available."
                        )
                    }
                }
            } catch (e: Exception) {
                logger.e("Failed to expand skill tree", e)
            }
        }
    }

    /**
     * Clear messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }

    /**
     * Dismiss challenge dialog
     */
    fun dismissChallengeDialog() {
        _uiState.value = _uiState.value.copy(
            showChallengeDialog = false,
            selectedNode = null,
            adaptiveConfig = null
        )
    }

    private suspend fun loadExistingSkillTree(): SkillTree? {
        // Implementation would load from Firebase/local storage
        // For now, return null to trigger generation
        return null
    }

    private suspend fun loadSkillMasteries() {
        val tree = _skillTree.value ?: return
        val masteries = mutableMapOf<String, SkillMastery>()

        // Load mastery data for each node from storage
        tree.allNodes.keys.forEach { nodeId ->
            // Implementation would load from Firebase/local storage
            // For now, create default mastery
            masteries[nodeId] = SkillMastery(
                id = com.dailydevchallenge.devstreaks.utils.generateUUID(),
                userId = userId,
                skillNodeId = nodeId,
                masteryLevel = MasteryLevel.NOVICE,
                progress = 0f,
                xpEarned = 0,
                completedChallenges = 0,
                timeSpentMinutes = 0,
                firstAttemptAt = kotlinx.datetime.Clock.System.now(),
                lastActivityAt = kotlinx.datetime.Clock.System.now()
            )
        }

        _skillMasteries.value = masteries
    }

    private fun createSkillBasedChallenge(node: SkillNode): ChallengeTask {
        // Create a challenge task based on the skill node
        return ChallengeTask(
            id = com.dailydevchallenge.devstreaks.utils.generateUUID(),
            pathId = _skillTree.value?.id ?: "",
            day = 1,
            title = "Practice: ${node.title}",
            type = "SKILL_PRACTICE",
            content = "Apply your knowledge of ${node.title}. ${node.description}",
            skill = node.title,
            xp = node.xpReward,
            checklist = listOf(
                "Complete the skill practice challenge",
                "Apply concepts from ${node.title}",
                "Demonstrate understanding"
            ),
            whyItMatters = "Mastering ${node.title} will help you progress in ${node.category.name.replace("_", " ").lowercase()}",
            challenges = emptyList() // Would be generated by LLM in a real implementation
        )
    }

    private fun updateSkillTreeProgress() {
        val tree = _skillTree.value ?: return
        val masteries = _skillMasteries.value

        val totalProgress = masteries.values.sumOf { it.progress.toDouble() }
        val averageProgress = if (masteries.isNotEmpty()) {
            (totalProgress / masteries.size).toFloat()
        } else 0f

        val totalXp = masteries.values.sumOf { it.xpEarned }

        _skillTree.value = tree.copy(
            overallProgress = averageProgress,
            totalXpEarned = totalXp,
            lastUpdated = kotlinx.datetime.Clock.System.now()
        )
    }
}

/**
 * UI state for skill tree screen
 */
data class SkillTreeUiState(
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val hasSkillTree: Boolean = false,
    val selectedNode: SkillNode? = null,
    val adaptiveConfig: AdaptiveChallengeConfig? = null,
    val showChallengeDialog: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
