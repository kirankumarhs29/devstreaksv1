package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.database.ChallengePathQueries
import com.dailydevchallenge.database.UserProfileQueries
import com.dailydevchallenge.database.UserProgress
import com.dailydevchallenge.devstreaks.database.*
import com.dailydevchallenge.devstreaks.model.ChallengePathResponse
import com.dailydevchallenge.devstreaks.model.ChallengePath
import com.dailydevchallenge.devstreaks.utils.generateUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.*
import com.dailydevchallenge.devstreaks.database.toModel // for both extensions
import com.dailydevchallenge.devstreaks.model.ChallengeActivity
import com.dailydevchallenge.devstreaks.model.ChallengePathWithTasks
import com.dailydevchallenge.devstreaks.model.ChallengeTask
import com.dailydevchallenge.devstreaks.model.CompletedChallenge
import com.dailydevchallenge.devstreaks.model.TaskReflection

class ChallengeRepository(
    private val pathQueries: ChallengePathQueries,
    private val userProfileQueries: UserProfileQueries
) {

    suspend fun savePathToDb(path: ChallengePathResponse): String = withContext(Dispatchers.Default) {
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

    suspend fun markTaskCompleted(taskId: String, xp: Int ,userId: String) = withContext(Dispatchers.Default) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

        userProfileQueries.insertUserProgress(
            id = generateUUID(),
            userId = userId,
            completedTaskId = taskId,
            completedDate = today
        )

        val currentStats = pathQueries.selectUserStats().executeAsOneOrNull()
        val yesterday = LocalDate.parse(today).minus(1, DateTimeUnit.DAY).toString()
        val isStreak = currentStats?.lastCompletedDate == yesterday

        val newXp = (currentStats?.xp ?: 0) + xp
        val newStreak = if (isStreak) (currentStats?.streak ?: 0) + 1 else 1

        pathQueries.insertOrReplaceUserStats(
            id = "user_stats",
            xp = newXp.toLong(),
            streak = newStreak.toLong(),
            lastCompletedDate = today
        )
    }

    suspend fun isTaskCompleted(taskId: String, userId: String): Boolean = withContext(Dispatchers.Default) {
        userProfileQueries.selectUserProgressByTask(taskId, userId).executeAsOneOrNull() != null
    }

    suspend fun getUserStats(): Triple<Int, Int, String?> = withContext(Dispatchers.Default) {
        pathQueries.selectUserStats().executeAsOneOrNull()?.let {
            Triple(it.xp?.toInt() ?: 0, it.streak?.toInt() ?: 0, it.lastCompletedDate)
        } ?: Triple(0, 0, null)
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

    suspend fun getAllUserProgress(): List<UserProgress> {
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
    }
    suspend fun saveTaskReflection(reflection: TaskReflection) = withContext(Dispatchers.Default) {
        pathQueries.insertTaskReflection(
            id = generateUUID(),
            taskId = reflection.taskId,
            reflection = reflection.reflection,
            timestamp = reflection.timestamp
        )
    }

    private suspend fun getAllCompletedChallenges(): List<CompletedChallenge> = withContext(Dispatchers.Default) {
        pathQueries.selectAllCompletedChallenges().executeAsList().map {
            CompletedChallenge(it.pathId, it.completedDate)
        }
    }

    suspend fun getAllReflections(): List<TaskReflection> = withContext(Dispatchers.Default) {
        pathQueries.selectAllReflections().executeAsList().map {
            TaskReflection(it.id, it.taskId, it.reflection, it.timestamp)
        }
    }
}
