package com.dailydevchallenge.devstreaks.features.devcoach
// file: src/commonMain/kotlin/com/dailydevchallenge/features/devcoach/MemoryItem.kt

data class MemoryItem(
    val id: String,
    val question: String,
    val userAnswer: String,
    val feedback: String,
    val timestamp: Long
)
