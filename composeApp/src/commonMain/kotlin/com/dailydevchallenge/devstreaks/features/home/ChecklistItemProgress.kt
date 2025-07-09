package com.dailydevchallenge.devstreaks.features.home

data class ChecklistItemProgress(
    val id: String,
    val taskId: String,
    val item: String,
    val isChecked: Boolean
)
