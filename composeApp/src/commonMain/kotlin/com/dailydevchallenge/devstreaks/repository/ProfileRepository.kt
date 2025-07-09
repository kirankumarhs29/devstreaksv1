package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.devstreaks.features.onboarding.LearningProfile

interface ProfileRepository {
    suspend fun saveProfile(profile: LearningProfile, userId: String)
    suspend fun getProfile(userId: String): LearningProfile?
    suspend fun clearProfile(userId: String)
}



