package com.dailydevchallenge.devstreaks.model

data class Course(
    val id: String,
    val name: String,
    val tasks: List<DayTask>
) {
    val totalDays: Int get() = tasks.size
}
