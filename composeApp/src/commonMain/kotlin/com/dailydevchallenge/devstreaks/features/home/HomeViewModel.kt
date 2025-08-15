package com.dailydevchallenge.devstreaks.features.home


import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dailydevchallenge.devstreaks.model.UserStats
import com.dailydevchallenge.devstreaks.llm.LLMService
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfilePreferences
import com.dailydevchallenge.devstreaks.model.ChallengePathResponse
import com.dailydevchallenge.devstreaks.repository.LeaderboardRepository
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import kotlinx.datetime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailydevchallenge.devstreaks.model.User
import com.dailydevchallenge.devstreaks.repository.UserProgressRepository


class HomeViewModel(
    private val repository: ChallengeRepository,
    private val profilePreferences: LearningProfilePreferences,
    private val llmService: LLMService,
    private val lRepository: LeaderboardRepository,
    private val userStatsManager: UserStatsManager
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<ChallengeTask>>(emptyList())
    val tasks: StateFlow<List<ChallengeTask>> = _tasks.asStateFlow()

    private val _todayTask = MutableStateFlow<ChallengeTask?>(null)
    val todayTask: StateFlow<ChallengeTask?> = _todayTask.asStateFlow()

    private val _completedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val completedTaskIds: StateFlow<Set<String>> = _completedTaskIds.asStateFlow()

    val userStats: StateFlow<UserStats> = userStatsManager.userStats
    private val _profile = MutableStateFlow<LearningProfile?>(null)
    val profile: StateFlow<LearningProfile?> = _profile.asStateFlow()
    private val _startDate = MutableStateFlow<LocalDate>(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
    private val _currentTrack = MutableStateFlow("Frontend Mastery")
    val currentTrack: StateFlow<String> = _currentTrack.asStateFlow()
    private val _quickPractice = MutableStateFlow<ChallengeTask?>(null)
    val skills = profile.value?.skills ?: listOf("Kotlin", "DSA")
    val quickPractice: StateFlow<ChallengeTask?> = _quickPractice
    private val _past7DaysActivity = MutableStateFlow<List<Int>>(emptyList())
    val past7DaysActivity: StateFlow<List<Int>> = _past7DaysActivity.asStateFlow()

    private val _devCoachInsights = MutableStateFlow("You're improving in quizzes! Keep the momentum.")
    val devCoachInsights: StateFlow<String> = _devCoachInsights.asStateFlow()
        private val _onboardingCompleted = mutableStateOf(false)
    val onboardingCompleted: State<Boolean> get() = _onboardingCompleted
    private val _generatedCourse = MutableStateFlow<ChallengePathResponse?>(null)
    val generatedCourse: StateFlow<ChallengePathResponse?> = _generatedCourse.asStateFlow()

    private val _isCourseLoading = MutableStateFlow(false)
    val isCourseLoading: StateFlow<Boolean> = _isCourseLoading.asStateFlow()
    var xp by mutableStateOf(0)
        private set
    var totalPomodoroSessions by mutableStateOf(0)
        private set
    val earnedBadges = mutableStateListOf<String>()
    fun incrementPomodoroSession() {
        totalPomodoroSessions++
    }
    fun unlockBadge(name: String) {
        if (!earnedBadges.contains(name)) {
            earnedBadges.add(name)
        }
    }
    fun addXP(amount: Int) {
        xp += amount
    }
    fun resetProgress() {
        xp = 0
        totalPomodoroSessions = 0
        earnedBadges.clear()
    }


    fun loadGeneratedCourse(requestId: String) {
        viewModelScope.launch {
            _isCourseLoading.value = true
            try {
                val course = repository.fetchGeneratedCourse(requestId)
                _generatedCourse.value = course
                if (course != null) {
                    repository.savePathToDb(course)
                    UserPreferences.clearPendingRequest()
                }
            } catch (e: Exception) {
                println("Error loading generated course: ${e.message}")
            } finally {
                _isCourseLoading.value = false
            }
        }
    }


    init {
        viewModelScope.launch {
            _onboardingCompleted.value = profilePreferences.isOnboardingCompleted()
        }
    }

    fun setOnboardingCompleted(value: Boolean) {
        _onboardingCompleted.value = value
        profilePreferences.setOnboardingCompleted(value) // save persistently
    }
    val challengeProgress: StateFlow<Pair<Int, Int>> = combine(
        completedTaskIds,
        tasks
    ) { completed, allTasks ->
        Pair(completed.size + 1, allTasks.size)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Pair(1, 0)
    )
    val estimatedEndDate: StateFlow<LocalDate> = challengeProgress.map { (current, total) ->
        val remainingDays = (total - current).coerceAtLeast(0)
        _startDate.value.plus(remainingDays, DateTimeUnit.DAY)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _startDate.value)



    init {
        loadLatestChallenges()
        loadProfile()
        viewModelScope.launch {
            userStatsManager.loadStats()
            loadActivityChart()
        }
    }

    private fun loadLatestChallenges() {
        viewModelScope.launch {
            val pathId = repository.getLatestPathId()
            if (pathId != null) {
                val pathTasks = repository.getTasksForPath(pathId)
                _tasks.value = pathTasks

                val trackName = repository.getPathById(pathId)?.track ?: "General Learning"
                _currentTrack.value = trackName
                val userId = UserPreferences.getSafeUserId()

                val completed = pathTasks.filter { repository.isTaskCompleted(it.id, userId) }
                    .map { it.id }
                    .toSet()
                _completedTaskIds.value = completed

                _todayTask.value = pathTasks.firstOrNull { it.id !in completed }
            }
        }
    }
    private fun loadProfile() {
        _profile.value = profilePreferences.getProfile()
    }
    private suspend fun loadActivityChart() {
        // You can fetch this from repository if available or simulate for now
        _past7DaysActivity.value = listOf(10, 30, 50, 70, 60, 80, 90)
    }

    fun markTaskCompleted(taskId: String, xpEarned: Int) {
        val userId = UserPreferences.getSafeUserId()
        viewModelScope.launch {
            if (repository.isTaskCompleted(taskId, userId)) return@launch

            // Mark task as completed in the repository
            repository.markTaskCompleted(taskId, xpEarned, userId)

            // Update stats through the stats manager
            val task = repository.getTaskById(taskId)
            if (task != null) {
                userStatsManager.onChallengeCompleted(task.pathId, task.day, xpEarned)
            }

            // Refresh the stats and reload challenges
            userStatsManager.refreshAfterTaskCompletion(taskId, xpEarned)
            loadLatestChallenges()
        }
    }

    fun isCompleted(taskId: String): Boolean {
        return completedTaskIds.value.contains(taskId)
    }

    fun getTaskById(id: String, onResult: (ChallengeTask?) -> Unit) {
        viewModelScope.launch {
            val task = repository.getTaskById(id)
            onResult(task)
        }
    }

    fun refreshTasks() {
        viewModelScope.launch {
            val pathId = repository.getLatestPathId() ?: return@launch
            val updatedTasks = repository.getTasksForPath(pathId)
            val userId = UserPreferences.getSafeUserId()
            _tasks.value = updatedTasks
            _todayTask.value = updatedTasks.firstOrNull { !repository.isTaskCompleted(it.id,userId) }
        }
    }

    fun reloadStats() {
        viewModelScope.launch {
            userStatsManager.loadStats()
        }
    }

    override fun onCleared() {
        viewModelScope.cancel()
    }

    fun getTaskByIdSync(taskId: String): ChallengeTask? {
        return tasks.value.find { it.id == taskId }
    }
    fun isTaskCompletedSync(taskId: String): Boolean {
        val userId = UserPreferences.getSafeUserId()
        return runBlocking {
            repository.isTaskCompleted(taskId, userId)
        }
    }
//    fun markOnboardingComplete() {
//        // Save to datastore or preferences
//        profilePreferences.setOnboardingCompleted(true)
//    }





}
