package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.database.ChallengePathQueries
import com.dailydevchallenge.database.SelectAllUserProgress
import com.dailydevchallenge.database.UserProfileQueries
import com.dailydevchallenge.database.UserProgress
import com.dailydevchallenge.devstreaks.model.ChallengePathResponse
import com.dailydevchallenge.devstreaks.model.ChallengePath
import com.dailydevchallenge.devstreaks.utils.generateUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import com.dailydevchallenge.devstreaks.database.toModel // for both extensions
import com.dailydevchallenge.devstreaks.features.feed.UserStats
import com.dailydevchallenge.devstreaks.model.BaselineMetrics
import com.dailydevchallenge.devstreaks.model.ChallengeActivity
import com.dailydevchallenge.devstreaks.model.ChallengeCompletionMetrics
import com.dailydevchallenge.devstreaks.model.ChallengePathWithTasks
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.model.CompletedChallenge
import com.dailydevchallenge.devstreaks.model.EngagementRecord
import com.dailydevchallenge.devstreaks.model.TaskReflection
import com.dailydevchallenge.devstreaks.model.User
import com.dailydevchallenge.devstreaks.settings.UserPreferences
import com.dailydevchallenge.devstreaks.sync.FirebaseUserHelper
import com.dailydevchallenge.devstreaks.sync.PlatformSync
import com.dailydevchallenge.devstreaks.utils.getLogger
import com.dailydevchallenge.devstreaks.model.PerformanceMetrics
import com.dailydevchallenge.devstreaks.model.UserPerformanceSummary
import com.dailydevchallenge.devstreaks.model.DifficultyLevel
import com.dailydevchallenge.devstreaks.model.DifficultyAdjustment
import com.dailydevchallenge.devstreaks.model.WeakArea
import com.dailydevchallenge.devstreaks.service.DifficultyCalculator
import kotlin.math.roundToInt


