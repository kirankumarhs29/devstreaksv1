package com.dailydevchallenge.devstreaks.features.feed

data class UserStats(
    val userId: String,
    val name: String,
    val xp: Int,
    val streak: Int,
    val logicScore: Int
)
