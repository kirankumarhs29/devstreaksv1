package com.dailydevchallenge.devstreaks.features.home


import com.dailydevchallenge.devstreaks.llm.LLMService


import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.model.UserStats
import com.dailydevchallenge.devstreaks.repository.ChallengeRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfilePreferences
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import kotlinx.datetime.*


class HomeViewModel(
    private val repository: ChallengeRepository,
    private val profilePreferences: LearningProfilePreferences,
    private val llmService: LLMService
) {

    private val viewModelScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, throwable ->
            println("Error in ViewModel scope: ${throwable.message}")
        }
    )

    private val statsManager = UserStatsManager(repository)

    private val _tasks = MutableStateFlow<List<ChallengeTask>>(emptyList())
    val tasks: StateFlow<List<ChallengeTask>> = _tasks.asStateFlow()

    private val _todayTask = MutableStateFlow<ChallengeTask?>(null)
    val todayTask: StateFlow<ChallengeTask?> = _todayTask.asStateFlow()

    private val _completedTaskIds = MutableStateFlow<Set<String>>(emptySet())
    val completedTaskIds: StateFlow<Set<String>> = _completedTaskIds.asStateFlow()

    val userStats: StateFlow<UserStats> = statsManager.userStats
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
            statsManager.loadStats()
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
            statsManager.refreshAfterTaskCompletion(taskId, xpEarned)
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
            statsManager.loadStats()
        }
    }

    fun onCleared() {
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
    fun generateQuickPractice() {
        viewModelScope.launch {
            val task = llmService.generateQuickPractice(skills) // inject skills
            _quickPractice.value = task
        }
    }




}
