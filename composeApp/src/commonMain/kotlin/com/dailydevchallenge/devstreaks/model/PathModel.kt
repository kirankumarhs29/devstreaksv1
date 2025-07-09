package com.dailydevchallenge.devstreaks.model

import com.dailydevchallenge.devstreaks.model.Challenge

data class PathModel(
    val id: String,
    val title: String,
    val description: String,
    val challenges: List<Challenge>,// <-- must be List<Challenge>
    val totalXP: Int = 0
)