class ChallengeRepository(
    private val pathQueries: ChallengePathQueries,
    private val userProfileQueries: UserProfileQueries,
    private val firebaseUserHelper: FirebaseUserHelper
) {
    val logger  = { getLogger()}
    private val difficultyCalculator = DifficultyCalculator()

    suspend fun savePathToDb(path: ChallengePathResponse): String = withContext(Dispatchers.Default) {
        // Check if similar content already exists before saving
        val existingPath = checkForExistingContent(path.track)
        if (existingPath != null) {
            logger().d("ChallengeRepository", "Reusing existing path: ${existingPath.id}")
            return@withContext existingPath.id
        }
        
        val pathId = generateUUID()
        pathQueries.insertPath(id = pathId, track = path.track)

        path.days.forEach { day ->
            val taskId = generateUUID()
            pathQueries.insertTask(
                id = taskId,
                pathId = pathId,
                day = day.day.toLong(),
                title = day.title,
                type = day.type,
                content = day.content,
                xp = day.xp.toLong(),
                checklist = day.checklist.joinToString("|"),
                whyItMatters = day.whyItMatters,
                bonus = day.bonus,
                tip = day.tip,
                aiBreakdown = day.aiBreakdown,
                videoUrl = day.videoUrl,
                codeExample = day.codeExample
            )

            day.challenges.forEach { activity ->
                pathQueries.insertActivity(
                    id = generateUUID(),
                    taskId = taskId,
                    type = activity.type.name,
                    prompt = activity.prompt,
                    options = activity.options?.joinToString("|"),
                    correctAnswer = activity.correctAnswer,
                    language = activity.language,
                    starterCode = activity.starterCode,
                    explanation = activity.explanation,
                    videoUrl = activity.videoUrl
                )
            }
        }

        return@withContext pathId
    }

    /**
     * Check for existing content before AI generation to enable reuse
     */
    suspend fun checkForExistingContent(track: String): ChallengePath? = withContext(Dispatchers.Default) {
        return@withContext pathQueries.checkExistingChallengePath(track).executeAsOneOrNull()?.let {
            ChallengePath(it.id, it.track)
        }
    }

    /**
     * Find similar tasks for content reuse
     */
    suspend fun findSimilarTasks(type: String, keyword: String): List<ChallengeTask> = withContext(Dispatchers.Default) {
        return@withContext pathQueries.findSimilarTasks(type, keyword, keyword).executeAsList().map { result ->
            // Create ChallengeTask from partial query result
            ChallengeTask(
                id = result.id,
                pathId = "", // Not available in partial result
                day = 0, // Not available in partial result
                title = result.title,
                type = result.type,
                content = result.content,
                xp = 0, // Not available in partial result
                challenges = emptyList() // Will be loaded separately if needed
            )
        }
    }

    /**
     * Check if specific activity type already exists
     */
    suspend fun checkExistingActivity(type: String, prompt: String): List<ChallengeActivity> = withContext(Dispatchers.Default) {
        return@withContext pathQueries.checkExistingActivity(type, prompt).executeAsList().map { result ->
            // Create ChallengeActivity from partial query result
            ChallengeActivity(
                id = result.id,
                type = com.dailydevchallenge.devstreaks.model.ActivityType.valueOf(result.type),
                prompt = result.prompt,
                options = null, // Not available in partial result
                correctAnswer = null, // Not available in partial result
                language = null, // Not available in partial result
                starterCode = null, // Not available in partial result
                explanation = null, // Not available in partial result
                videoUrl = null // Not available in partial result
            )
        }
    }

    /**
     * Get reusable content by skill area
     */
    suspend fun getReusableContentBySkill(skillKeyword: String): List<Pair<ChallengeTask, List<ChallengeActivity>>> = withContext(Dispatchers.Default) {
        val results = pathQueries.getReusableContentBySkill(skillKeyword, skillKeyword).executeAsList()
        val taskMap = mutableMapOf<String, MutableList<ChallengeActivity>>()
        val tasks = mutableMapOf<String, ChallengeTask>()

        results.forEach { row ->
            val taskId = row.id
            if (!tasks.containsKey(taskId)) {
                tasks[taskId] = ChallengeTask(
                    id = row.id,
                    pathId = "", // Will be filled from path lookup if needed
                    day = 1,
                    title = row.title,
                    type = row.type,
                    content = row.content,
                    xp = 50,
                    checklist = emptyList(),
                    whyItMatters = "",
                    tip = "",
                    challenges = emptyList()
                )
                taskMap[taskId] = mutableListOf()
            }

            row.prompt?.let { prompt ->
                row.activityType?.let { activityType ->
                    taskMap[taskId]?.add(ChallengeActivity(
                        id = generateUUID(),
                        type = com.dailydevchallenge.devstreaks.model.ActivityType.valueOf(activityType.uppercase()),
                        prompt = prompt
                    ))
                }
            }
        }

        return@withContext tasks.map { (taskId, task) ->
            task to (taskMap[taskId] ?: emptyList())
        }
    }

    /**
     * Check if content was recently generated to avoid duplication
     */
    suspend fun isRecentContent(track: String): Boolean = withContext(Dispatchers.Default) {
        val count = pathQueries.checkRecentContent(track).executeAsOne()
        return@withContext count > 0
    }

    /**
     * Find content gaps for targeted generation
     */
    suspend fun findContentGaps(minCount: Long = 3): List<Pair<String, Long>> = withContext(Dispatchers.Default) {
        return@withContext pathQueries.findContentGaps(minCount).executeAsList().map { 
            it.type to it.count 
        }
    }

    suspend fun getTasksForPath(pathId: String): List<ChallengeTask> = withContext(Dispatchers.Default) {
        val tasks = pathQueries.selectTasksForPath(pathId).executeAsList()
        return@withContext tasks.map { taskEntity ->
            val challenges = pathQueries
                .selectActivitiesByTask(taskEntity.id)
                .executeAsList()
                .map { it.toModel() } // This maps ChallengeActivityEntity → ChallengeActivity
            taskEntity.toModel(challenges) // This maps ChallengeTaskEntity + activities → ChallengeTask
        }
    }
    suspend fun logEngagementTime(
        taskId: String,
        userId: String,
        startTime: Long,
        endTime: Long
    ) = withContext(Dispatchers.Default) {
        val duration = endTime - startTime
        pathQueries.insertEngagement(
            id = generateUUID(),
            taskId = taskId,
            userId = userId,
            startTime = startTime,
            endTime = endTime,
            durationMillis = duration
        )
        PlatformSync.uploadEngagementData(
            EngagementRecord(taskId, userId, startTime, endTime, duration)
        )
    }




    suspend fun getTaskById(id: String): ChallengeTask? = withContext(Dispatchers.Default) {
        val task = pathQueries.selectTaskById(id).executeAsOneOrNull()
        val challenges = task?.id
            ?.let { pathQueries.selectActivitiesByTask(it).executeAsList().map { it.toModel() } }
            ?: emptyList()
        return@withContext task?.toModel(challenges)
    }

    suspend fun getActivitiesForTask(taskId: String): List<ChallengeActivity> = withContext(Dispatchers.Default) {
        pathQueries.selectActivitiesByTask(taskId).executeAsList().map { it.toModel() }
    }

    suspend fun getAllPaths(): List<ChallengePath> = withContext(Dispatchers.Default) {
        pathQueries.selectAllPaths().executeAsList().map { ChallengePath(it.id, it.track) }
    }

    suspend fun getLatestPathId(): String? = withContext(Dispatchers.Default) {
        pathQueries.selectAllPaths().executeAsList().lastOrNull()?.id
    }

    suspend fun markTaskCompleted(taskId: String, xp: Int, userId: String) = withContext(Dispatchers.Default) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

        userProfileQueries.insertUserProgress(
            id = generateUUID(),
            userId = userId,
            completedTaskId = taskId,
            completedDate = today
        )
        PlatformSync.uploadUserProgress(
            UserProgress(
                id = generateUUID(),
                userId = userId,
                completedTaskId = taskId,
                completedDate = today
            )
        )

        // Get current stats from local DB
        val currentStats = pathQueries.selectUserStats().executeAsOneOrNull()
        val yesterday = LocalDate.parse(today).minus(1, DateTimeUnit.DAY).toString()
        val isStreak = currentStats?.lastCompletedDate == yesterday || currentStats?.lastCompletedDate == today

        val newXp = (currentStats?.xp ?: 0) + xp
        val newStreak = if (isStreak) (currentStats?.streak ?: 0) + 1 else 1

        // Update both Firebase and local DB
        firebaseUserHelper.updateUserProgress(userId, newXp.toLong(), newStreak.toLong())
        pathQueries.insertOrReplaceUserStats(
            id = "user_stats",
            xp = newXp.toLong(),
            streak = newStreak.toLong(),
            lastCompletedDate = today
        )

        logger().d("ChallengeRepository", "Task completed: XP updated from ${currentStats?.xp ?: 
        0} to $newXp, Streak: $newStreak")
    }
    suspend fun getXPAndStreak(): Pair<Int, Int> = withContext(Dispatchers.Default) {
        val stats = pathQueries.selectUserStats().executeAsOneOrNull()
        return@withContext Pair(stats?.xp?.toInt() ?: 0, stats?.streak?.toInt() ?: 0)
    }

    suspend fun isTaskCompleted(taskId: String, userId: String): Boolean = withContext(Dispatchers.Default) {
        userProfileQueries.selectUserProgressByTask(taskId, userId).executeAsList().isNotEmpty()
    }

    suspend fun getUserStats(): UserStats {
        val userId = UserPreferences.getSafeUserId() // Get from your auth/session
        return firebaseUserHelper.fetchUserProgress(userId) ?: UserStats(userId, "" +
                "(DevStreak-User)", 0, 0, 0)
    }


    suspend fun getPathById(pathId: String): ChallengePath? = withContext(Dispatchers.Default) {
        pathQueries.selectAllPaths().executeAsList().find { it.id == pathId }?.let {
            ChallengePath(it.id, it.track)
        }
    }

    suspend fun saveChecklistItem(taskId: String, item: String, isChecked: Boolean) {
        val id = "$taskId|$item"
        pathQueries.insertChecklistProgress(
            id = id,
            taskId = taskId,
            item = item,
            isChecked = if (isChecked) 1 else 0
        )
    }

    suspend fun getChecklistForTask(taskId: String): Map<String, Boolean> {
        return pathQueries.selectChecklistForTask(taskId)
            .executeAsList()
            .associate { it.item to (it.isChecked.toInt() != 0) }
    }

    suspend fun getAllUserProgress(): List<SelectAllUserProgress> {
        return userProfileQueries.selectAllUserProgress().executeAsList()
    }

    suspend fun getAllPathsWithTasks(): List<ChallengePathWithTasks> = withContext(Dispatchers.Default) {
        pathQueries.selectAllPaths().executeAsList().map { pathEntity ->
            val tasks = getTasksForPath(pathEntity.id)
            ChallengePathWithTasks(
                id = pathEntity.id,
                track = pathEntity.track,
                tasks = tasks
            )
        }
    }

    suspend fun getSavedChallengePath(): ChallengePathResponse? = withContext(Dispatchers.Default) {
        val latestPathId = getLatestPathId() ?: return@withContext null
        val tasks = getTasksForPath(latestPathId)
        val track = pathQueries.selectPathById(latestPathId).executeAsOneOrNull()?.track ?: return@withContext null

        return@withContext ChallengePathResponse(
            track = track,
            days = tasks
        )
    }
    suspend fun getAllCompletedTaskIds(): List<String> = withContext(Dispatchers.Default) {
        userProfileQueries.selectAllUserProgress()
            .executeAsList()
            .mapNotNull { it.completedTaskId } // Return only non-null values
    }

    suspend fun saveCompletedChallenge(entry: CompletedChallenge) = withContext(Dispatchers.Default) {
        pathQueries.insertCompletedChallenge(
            pathId = entry.pathId,
            completedDate = entry.completedDate
        )
        PlatformSync.uploadCompletedChallenge(entry)
    }
    suspend fun saveTaskReflection(reflection: TaskReflection) = withContext(Dispatchers.Default) {
        pathQueries.insertTaskReflection(
            id = generateUUID(),
            taskId = reflection.taskId,
            reflection = reflection.reflection,
            timestamp = reflection.timestamp
        )
        PlatformSync.uploadReflection(reflection)
    }

    // NEW: Performance tracking methods for dynamic difficulty adjustment

    /**
     * Record detailed performance metrics for a completed task
     */
    suspend fun recordPerformanceMetrics(metrics: PerformanceMetrics) = withContext(Dispatchers.Default) {
        pathQueries.insertPerformanceMetrics(
            id = metrics.id,
            userId = metrics.userId,
            taskId = metrics.taskId,
            startTime = metrics.startTime,
            endTime = metrics.endTime,
            durationMillis = metrics.durationMillis,
            attempts = metrics.attempts.toLong(),
            hintsUsed = metrics.hintsUsed.toLong(),
            completed = if (metrics.completed) 1 else 0,
            errorCount = metrics.errorCount.toLong(),
            difficultyLevel = metrics.difficultyLevel.value.toLong(),
            timestamp = metrics.timestamp
        )

        logger().d("ChallengeRepository", "Performance metrics recorded: Task=${metrics.taskId}, Duration=${metrics.durationMillis}ms, Attempts=${metrics.attempts}")
    }

    /**
     * Calculate user performance summary for difficulty adjustment
     */
    suspend fun calculateUserPerformanceSummary(userId: String, recentTasksLimit: Int = 10): UserPerformanceSummary = withContext(Dispatchers.Default) {
        val recentMetrics = pathQueries.selectRecentPerformanceMetrics(userId, recentTasksLimit.toLong()).executeAsList()

        if (recentMetrics.isEmpty()) {
            return@withContext UserPerformanceSummary(
                userId = userId,
                averageCompletionTime = 0L,
                successRate = 0.0,
                averageAttempts = 0.0,
                averageHintsUsed = 0.0,
                averageErrorCount = 0.0,
                currentDifficultyLevel = DifficultyLevel.EASY,
                recentTaskCount = 0
            )
        }

        val completedTasks = recentMetrics.filter { it.completed.toInt() == 1 }
        val avgCompletionTime = if (completedTasks.isNotEmpty()) {
            completedTasks.map { it.durationMillis }.average().toLong()
        } else 0L

        val successRate = completedTasks.size.toDouble() / recentMetrics.size.toDouble()
        val avgAttempts = recentMetrics.map { it.attempts.toDouble() }.average()
        val avgHints = recentMetrics.map { it.hintsUsed.toDouble() }.average()
        val avgErrors = recentMetrics.map { it.errorCount.toDouble() }.average()

        // Get current difficulty level from most recent task
        val currentDifficultyLevel = recentMetrics.firstOrNull()?.difficultyLevel?.let {
            DifficultyLevel.fromValue(it.toInt())
        } ?: DifficultyLevel.EASY

        UserPerformanceSummary(
            userId = userId,
            averageCompletionTime = avgCompletionTime,
            successRate = successRate,
            averageAttempts = avgAttempts,
            averageHintsUsed = avgHints,
            averageErrorCount = avgErrors,
            currentDifficultyLevel = currentDifficultyLevel,
            recentTaskCount = recentMetrics.size
        )
    }

    /**
     * Get difficulty adjustment recommendation for user
     */
    suspend fun getDifficultyAdjustment(userId: String): DifficultyAdjustment = withContext(Dispatchers.Default) {
        val performanceSummary = calculateUserPerformanceSummary(userId)
        val baselineMetrics = emptyMap<DifficultyLevel, com.dailydevchallenge.devstreaks.service.BaselineMetrics>() // Using defaults

        return@withContext difficultyCalculator.calculateDifficultyAdjustment(performanceSummary, baselineMetrics)
    }

    /**
     * Adjust challenge XP based on difficulty level
     */
    fun calculateAdjustedXP(baseXP: Int, difficultyLevel: DifficultyLevel): Int {
        return (baseXP * difficultyLevel.multiplier).roundToInt()
    }

    /**
     * Get tasks filtered by difficulty level
     */
    suspend fun getTasksForPathWithDifficulty(pathId: String, difficultyLevel: DifficultyLevel): List<ChallengeTask> = withContext(Dispatchers.Default) {
        val allTasks = getTasksForPath(pathId)

        // Filter or modify tasks based on difficulty level
        return@withContext allTasks.map { task ->
            val adjustedXP = calculateAdjustedXP(task.xp, difficultyLevel)
            val adjustedChallenges = adjustChallengesForDifficulty(task.challenges, difficultyLevel)

            task.copy(
                xp = adjustedXP,
                challenges = adjustedChallenges
            )
        }
    }

    /**
     * Adjust challenge activities based on difficulty level
     */
    private fun adjustChallengesForDifficulty(challenges: List<ChallengeActivity>, difficultyLevel: DifficultyLevel): List<ChallengeActivity> {
        return challenges.map { challenge ->
            when (difficultyLevel) {
                DifficultyLevel.BEGINNER -> challenge.copy(
                    starterCode = enhanceStarterCodeForBeginners(challenge.starterCode),
                    explanation = addDetailedExplanation(challenge.explanation)
                )
                DifficultyLevel.EXPERT -> challenge.copy(
                    starterCode = reduceStarterCodeHints(challenge.starterCode),
                    explanation = reduceExplanationDetail(challenge.explanation)
                )
                else -> challenge
            }
        }
    }

    private fun enhanceStarterCodeForBeginners(starterCode: String?): String? {
        return starterCode?.let { code ->
            // Add more comments and hints for beginners
            "// TODO: Complete the implementation below\n// Hint: Follow the step-by-step approach\n$code"
        }
    }

    private fun addDetailedExplanation(explanation: String?): String? {
        return explanation?.let { exp ->
            "$exp\n\nBeginner tip: Take your time to understand each step before proceeding."
        }
    }

    private fun reduceStarterCodeHints(starterCode: String?): String? {
        return starterCode?.let { code ->
            // Remove obvious hints for expert level
            code.replace(Regex("//\\s*TODO:.*"), "")
                .replace(Regex("//\\s*Hint:.*"), "")
                .trim()
        }
    }

    private fun reduceExplanationDetail(explanation: String?): String? {
        return explanation?.let { exp ->
            // Keep only essential information for experts
            exp.split("\n").take(2).joinToString("\n")
        }
    }

    /**
     * Get user by ID - needed for ProfileEditViewModel
     */
    suspend fun getUserById(userId: String): User? = withContext(Dispatchers.Default) {
        return@withContext try {
            // Try to get user from Firebase first
            firebaseUserHelper.getUserById(userId)
        } catch (e: Exception) {
            logger().e("ChallengeRepository", e,"Error fetching user by ID: ${e.message}")
            null
        }
    }

    /**
     * Update user - needed for ProfileEditViewModel
     */
    suspend fun updateUser(user: User): Boolean = withContext(Dispatchers.Default) {
        return@withContext try {
            firebaseUserHelper.updateUser(user)
            true
        } catch (e: Exception) {
            logger().e("ChallengeRepository", e,"Error updating user: ${e.message}")
            false
        }
    }

    /**
     * Fetch generated course - needed for HomeViewModel
     */
    suspend fun fetchGeneratedCourse(requestId: String): ChallengePathResponse? = withContext(Dispatchers.Default) {
        return@withContext try {
            PlatformSync.fetchGeneratedCourse(requestId)
        } catch (e: Exception) {
            logger().e("ChallengeRepository", e,"Error fetching generated course: ${e.message}")
            null
        }
    }

    /**
     * ADAPTIVE INTELLIGENCE METHODS
     * Supporting dynamic difficulty adjustment, performance tracking, and personalized challenges
     */

    /**
     * Get adaptive challenge based on user's performance and preferences
     */
    suspend fun getAdaptiveChallenge(
        userId: String,
        difficulty: DifficultyLevel,
        skillArea: String?,
        excludeCompleted: Boolean = true
    ): ChallengeTask? = withContext(Dispatchers.Default) {
        try {
            // Get all available tasks
            val allPaths = pathQueries.selectAllPaths().executeAsList()
            val allTasksDb = mutableListOf<com.dailydevchallenge.database.ChallengeTask>()

            allPaths.forEach { path ->
                val tasks = pathQueries.selectTasksForPath(path.id).executeAsList()
                allTasksDb.addAll(tasks) // ✅ Now using correct database type
            }

            // Convert database ChallengeTask to model ChallengeTask
            val allTasks = allTasksDb.map { dbTask ->
                val activities = pathQueries.selectActivitiesByTask(dbTask.id)
                    .executeAsList()
                    .map { it.toModel() }
                dbTask.toModel(activities) // ✅ Extension converts db → model
            }

            // Filter by skill area if specified
            val filteredTasks = if (skillArea != null) {
                allTasks.filter { task ->
                    task.title.contains(skillArea, ignoreCase = true) ||
                            task.type.contains(skillArea, ignoreCase = true) ||
                            task.content.contains(skillArea, ignoreCase = true)
                }
            } else {
                allTasks
            }

            // Filter by difficulty based on XP
            val difficultyFiltered = filteredTasks.filter { task ->
                when (difficulty) {
                    DifficultyLevel.BEGINNER, DifficultyLevel.EASY -> task.xp <= 20
                    DifficultyLevel.MEDIUM -> task.xp in 21..40
                    DifficultyLevel.HARD -> task.xp in 41..60
                    DifficultyLevel.EXPERT -> task.xp > 60
                }
            }

            // Exclude completed tasks if requested
            val completedTaskIds = getCompletedTaskIds(userId)
            val finalTasks = if (excludeCompleted) {
                difficultyFiltered.filter { it.id !in completedTaskIds }
            } else {
                difficultyFiltered
            }

            finalTasks.randomOrNull() // ✅ Return random task or null
        } catch (e: Exception) {
            logger().e("ChallengeRepository", e, "Error getting adaptive challenge: ${e.message}")
            null
        }
    }
    private suspend fun getCompletedTaskIds(userId: String): Set<String> {
        return try {
            userProfileQueries.selectAllUserProgress().executeAsList()
                .filter { it.userId == userId }
                .mapNotNull { it.completedTaskId } // ✅ Get task IDs, not path IDs
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
    private suspend fun updateUserXP(userId: String, xp: Int) {
        try {
            val currentUser = userProfileQueries.getUserById(userId).executeAsOneOrNull()
            if (currentUser != null) {
                val newXP = (currentUser.xp ?: 0L) + xp
                userProfileQueries.updateUserXP(xp.toLong(), userId)
            }
        } catch (e: Exception) {
            // Handle error
        }
    }


    /**
     * Save performance metrics for adaptive intelligence
     */
    suspend fun savePerformanceMetrics(metrics: PerformanceMetrics) = withContext(Dispatchers.Default) {
        pathQueries.insertPerformanceMetrics(
            id = metrics.id,
            userId = metrics.userId,
            taskId = metrics.taskId,
            startTime = metrics.startTime,
            endTime = metrics.endTime,
            durationMillis = metrics.durationMillis,
            attempts = metrics.attempts.toLong(),
            hintsUsed = metrics.hintsUsed.toLong(),
            completed = if (metrics.completed) 1L else 0L,
            errorCount = metrics.errorCount.toLong(),
            difficultyLevel = metrics.difficultyLevel.value.toLong(),
            timestamp = metrics.timestamp

        )

        logger().d("ChallengeRepository", "Performance metrics saved for challenge ${metrics.taskId}")
    }


    /**
     * Get recent challenges for context building
     */
    suspend fun getRecentChallenges(userId: String, limit: Int = 5): List<ChallengeTask> =
        withContext(Dispatchers.Default) {

        // Use existing query to get recent completed tasks
        val recentTaskRows = userProfileQueries.selectRecentCompletedTasks(userId, limit.toLong()).executeAsList()
        val tasks = mutableListOf<ChallengeTask>()

        recentTaskRows.forEach { row ->
            row.completedTaskId?.let { taskId ->
                getTaskById(taskId)?.let { task ->
                    tasks.add(task)
                }
            }
        }

        return@withContext tasks
    }

    /**
     * Get recent performance metrics for real-time analysis
     */
    suspend fun getRecentPerformanceMetrics(userId: String, limit: Int = 10): List<PerformanceMetrics> =
        withContext(Dispatchers.Default) {
            val dbMetrics = pathQueries.selectRecentPerformanceMetrics(userId, limit.toLong()).executeAsList()
            return@withContext dbMetrics.map { dbMetric ->
                PerformanceMetrics(
                    id = dbMetric.id,
                    userId = dbMetric.userId,
                    taskId = dbMetric.taskId,
                    startTime = dbMetric.startTime,
                    endTime = dbMetric.endTime,
                    durationMillis = dbMetric.durationMillis,
                    attempts = dbMetric.attempts.toInt(),
                    hintsUsed = dbMetric.hintsUsed.toInt(),
                    completed = dbMetric.completed.toInt() == 1,
                    errorCount = dbMetric.errorCount.toInt(),
                    difficultyLevel = DifficultyLevel.fromValue(dbMetric.difficultyLevel.toInt()),
                    timestamp = dbMetric.timestamp
                )
            }
        }

    /**
     * Check if user has completed Day 2 and should transition to adaptive challenges
     */
    suspend fun shouldTriggerAdaptiveChallenges(userId: String): Boolean = withContext(Dispatchers.Default) {
        val completedTasks = userProfileQueries.selectAllUserProgress()
            .executeAsList()
            .filter { it.userId == userId }

        // Check if user has completed at least 2 days worth of challenges
        val completedDays = completedTasks.map { progress ->
            pathQueries.selectTaskById(progress.completedTaskId ?: "").executeAsOneOrNull()?.day?.toInt() ?: 0
        }.distinct()

        return@withContext completedDays.contains(2) && completedDays.size >= 2
    }

    /**
     * Trigger automatic adaptive challenge generation after Day 2 completion
     */
//    suspend fun triggerAdaptiveChallengeGeneration(userId: String): Boolean = withContext(Dispatchers.Default) {
//        try {
//            if (!shouldTriggerAdaptiveChallenges(userId)) {
//                return@withContext false
//            }
//
//            // Generate next adaptive challenges based on performance
//            val performanceSummary = calculateUserPerformanceSummary(userId)
//            val weakAreas = pathQueries.selectWeakAreasForUser(userId).executeAsList()
//
//            // Create adaptive challenge generation request
//            val generationRequest = AdaptiveChallengeGenerationRequest(
//                userId = userId,
//                targetDifficulty = performanceSummary.currentDifficultyLevel,
//                weakAreas = weakAreas.map { it.skillArea },
//                preferredTopics = getPreferredTopicsFromHistory(userId),
//                generationCount = 5 // Generate 5 challenges ahead
//            )
//
//            // Queue background generation
//            queueAdaptiveChallengeGeneration(generationRequest)
//
//            logger().d("ChallengeRepository", "Triggered adaptive challenge generation for user $userId")
//            return@withContext true
//        } catch (e: Exception) {
//            logger().e("ChallengeRepository", e, "Error triggering adaptive generation: ${e.message}")
//            return@withContext false
//        }
//    }

    /**
     * Get user performance summary for adaptive learning
     */
    suspend fun getUserPerformanceSummary(userId: String): UserPerformanceSummary = withContext(Dispatchers.Default) {
        val recentMetrics = getRecentPerformanceMetrics(userId, 20)
        val completedTasks = userProfileQueries.selectAllUserProgress()
            .executeAsList()
            .filter { it.userId == userId }

        val successRate = if (recentMetrics.isNotEmpty()) {
            recentMetrics.count { it.completed }.toDouble() / recentMetrics.size.toDouble()
        } else 0.0

        val averageCompletionTime = if (recentMetrics.isNotEmpty()) {
            recentMetrics.filter { it.completed }.map { it.durationMillis }.average().toLong()
        } else 0L

        val averageAttempts = if (recentMetrics.isNotEmpty()) {
            recentMetrics.map { it.attempts.toDouble() }.average()
        } else 0.0

        val averageHintsUsed = if (recentMetrics.isNotEmpty()) {
            recentMetrics.map { it.hintsUsed.toDouble() }.average()
        } else 0.0

        val averageErrorCount = if (recentMetrics.isNotEmpty()) {
            recentMetrics.map { it.errorCount.toDouble() }.average()
        } else 0.0

        val currentDifficultyLevel = if (recentMetrics.isNotEmpty()) {
            recentMetrics.first().difficultyLevel
        } else DifficultyLevel.EASY

        return@withContext UserPerformanceSummary(
            userId = userId,
            averageCompletionTime = averageCompletionTime,
            successRate = successRate,
            averageAttempts = averageAttempts,
            averageHintsUsed = averageHintsUsed,
            averageErrorCount = averageErrorCount,
            currentDifficultyLevel = currentDifficultyLevel,
            recentTaskCount = recentMetrics.size
        )
    }

    suspend fun getUserPerformanceMetrics(userId: String): com.dailydevchallenge.devstreaks.model.UserPerformanceMetrics? = withContext(Dispatchers.Default) {
        try {
            val completedProgress = userProfileQueries.selectAllUserProgress().executeAsList()
                .filter { it.userId == userId && it.completedTaskId != null }
            if (completedProgress.isEmpty()) return@withContext null

            // For each completed progress, get PerformanceMetrics by taskId
            val metricsList = completedProgress.mapNotNull { progress ->
                val perf = pathQueries.selectPerformanceByTask(progress.completedTaskId!!, userId).executeAsOneOrNull()
                perf
            }.filter { it.completed.toInt() == 1 }
            if (metricsList.isEmpty()) return@withContext null

            val totalChallenges = metricsList.size
            val averageScore = metricsList.map { it.attempts.toDouble() }.average() // Replace with actual score if available
            val averageTime = metricsList.map { it.durationMillis.toDouble() }.average()

            com.dailydevchallenge.devstreaks.model.UserPerformanceMetrics(
                totalChallenges = totalChallenges,
                averageScore = averageScore,
                averageTime = averageTime.toLong()
            )
        } catch (e: Exception) {
            logger().d("ChallengeRepository", "Error in getUserPerformanceMetrics: ${e.message}")
            null
        }
    }
}
