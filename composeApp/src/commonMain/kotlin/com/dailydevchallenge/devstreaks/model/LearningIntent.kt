package com.dailydevchallenge.devstreaks.model

import kotlinx.serialization.Serializable

@Serializable
data class LearningIntent(
    val primaryGoal: String = "",
    val skillFocus: List<String> = emptyList(),
    val careerTrack: String = "",
    val experience: String = "",
    val timePerDay: Int = 30,
    val learningStyle: String = "",
    val fears: String = "",
    val motivations: List<String> = emptyList()
)
