package com.dailydevchallenge.devstreaks.data
import com.dailydevchallenge.devstreaks.utils.generateUUID
import kotlinx.datetime.*

data class Journal(
    val id: String = generateUUID(),
    val title: String,
    val content: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val isSynced: Boolean = false // important for offline sync
)