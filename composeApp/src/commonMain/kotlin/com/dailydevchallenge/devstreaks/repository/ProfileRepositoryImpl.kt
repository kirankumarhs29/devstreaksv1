package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.database.UserProfileQueries
import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile

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
}
