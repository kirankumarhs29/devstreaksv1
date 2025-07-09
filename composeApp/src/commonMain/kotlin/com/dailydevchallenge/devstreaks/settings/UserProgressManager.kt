package com.dailydevchallenge.devstreaks.settings

//import com.dailydevchallenge.app.data.MockChallengeProvider.challenges
import kotlinx.datetime.*


object UserProgressManager {
    var currentDay = 1 // Simulate the current day for challenge unlock
    private val completedChallenges = mutableSetOf<String>()
    private var currentXp = 0
    private var streak = 0
    private var lastCompletedDate: LocalDate? = null

    fun markChallengeCompleted(id: String) {
        if (completedChallenges.contains(id)) return

        completedChallenges.add(id)
        currentXp += 50

        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val yesterday = today.minus(1, DateTimeUnit.DAY)

        if (lastCompletedDate == yesterday) {
            streak++
        } else if (lastCompletedDate != today) {
            streak = 1
        }

        lastCompletedDate = today
    }

    fun isChallengeCompleted(id: String): Boolean = completedChallenges.contains(id)

//    fun getTodayChallenge(): Challenge = challenges[currentDay % challenges.size]

    fun getXp(): Int = currentXp

    fun getStreak(): Int = streak

    fun resetProgress() {
        completedChallenges.clear()
        currentXp = 0
        streak = 0
        currentDay = 1
        lastCompletedDate = null
    }

    fun advanceDay() {
        currentDay += 1
    }
}
