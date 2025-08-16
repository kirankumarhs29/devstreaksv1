package com.dailydevchallenge.devstreaks.features.home

//data class ChecklistItemProgress(
//    val id: String,
//    val taskId: String,
//    val item: String,
//    val isChecked: Boolean
//)

data class DisplayStats(
    val level: Int,
    val xp: Int,
    val streak: Int,
    val currentDay: Int,
    val totalDays: Int,
    val trackName: String,
    val eta: String
)

