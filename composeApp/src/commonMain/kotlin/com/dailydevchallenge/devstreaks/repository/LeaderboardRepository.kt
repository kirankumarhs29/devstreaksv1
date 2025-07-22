package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.devstreaks.features.feed.UserStats


interface LeaderboardRepository {
    suspend fun getAllUserStats(): List<UserStats>
}
