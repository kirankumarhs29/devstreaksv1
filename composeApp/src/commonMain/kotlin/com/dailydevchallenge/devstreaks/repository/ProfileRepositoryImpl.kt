package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.database.UserProfileQueries
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile
import com.dailydevchallenge.devstreaks.model.ChallengeStatus
import com.dailydevchallenge.devstreaks.model.User
import com.dailydevchallenge.devstreaks.model.UserChallengeProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class ProfileRepositoryImpl(
    private val queries: UserProfileQueries
) : ProfileRepository {

    override suspend fun saveProfile(profile: LearningProfile, userId: String) {
        queries.insertOrReplaceProfile(
            id = "$userId-profile", // unique key per user
            userId = userId,
            goal = profile.goal,
            skills = profile.skills.joinToString(","),
            experience = profile.experience,
            time_per_day = profile.timePerDay,
            days = profile.days,
            style = profile.style,
            fear = profile.fear
        )
    }

    override suspend fun getProfile(userId: String): LearningProfile? {
        return queries.selectProfile(userId).executeAsOneOrNull()?.let {
            LearningProfile(
                goal = it.goal.orEmpty(),
                skills = it.skills?.split(",") ?: emptyList(),
                experience = it.experience.orEmpty(),
                timePerDay = it.time_per_day.orEmpty(),
                days = it.days.orEmpty(),
                style = it.style.orEmpty(),
                fear = it.fear.orEmpty()
            )
        }
    }

    override suspend fun clearProfile(userId: String) {
        queries.deleteProfile(userId)
    }

    suspend fun saveUserChallengeProgress(progress: UserChallengeProgress) = withContext(Dispatchers.Default) {
        queries.insertUserChallengeProgress(
            userId = progress.userId,
            challengeId = progress.challengeId,
            status = progress.status.name,
            startedAt = progress.startedAt.toEpochMilliseconds(),
            completedAt = progress.completedAt?.toEpochMilliseconds(),
            attempts = progress.attempts!!.toLong(),
            score = progress.score?.toLong(),
            feedback = progress.feedback,
            hintUsed = if (progress.hintUsed) 1 else 0,
            xpEarned = progress.xpEarned,
            timeSpentSeconds = progress.timeSpentSeconds
        )
    }
    suspend fun getUserChallengeProgress(userId: String, challengeId: String): UserChallengeProgress? = withContext(Dispatchers.Default) {
        val result = queries.selectChallengeProgress(userId, challengeId).executeAsOneOrNull() ?:
        return@withContext null

        return@withContext UserChallengeProgress(
            userId = result.userId,
            challengeId = result.challengeId,
            status = ChallengeStatus.valueOf(result.status),
            startedAt = Instant.fromEpochMilliseconds(result.startedAt),
            completedAt = result.completedAt?.let { Instant.fromEpochMilliseconds(it) },
            attempts = result.attempts,
            score = result.score?.toInt(),
            feedback = result.feedback,
            hintUsed = result.hintUsed?.toInt() == 1,
            xpEarned = result.xpEarned,
            timeSpentSeconds = result.timeSpentSeconds
        )
    }
    suspend fun getUserById(userId: String): User? = withContext(Dispatchers.Default) {
        queries.getUserById(userId).executeAsOneOrNull()?.let {
            User(
                userId = it.userId,
                email = it.email,
                passwordHash = it.passwordHash,
                username = it.username,
                avatarUrl = it.avatarUrl,
                createdAt = it.createdAt,
                lastLogin = it.lastLogin,
                xp = it.xp?.toInt() ?: 0,
                level = it.level ?: 1,
                dailyStreak = it.dailyStreak ?: 0,
                streakStartDate = it.streakStartDate?.let { s -> Instant.fromEpochMilliseconds(s) },
                preferences = parsePreferences(it.preferences),
                role = it.role.toString(),
                badges = parseBadges(it.badges)
            )
        }
    }

    private fun parsePreferences(prefs: String?): Map<String, String> {
        return prefs?.split(";")?.mapNotNull {
            val parts = it.split("=")
            if (parts.size == 2) parts[0] to parts[1] else null
        }?.toMap() ?: emptyMap()
    }

    private fun parseBadges(badges: String?): List<String> {
        return badges?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }




}
