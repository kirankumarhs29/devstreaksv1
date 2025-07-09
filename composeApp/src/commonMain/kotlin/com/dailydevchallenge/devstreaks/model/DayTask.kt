package com.dailydevchallenge.devstreaks.model

data class DayTask(
    val title: String,
    val description: String,
    val estimatedMinutes: Int,
    val points: Int = 10
)
