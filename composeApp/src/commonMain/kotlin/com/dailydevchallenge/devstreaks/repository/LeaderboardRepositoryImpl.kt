package com.dailydevchallenge.devstreaks.repository


import com.dailydevchallenge.devstreaks.features.feed.UserStats
import com.dailydevchallenge.devstreaks.sync.FirebaseUserHelper

class LeaderboardRepositoryImpl(private val firebaseUserHelper: FirebaseUserHelper) :
    LeaderboardRepository {
    override suspend fun getAllUserStats(): List<UserStats> =
        firebaseUserHelper.getAllUserStats()
}