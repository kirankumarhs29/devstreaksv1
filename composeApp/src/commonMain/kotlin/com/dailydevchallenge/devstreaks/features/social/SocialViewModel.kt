package com.dailydevchallenge.devstreaks.features.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.model.*
import com.dailydevchallenge.devstreaks.service.SocialEngine
import com.dailydevchallenge.devstreaks.service.PredictiveAnalyticsEngine
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.utils.getLogger
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for managing social features, collaborative learning, and peer interactions
 */
class SocialViewModel(
    private val socialEngine: SocialEngine,
    private val predictiveEngine: PredictiveAnalyticsEngine
) : ViewModel() {

    private val logger = getLogger()

    // UI State
    private val _uiState = MutableStateFlow(SocialUiState())
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    // Social Profile
    private val _socialProfile = MutableStateFlow<SocialProfile?>(null)
    val socialProfile: StateFlow<SocialProfile?> = _socialProfile.asStateFlow()

    // Leaderboards
    private val _leaderboards = MutableStateFlow<Map<LeaderboardType, List<LeaderboardEntry>>>(emptyMap())
    val leaderboards: StateFlow<Map<LeaderboardType, List<LeaderboardEntry>>> = _leaderboards.asStateFlow()

    // Code Reviews
    private val _codeReviews = MutableStateFlow<List<CodeReview>>(emptyList())
    val codeReviews: StateFlow<List<CodeReview>> = _codeReviews.asStateFlow()

    // Collaborative Challenges
    private val _collaborativeChallenges = MutableStateFlow<List<CollaborativeChallenge>>(emptyList())
    val collaborativeChallenges: StateFlow<List<CollaborativeChallenge>> = _collaborativeChallenges.asStateFlow()

    // Community Challenge
    private val _currentCommunityChallenge = MutableStateFlow<CommunityChallenge?>(null)
    val currentCommunityChallenge: StateFlow<CommunityChallenge?> = _currentCommunityChallenge.asStateFlow()

    // Study Groups
    private val _userStudyGroups = MutableStateFlow<List<StudyGroup>>(emptyList())
    val userStudyGroups: StateFlow<List<StudyGroup>> = _userStudyGroups.asStateFlow()

    init {
        loadSocialData()
    }

    /**
     * Load all social data for the current user
     */
    fun loadSocialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // TODO: Get current user ID from auth service
                val userId = "current_user_id" // This should come from auth

                // Load social profile
                val profile = socialEngine.getSocialProfile(userId)
                _socialProfile.value = profile

                // Load leaderboards
                loadLeaderboards()

                // Load code reviews
                loadCodeReviews(userId)

                // Load collaborative challenges
                loadCollaborativeChallenges()

                // Load current community challenge
                loadCurrentCommunityChallenge()

                // Load user study groups
                loadUserStudyGroups(userId)

                _uiState.update { it.copy(isLoading = false) }

            } catch (e: Exception) {
                logger.e("Failed to load social data", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load social features: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Generate and submit peer feedback for code
     */
    fun generatePeerFeedback(
        submissionId: String,
        submissionCode: String,
        submissionDescription: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingFeedback = true) }

            try {
                val userId = UserPreferences.getSafeUserId()
                val result: Result<PeerFeedback> = socialEngine.generatePeerFeedback(
                    userId = userId,
                    submissionId = submissionId,
                    submissionCode = submissionCode,
                    submissionDescription = submissionDescription
                )

                result.onSuccess {
                    _uiState.update {
                        it.copy(
                            isGeneratingFeedback = false,
                            successMessage = "Peer feedback generated successfully"
                        )
                    }
                    // Reload code reviews to show the new feedback
                    loadCodeReviews(userId)
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isGeneratingFeedback = false,
                            error = "Failed to generate peer feedback: ${error.message}"
                        )
                    }
                }

            } catch (e: Exception) {
                logger.e("Failed to generate peer feedback", e)
                _uiState.update {
                    it.copy(
                        isGeneratingFeedback = false,
                        error = "Failed to generate peer feedback: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Create a new collaborative challenge
     */
    fun createCollaborativeChallenge(
        theme: String,
        difficulty: DifficultyLevel,
        maxParticipants: Int = 4
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingChallenge = true) }

            try {
                val userId = "current_user_id" // TODO: Get from auth
                val result = socialEngine.createCollaborativeChallenge(
                    creatorId = userId,
                    theme = theme,
                    difficulty = difficulty,
                    maxParticipants = maxParticipants
                )

                result.onSuccess { challenge ->
                    _uiState.update {
                        it.copy(
                            isCreatingChallenge = false,
                            successMessage = "Collaborative challenge created successfully"
                        )
                    }
                    // Reload collaborative challenges
                    loadCollaborativeChallenges()
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCreatingChallenge = false,
                            error = "Failed to create challenge: ${error.message}"
                        )
                    }
                }

            } catch (e: Exception) {
                logger.e("Failed to create collaborative challenge", e)
                _uiState.update {
                    it.copy(
                        isCreatingChallenge = false,
                        error = "Failed to create challenge: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Join a collaborative challenge
     */
    fun joinCollaborativeChallenge(challengeId: String) {
        viewModelScope.launch {
            try {
                val userId = "current_user_id" // TODO: Get from auth
                val result = socialEngine.joinCollaborativeChallenge(userId, challengeId)

                result.onSuccess {
                    _uiState.update {
                        it.copy(successMessage = "Successfully joined collaborative challenge")
                    }
                    loadCollaborativeChallenges()
                }.onFailure { error ->
                    _uiState.update {
                        it.copy(error = "Failed to join challenge: ${error.message}")
                    }
                }

            } catch (e: Exception) {
                logger.e("Failed to join collaborative challenge", e)
                _uiState.update {
                    it.copy(error = "Failed to join challenge: ${e.message}")
                }
            }
        }
    }

    /**
     * Create a new study group
     */
    fun createStudyGroup(
        name: String,
        description: String,
        focusSkills: List<String>,
        targetLevel: DifficultyLevel,
        maxMembers: Int = 10
    ) {
        viewModelScope.launch {
            try {
                val userId = "current_user_id" // TODO: Get from auth
                val studyGroup = socialEngine.createStudyGroup(
                    creatorId = userId,
                    name = name,
                    description = description,
                    focusSkills = focusSkills,
                    targetLevel = targetLevel,
                    maxMembers = maxMembers
                )

                _uiState.update {
                    it.copy(successMessage = "Study group created successfully")
                }
                loadUserStudyGroups(userId)

            } catch (e: Exception) {
                logger.e("Failed to create study group", e)
                _uiState.update {
                    it.copy(error = "Failed to create study group: ${e.message}")
                }
            }
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

    // Private helper methods
    private suspend fun loadLeaderboards() {
        val leaderboardMap = mutableMapOf<LeaderboardType, List<LeaderboardEntry>>()

        LeaderboardType.entries.forEach { category ->
            val entries = socialEngine.getLeaderboard(category, 20)
            leaderboardMap[category] = entries
        }

        _leaderboards.value = leaderboardMap
    }

    private suspend fun loadCodeReviews(userId: String) {
        val reviews = socialEngine.getRecentCodeReviews(userId, 10)
        _codeReviews.value = reviews
    }

    private suspend fun loadCollaborativeChallenges() {
        val challenges = socialEngine.getActiveCollaborativeChallenges()
        _collaborativeChallenges.value = challenges
    }

    private suspend fun loadCurrentCommunityChallenge() {
        val challenge = socialEngine.getCurrentCommunityChallenge()
        _currentCommunityChallenge.value = challenge
    }

    private suspend fun loadUserStudyGroups(userId: String) {
        val studyGroups = socialEngine.getUserStudyGroups(userId)
        _userStudyGroups.value = studyGroups
    }
}

/**
 * UI state for social features screen
 */
data class SocialUiState(
    val isLoading: Boolean = false,
    val isGeneratingFeedback: Boolean = false,
    val isCreatingChallenge: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
